package com.learningai.backend.service.scraper;

import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.service.AiService;
import com.learningai.backend.service.LanguageService;
import com.learningai.backend.entity.ScrapedContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

        private final EmbeddingService embeddingService;
        private final WebScraperService scraperService;
        private final GroqUrlSuggestionService urlSuggestionService;
        private final ContentPipelineService pipelineService;
        private final LearningProfileRepository profileRepository;
        private final AiService aiService;
        private final LanguageService languageService;

        private static final int MIN_CHUNKS_THRESHOLD = 2;
        private static final int MAX_CONTEXT_CHARS = 3000;
        private static final double MIN_SIMILARITY_THRESHOLD = 0.40;

        // ─── Main RAG entry point ─────────────────────────────────────────────

        public RagResponse answer(UUID userId,
                        String userQuestion,
                        String conceptName) {

                // Load user's Learning DNA
                LearningProfile profile = profileRepository
                                .findByUserId(userId).orElse(null);
                String topicGoal = profile != null ? profile.getGoal() : "General";

                String searchTag = resolveSearchTag(userQuestion, conceptName, topicGoal);

                String learningStyle = profile != null ? profile.getLearningStyle() : "PRACTICE";
                String difficulty = profile != null ? profile.getCurrentDifficulty() : "MEDIUM";

                LanguageService.TranslationContext langCtx = languageService.prepareQuery(userQuestion);

                String englishQuestion = langCtx.englishQuery();

                log.info("RAG query — user:{} topic:{} concept:{} q:{} lang : {}",
                                userId, topicGoal, conceptName, userQuestion, langCtx.languageName());

                if (!langCtx.wasEnglish()) {
                        log.info("Translated query: '{}' → '{}'",
                                        englishQuestion, englishQuestion);
                }

                // ── Layer 1: Vector search ────────────────────────────────────────
                List<EmbeddingService.SearchResult> chunks = embeddingService.search(englishQuestion, null, 5);

                List<EmbeddingService.SearchResult> relevantChunks = chunks.stream()
                                .filter(c -> c.getSimilarity() >= MIN_SIMILARITY_THRESHOLD)
                                .collect(java.util.stream.Collectors.toList());

                log.info("Layer 1 — {} total chunks, {} above threshold ({})",
                                chunks.size(), relevantChunks.size(), MIN_SIMILARITY_THRESHOLD);

                if (relevantChunks.size() >= MIN_CHUNKS_THRESHOLD) {
                        log.info("Layer 1 hit — using {} relevant chunks", relevantChunks.size());

                        String englishAnswer = generateWithContext(
                                        englishQuestion, conceptName, relevantChunks,
                                        profile, learningStyle, difficulty,
                                        "RETRIEVED", searchTag).getAnswer();

                        String finalAnswer = langCtx.wasEnglish() ? englishAnswer
                                        : languageService.translateFromEnglish(
                                                        englishAnswer,
                                                        langCtx.languageCode(),
                                                        langCtx.languageName());
                        return buildResponse(finalAnswer, relevantChunks,
                                        "RETRIEVED", conceptName, topicGoal,langCtx);
                }

                log.info("Layer 1 miss — chunks not relevant enough, trying Groq direct");

                List<EmbeddingService.SearchResult> topicMatches = chunks.stream()
                                .filter(c -> c.getConceptTag() != null &&
                                                c.getConceptTag().equalsIgnoreCase(topicGoal))
                                .collect(java.util.stream.Collectors.toList());

                List<EmbeddingService.SearchResult> relevantTopicChunks = topicMatches.stream()
                                .filter(c -> c.getSimilarity() >= MIN_SIMILARITY_THRESHOLD)
                                .collect(java.util.stream.Collectors.toList());

                if (relevantTopicChunks.size() >= MIN_CHUNKS_THRESHOLD) {
                        log.info("Layer 1 topic-filtered hit — {} chunks for topic:{}",
                                        topicMatches.size(), topicGoal);

                        String englishAnswer = generateWithContext(
                                        englishQuestion, conceptName, relevantTopicChunks,
                                        profile, learningStyle, difficulty,
                                        "RETRIEVED", searchTag).getAnswer();

                        String finalAnswer = langCtx.wasEnglish() ? englishAnswer
                                        : languageService.translateFromEnglish(
                                                        englishAnswer,
                                                        langCtx.languageCode(),
                                                        langCtx.languageName());
                        return buildResponse(finalAnswer, relevantTopicChunks,
                                        "RETRIEVED", conceptName, topicGoal,langCtx);
                }

                // If global search returned something but not topic-matched,
                // still use it if concept name matches
                List<EmbeddingService.SearchResult> conceptMatches = chunks.stream()
                                .filter(c -> c.getConceptName() != null &&
                                                conceptName != null &&
                                                c.getConceptName().toLowerCase()
                                                                .contains(conceptName.toLowerCase()))
                                .collect(java.util.stream.Collectors.toList());

                List<EmbeddingService.SearchResult> relevantConceptChunks = conceptMatches.stream()
                                .filter(c -> c.getSimilarity() >= MIN_SIMILARITY_THRESHOLD)
                                .collect(java.util.stream.Collectors.toList());

                if (!relevantConceptChunks.isEmpty()) {
                        log.info("Layer 1 concept-matched hit — {} chunks",
                                        conceptMatches.size());

                        String englishAnswer = generateWithContext(
                                        englishQuestion, conceptName, relevantConceptChunks,
                                        profile, learningStyle, difficulty,
                                        "RETRIEVED", searchTag).getAnswer();

                        String finalAnswer = langCtx.wasEnglish() ? englishAnswer
                                        : languageService.translateFromEnglish(
                                                        englishAnswer,
                                                        langCtx.languageCode(),
                                                        langCtx.languageName());
                        return buildResponse(finalAnswer, relevantConceptChunks,
                                        "RETRIEVED", conceptName, topicGoal,langCtx);
                }

                // No good match — fall through to Groq direct
                log.info("Layer 1 miss — no topic/concept match in {} global results",
                                chunks.size());

                log.info("Layer 1 miss — trying Groq direct answer");

                // ── Layer 2: Groq direct answer ───────────────────────────────────
                GroqUrlSuggestionService.GroqDirectAnswer direct = urlSuggestionService.tryDirectAnswer(
                                englishQuestion, topicGoal, conceptName);

                if ("HIGH".equals(direct.getConfidence())) {
                        log.info("Layer 2 hit — Groq HIGH confidence direct answer");
                        String finalAnswer = langCtx.wasEnglish()
                                        ? direct.getAnswer()
                                        : languageService.translateFromEnglish(
                                                        direct.getAnswer(),
                                                        langCtx.languageCode(),
                                                        langCtx.languageName());

                        return buildResponse(finalAnswer, List.of(),
                                        "AI_KNOWLEDGE", conceptName, topicGoal,langCtx);
                }

                log.info("Layer 2 confidence: {} — triggering on-demand scrape",
                                direct.getConfidence());

                // ── Layer 3: Groq suggests URLs → scrape → re-search ─────────────
                List<String> suggestedUrls = urlSuggestionService.suggestUrls(
                                englishQuestion, searchTag, conceptName);

                List<ScrapedContent> scraped = scraperService.scrapeAll(
                                suggestedUrls, searchTag, conceptName, "RAG_ON_DEMAND");

                // Embed immediately — synchronous so we can use right away
                scraped.forEach(embeddingService::embedContent);

                // Re-search with fresh content
                chunks = embeddingService.search(englishQuestion, searchTag, 5);

                if (chunks.size() >= 1) {
                        log.info("Layer 3 hit — {} chunks after on-demand scrape",
                                        chunks.size());

                        String englishAnswer = generateWithContext(
                                        englishQuestion, conceptName, chunks,
                                        profile, learningStyle, difficulty,
                                        "SCRAPED_FRESH", searchTag).getAnswer();

                        String finalAnswer = langCtx.wasEnglish() ? englishAnswer
                                        : languageService.translateFromEnglish(
                                                        englishAnswer,
                                                        langCtx.languageCode(),
                                                        langCtx.languageName());

                        return buildResponse(finalAnswer, chunks,
                                        "SCRAPED_FRESH", conceptName, topicGoal,langCtx);
                }

                // Final fallback — use Groq's medium confidence answer
                log.warn("All layers exhausted — using Groq fallback answer");
                String fallback = generateFallbackAnswer(
                                englishQuestion, conceptName, topicGoal,
                                learningStyle, difficulty);

                String finalAnswer = langCtx.wasEnglish() ? fallback
                                : languageService.translateFromEnglish(
                                                fallback,
                                                langCtx.languageCode(),
                                                langCtx.languageName());

                return buildResponse(finalAnswer, List.of(),
                                "AI_FALLBACK", conceptName, topicGoal,langCtx);
        }

        // ─── Generate answer with retrieved context ───────────────────────────

        private RagResponse generateWithContext(
                        String userQuestion,
                        String conceptName,
                        List<EmbeddingService.SearchResult> chunks,
                        LearningProfile profile,
                        String learningStyle,
                        String difficulty,
                        String sourceType,
                        String resolvedTag) {

                // Build context string from top chunks
                String context = buildContext(chunks);

                // Build DNA-aware system prompt
                String systemPrompt = buildSystemPrompt(
                                learningStyle, difficulty, profile);

                // Build user message with context injected
                String userMessage = buildUserMessage(
                                userQuestion, conceptName, context,
                                profile);

                // Call Groq
                String answer = aiService.call(systemPrompt, userMessage);

                // Extract source citations
                List<RagSource> sources = chunks.stream()
                                .map(c -> RagSource.builder()
                                                .title(c.getSourceTitle())
                                                .url(c.getSourceUrl())
                                                .conceptTag(c.getConceptTag())
                                                .build())
                                .distinct()
                                .limit(3)
                                .collect(Collectors.toList());

                return RagResponse.builder()
                                .answer(answer)
                                .sources(sources)
                                .sourceType(sourceType)
                                .conceptName(conceptName)
                                .topicGoal(resolvedTag)
                                .build();
        }

        // ─── Build context string from chunks ────────────────────────────────

        private String buildContext(
                        List<EmbeddingService.SearchResult> chunks) {

                StringBuilder ctx = new StringBuilder();
                int totalChars = 0;

                for (int i = 0; i < chunks.size(); i++) {
                        EmbeddingService.SearchResult chunk = chunks.get(i);
                        String entry = String.format(
                                        "[Source %d: %s]\n%s\n\n",
                                        i + 1,
                                        chunk.getSourceTitle() != null
                                                        ? chunk.getSourceTitle()
                                                        : "Web Source",
                                        chunk.getChunkText());

                        if (totalChars + entry.length() > MAX_CONTEXT_CHARS)
                                break;

                        ctx.append(entry);
                        totalChars += entry.length();
                }

                return ctx.toString();
        }

        // ─── DNA-aware system prompt ──────────────────────────────────────────

        private String buildSystemPrompt(String learningStyle,
                        String difficulty,
                        LearningProfile profile) {

                String styleGuide = switch (learningStyle) {
                        case "PRACTICE" -> """
                                        - Be concise and direct
                                        - Include code examples whenever relevant
                                        - Focus on HOW to apply, not just theory
                                        - Use bullet points for steps
                                        """;
                        case "READING" -> """
                                        - Give detailed explanations with context
                                        - Explain the WHY behind concepts
                                        - Use analogies to connect with known ideas
                                        - Include background theory
                                        """;
                        case "VISUAL" -> """
                                        - Use ASCII diagrams where helpful
                                        - Break down into clear visual steps
                                        - Use tables for comparisons
                                        - Structure with clear sections
                                        """;
                        default -> "- Be clear and balanced between theory and practice.";
                };

                String difficultyGuide = switch (difficulty) {
                        case "EASY" -> "Use simple language. Avoid jargon. Build from basics.";
                        case "HARD" -> "Use technical depth. Include edge cases. Assume strong foundation.";
                        default -> "Balance simplicity with technical accuracy.";
                };

                String weakConceptsHint = "";
                if (profile != null && !profile.getWeakConcepts().isEmpty()) {
                        String weakList = String.join(", ",
                                        profile.getWeakConcepts().keySet()
                                                        .stream().limit(3)
                                                        .collect(Collectors.toList()));
                        weakConceptsHint = "\nUser struggles with: " + weakList +
                                        ". Be extra clear on these if they come up.";
                }

                return String.format("""
                                You are Aria, an expert AI learning mentor.
                                You help students learn any topic clearly and effectively.

                                STUDENT PROFILE:
                                - Learning style: %s
                                - Difficulty level: %s
                                %s

                                STYLE GUIDELINES:
                                %s

                                DIFFICULTY GUIDELINES:
                                %s

                                IMPORTANT RULES:
                                - Always use the provided context to answer
                                - Cite sources naturally: "According to [Source 1]..."
                                - If context doesn't fully answer, supplement with your knowledge
                                - Never make up facts — say "I'm not certain" if unsure
                                - End with ONE follow-up question to check understanding
                                "- Detect the language of the user's question and respond in the same language\n"
                                """,
                                learningStyle, difficulty, weakConceptsHint,
                                styleGuide, difficultyGuide);
        }

        // ─── User message with context injected ──────────────────────────────

        private String buildUserMessage(String userQuestion,
                        String conceptName,
                        String context,
                        LearningProfile profile) {
                String languageHint = detectNonEnglish(userQuestion)
                                ? "\n\nIMPORTANT: The user asked in a non-English language. Respond in the same language they used."
                                : "";

                return String.format("""
                                CONTEXT FROM TRUSTED SOURCES:
                                %s

                                CONCEPT: %s
                                QUESTION: %s
                                %s

                                Please answer based on the context above.
                                """,
                                context, conceptName, userQuestion, languageHint);
        }

        // ─── Fallback when all layers fail ────────────────────────────────────

        private String generateFallbackAnswer(String question,
                        String concept,
                        String topic,
                        String style,
                        String difficulty) {
                String systemPrompt = buildSystemPrompt(style, difficulty, null);
                String userMessage = String.format(
                                "Topic: %s\nConcept: %s\nQuestion: %s",
                                topic, concept, question);
                return aiService.call(systemPrompt, userMessage);
        }

        private boolean detectNonEnglish(String text) {
                if (text == null)
                        return false;
                // Count non-ASCII characters
                long nonAscii = text.chars()
                                .filter(c -> c > 127)
                                .count();
                // If more than 20% non-ASCII → non-English
                return nonAscii > (text.length() * 0.2);
        }

        private String resolveSearchTag(String question,
                        String conceptName,
                        String topicGoal) {
                try {
                        String systemPrompt = """
                                        You are a topic classifier for a learning platform.
                                        Your job is to identify the best topic tag for a question.

                                        Rules:
                                        - Return ONLY the topic tag — 1 to 4 words maximum
                                        - No explanation, no punctuation, no extra text
                                        - If the question fits the user's current goal, return that goal exactly
                                        - If the question is about a different subject, return the most accurate topic name
                                        - Examples of good tags: "DSA", "Machine Learning", "Stock Market",
                                          "Indian Entertainment", "Physics", "Web Development", "History of India",
                                          "Cooking", "Yoga", "Personal Finance"
                                        """;

                        String userMessage = String.format(
                                        """
                                                        User's current learning goal: %s
                                                        Concept asked about: %s
                                                        Question: %s

                                                        What is the best topic tag for this question?
                                                        If it matches the user's goal return "%s" exactly.
                                                        Otherwise return the correct topic in 1-4 words.
                                                        """,
                                        topicGoal, conceptName, question, topicGoal);

                        String tag = aiService.call(systemPrompt, userMessage)
                                        .trim()
                                        .replaceAll("\"", "") // remove any quotes Groq adds
                                        .replaceAll("\\.+$", "") // remove trailing dots
                                        .trim();

                        // Safety — if Groq returns something too long it hallucinated
                        if (tag.split("\\s+").length > 5) {
                                log.warn("Groq returned suspicious tag '{}' — using concept name",
                                                tag);
                                return conceptName;
                        }

                        log.info("Tag resolved — goal:'{}' concept:'{}' → tag:'{}'",
                                        topicGoal, conceptName, tag);

                        return tag;

                } catch (Exception e) {
                        log.warn("Tag resolution failed: {} — using concept name",
                                        e.getMessage());
                        return conceptName;
                }
        }

        private RagResponse buildResponse(String answer,
                        List<EmbeddingService.SearchResult> chunks,
                        String sourceType,
                        String conceptName,
                        String topicGoal,
                LanguageService.TranslationContext langCtx) {
                List<RagSource> sources = chunks.stream()
                                .map(c -> RagSource.builder()
                                                .title(c.getSourceTitle())
                                                .url(c.getSourceUrl())
                                                .conceptTag(c.getConceptTag())
                                                .build())
                                .distinct()
                                .limit(3)
                                .collect(java.util.stream.Collectors.toList());

                return RagResponse.builder()
                                .answer(answer)
                                .sources(sources)
                                .sourceType(sourceType)
                                .conceptName(conceptName)
                                .topicGoal(topicGoal)
                                .detectedLanguage(langCtx.languageName())
                                .languageCode(langCtx.languageCode())
                                .wasTranslated(!langCtx.wasEnglish())
                                .build();
        }

        // ─── Inner DTOs ───────────────────────────────────────────────────────

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class RagResponse {
                private String answer;
                private List<RagSource> sources;
                private String sourceType; // RETRIEVED / AI_KNOWLEDGE / SCRAPED_FRESH / AI_FALLBACK
                private String conceptName;
                private String topicGoal;
                // Add to RagResponse inner class:
                private String detectedLanguage;
                private String languageCode;
                private boolean wasTranslated;
        }

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class RagSource {
                private String title;
                private String url;
                private String conceptTag;
        }
}