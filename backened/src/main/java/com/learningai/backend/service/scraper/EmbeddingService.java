package com.learningai.backend.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.entity.ContentEmbedding;
import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ContentEmbeddingProjection;
import com.learningai.backend.repository.ContentEmbeddingRepository;
import com.learningai.backend.repository.ScrapedContentRepository;
import ai.djl.huggingface.tokenizers.Encoding;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper;
    private final ContentEmbeddingRepository embeddingRepository;
    private final ScrapedContentRepository contentRepository;

    // Groq supports nomic-embed-text-v1.5 — 768 dimensions
    private static final String MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2";
    private static final int CHUNK_SIZE = 400; // words per chunk
    private static final int CHUNK_OVERLAP = 50; // word overlap
    private static final int VECTOR_DIM = 384;

    // Model loaded once at startup — reused for all embeddings
    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    @PostConstruct
    public void loadModel() {
        try {
            log.info("Loading HuggingFace embedding model: {}", MODEL_NAME);

            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/"
                            + "sentence-transformers/all-MiniLM-L6-v2")
                    .optTranslator(new SentenceTranslator(VECTOR_DIM))
                    .optProgress(new ProgressBar())
                    .build();

            model = criteria.loadModel();
            predictor = model.newPredictor();
            log.info("Warming up embedding model...");
            float[] testVector = predictor.predict("test");
            log.info("Warmup complete — vector dim: {}", testVector.length);

            log.info("Embedding model loaded successfully — {} dims", VECTOR_DIM);

        } catch (Exception e) {
            log.error("Failed to load embedding model: {}", e.getMessage());
            log.warn("Falling back to random embeddings for dev mode");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (predictor != null)
            predictor.close();
        if (model != null)
            model.close();
        log.info("Embedding model closed");
    }

    // ─── Embed a single text string → float[] ────────────────────────────

    public float[] embed(String text) {
        if (predictor == null) {
            log.warn("Model not loaded — returning zero vector");
            return new float[VECTOR_DIM];
        }

        try {
            return predictor.predict(text);
        } catch (Exception e) {
            log.error("Embedding failed for text: {}", e.getMessage());
            throw new RuntimeException("Embedding failed: " + e.getMessage());
        }
    }

    private static class SentenceTranslator
            implements Translator<String, float[]> {

        private final int vectorDim;
        private HuggingFaceTokenizer tokenizer;

        SentenceTranslator(int vectorDim) {
            this.vectorDim = vectorDim;
        }

        @Override
        public void prepare(TranslatorContext ctx) throws Exception {
            tokenizer = HuggingFaceTokenizer.newInstance(
                    java.nio.file.Paths.get("models/all-MiniLM-L6-v2"),
                    Map.of(
                            "maxLength", "512",
                            "truncation", "true",
                            "padding", "true"));
        }

        @Override
        public NDList processInput(TranslatorContext ctx, String text) {
            Encoding encoding = tokenizer.encode(text);
            NDManager manager = ctx.getNDManager();
            NDArray inputIds = manager.create(encoding.getIds());
            NDArray attentionMask = manager.create(encoding.getAttentionMask());
            return new NDList(inputIds, attentionMask);
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            NDArray tokenEmbeddings = list.get(0);
            NDArray squeezed = tokenEmbeddings.squeeze(0);
            NDArray meanPooled = squeezed.mean(new int[] { 0 });
            NDArray norm = meanPooled.norm();
            NDArray normalized = meanPooled.div(
                    norm.maximum(meanPooled.getManager()
                            .create(1e-9f)));

            float[] result = normalized.toFloatArray();

            if (result.length != vectorDim) {
                return java.util.Arrays.copyOf(result, vectorDim);
            }
            return result;
        }
    }

    // ─── Embed and store a ScrapedContent entry ───────────────────────────

    public int embedContent(ScrapedContent content) {
        try {
            // Split into chunks
            List<String> chunks = chunkText(
                    content.getBodyText(), CHUNK_SIZE, CHUNK_OVERLAP);

            int stored = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                // Skip if already embedded
                if (embeddingRepository.existsByContentIdAndChunkIndex(
                        content.getId(), i)) {
                    continue;
                }

                // Get embedding from Groq
                float[] vector = embed(chunk);

                // Save embedding
                ContentEmbedding embedding = ContentEmbedding.builder()
                        .contentId(content.getId())
                        .conceptTag(content.getConceptTag())
                        .conceptName(content.getConceptName())
                        .chunkText(chunk)
                        .chunkIndex(i)
                        .embedding(vector)
                        .model(MODEL_NAME)
                        .sourceUrl(content.getUrl())
                        .sourceTitle(content.getTitle())
                        .build();

                embeddingRepository.save(embedding);
                stored++;

                // Rate limit — Groq embedding has limits
                Thread.sleep(200);
            }

            // Mark content as embedded
            content.setEmbedded(true);
            content.setEmbeddingModel(MODEL_NAME);
            content.setEmbeddedAt(java.time.Instant.now());
            contentRepository.save(content);

            log.info("Embedded content: {} ({} chunks) — {}",
                    content.getTitle(), stored, content.getUrl());

            return stored;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (Exception e) {
            log.error("Failed to embed content {}: {}",
                    content.getId(), e.getMessage());
            return 0;
        }
    }

    // ─── Batch embed all unembedded content ───────────────────────────────

    public void embedAllPending() {
        List<ScrapedContent> pending = contentRepository.findByEmbeddedFalseOrderByScrapedAtAsc();

        if (pending.isEmpty()) {
            log.info("No pending content to embed");
            return;
        }

        log.info("Batch embedding {} content items...", pending.size());
        int totalChunks = 0;

        for (ScrapedContent content : pending) {
            totalChunks += embedContent(content);
        }

        log.info("Batch embedding complete — {} total chunks stored",
                totalChunks);
    }

    // ─── Semantic search ──────────────────────────────────────────────────

    public List<SearchResult> search(String query,
            String primaryTag,
            int limit) {
        float[] queryVector = embed(query);
        String vectorString = toPostgresVector(queryVector);

        // Level 1 — search within primary tag first
        List<ContentEmbeddingProjection> results = new ArrayList<>();

        if (primaryTag != null && !primaryTag.isBlank()) {
            results = embeddingRepository.findSimilar(
                    vectorString, primaryTag, limit);
            log.info("Level 1 search (tag:{}) → {} results",
                    primaryTag, results.size());
        }

        // Level 2 — not enough results, search globally
        if (results.size() < 2) {
            log.info("Level 1 insufficient — falling back to global search");
            results = embeddingRepository.findSimilarGlobal(
                    vectorString, limit);
            log.info("Level 2 global search → {} results", results.size());
        }

        return results.stream()
                .map(ce -> SearchResult.builder()
                        .chunkText(ce.getChunkText())
                        .sourceUrl(ce.getSourceUrl())
                        .sourceTitle(ce.getSourceTitle())
                        .conceptTag(ce.getConceptTag())
                        .conceptName(ce.getConceptName())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── Convert float[] to pgvector string format ────────────────────────
    // pgvector expects: [0.1,0.2,0.3,...]

    public String toPostgresVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ─── Text chunking ────────────────────────────────────────────────────

    private List<String> chunkText(String text,
            int chunkSizeWords,
            int overlapWords) {
        if (text == null || text.isBlank())
            return List.of();

        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + chunkSizeWords, words.length);
            String chunk = String.join(" ",
                    Arrays.copyOfRange(words, start, end));
            chunks.add(chunk);

            // Move forward by chunkSize minus overlap
            start += (chunkSizeWords - overlapWords);

            // Safety — avoid infinite loop
            if (chunkSizeWords <= overlapWords)
                break;
        }

        return chunks;
    }

    // ─── Inner DTO ────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchResult {
        private String chunkText;
        private String sourceUrl;
        private String sourceTitle;
        private String conceptTag;
        private String conceptName;
    }
}