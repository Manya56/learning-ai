package com.learningai.backend.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.entity.ContentEmbedding;
import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ContentEmbeddingProjection;
import com.learningai.backend.repository.ContentEmbeddingRepository;
import com.learningai.backend.repository.ScrapedContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EmbeddingService — all DJL / HuggingFace model code removed.
 *
 * Now calls the Python embedding service via HTTP.
 * The Python service runs sentence-transformers/all-MiniLM-L6-v2
 * and returns 384-dim float vectors — identical to what DJL produced.
 *
 * This means Spring Boot no longer loads any ML model — saves ~300MB RAM.
 * The embedding service runs as a separate free Render service.
 */
@Slf4j
@Service
public class EmbeddingService {

    private final ObjectMapper                 objectMapper;
    private final ContentEmbeddingRepository   embeddingRepository;
    private final ScrapedContentRepository     contentRepository;
    private final WebClient                    embeddingClient;

    private static final String MODEL_NAME   = "sentence-transformers/all-MiniLM-L6-v2";
    private static final int    CHUNK_SIZE   = 400;
    private static final int    CHUNK_OVERLAP = 50;
    private static final int    VECTOR_DIM   = 384;
    private static final int    TIMEOUT_SECS = 30;

    public EmbeddingService(
            ObjectMapper objectMapper,
            ContentEmbeddingRepository embeddingRepository,
            ScrapedContentRepository contentRepository,
            @Value("${embedding.service.url:http://localhost:8002}") String embeddingServiceUrl) {

        this.objectMapper        = objectMapper;
        this.embeddingRepository = embeddingRepository;
        this.contentRepository   = contentRepository;

        // Build a dedicated WebClient for the embedding service
        this.embeddingClient = WebClient.builder()
                .baseUrl(embeddingServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        log.info("EmbeddingService initialized — calling: {}", embeddingServiceUrl);
    }

    // ─── Embed a single text ──────────────────────────────────────────────

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[VECTOR_DIM];
        }

        try {
            JsonNode response = embeddingClient.post()
                    .uri("/embed")
                    .bodyValue(Map.of("text", text))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECS))
                    .block();

            return parseVector(response, "embedding");

        } catch (Exception e) {
            log.error("Embedding HTTP call failed: {}", e.getMessage());
            // Return zero vector — content will not be searchable
            // but system keeps running
            return new float[VECTOR_DIM];
        }
    }

    // ─── Embed and store a ScrapedContent entry ───────────────────────────

    public int embedContent(ScrapedContent content) {
        try {
            List<String> chunks = chunkText(
                    content.getBodyText(), CHUNK_SIZE, CHUNK_OVERLAP);

            int stored = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                if (embeddingRepository.existsByContentIdAndChunkIndex(
                        content.getId(), i)) {
                    continue;
                }

                float[] vector = embed(chunk);

                ContentEmbedding embedding = ContentEmbedding.builder()
                        .contentId(content.getId())
                        .conceptTag(content.getConceptTag().replace("+", " "))
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

                // Small delay between chunks to avoid overwhelming embedding service
                Thread.sleep(100);
            }

            content.setEmbedded(true);
            content.setEmbeddingModel(MODEL_NAME);
            content.setEmbeddedAt(java.time.Instant.now());
            contentRepository.save(content);

            log.info("Embedded: {} ({} chunks)", content.getTitle(), stored);
            return stored;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (Exception e) {
            log.error("Failed to embed content {}: {}", content.getId(), e.getMessage());
            return 0;
        }
    }

    // ─── Batch embed all pending ──────────────────────────────────────────

    public void embedAllPending() {
        List<ScrapedContent> pending = contentRepository
                .findByEmbeddedFalseOrderByScrapedAtAsc();

        if (pending.isEmpty()) {
            log.info("No pending content to embed");
            return;
        }

        log.info("Batch embedding {} items...", pending.size());
        int totalChunks = 0;

        for (ScrapedContent content : pending) {
            totalChunks += embedContent(content);
        }

        log.info("Batch embedding complete — {} total chunks", totalChunks);
    }

    // ─── Semantic search ──────────────────────────────────────────────────

    public List<SearchResult> search(String query, String primaryTag, int limit) {
        float[] queryVector  = embed(query);
        String vectorString  = toPostgresVector(queryVector);

        List<ContentEmbeddingProjection> results = new ArrayList<>();

        if (primaryTag != null && !primaryTag.isBlank()) {
            results = embeddingRepository.findSimilar(vectorString, primaryTag, limit);
            log.info("Search (tag:{}) → {} results", primaryTag, results.size());
        }

        return results.stream()
                .map(ce -> SearchResult.builder()
                        .chunkText(ce.getChunkText())
                        .sourceUrl(ce.getSourceUrl())
                        .sourceTitle(ce.getSourceTitle())
                        .conceptTag(ce.getConceptTag())
                        .conceptName(ce.getConceptName())
                        .similarity(ce.getSimilarity() != null ? ce.getSimilarity() : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    public String toPostgresVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] parseVector(JsonNode response, String field) {
        if (response == null || !response.has(field)) {
            log.warn("Embedding response missing '{}' field", field);
            return new float[VECTOR_DIM];
        }
        JsonNode arr = response.get(field);
        float[] vector = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vector[i] = arr.get(i).floatValue();
        }
        return vector;
    }

    private List<String> chunkText(String text, int chunkSizeWords, int overlapWords) {
        if (text == null || text.isBlank()) return List.of();
        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + chunkSizeWords, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
            start += (chunkSizeWords - overlapWords);
            if (chunkSizeWords <= overlapWords) break;
        }
        return chunks;
    }

    // ─── DTO ──────────────────────────────────────────────────────────────

    @lombok.Data @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class SearchResult {
        private String chunkText;
        private String sourceUrl;
        private String sourceTitle;
        private String conceptTag;
        private String conceptName;
        private double similarity;
    }
}