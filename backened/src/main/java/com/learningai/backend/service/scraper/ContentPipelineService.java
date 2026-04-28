package com.learningai.backend.service.scraper;

import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ScrapedContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPipelineService {

    private final WebScraperService         scraperService;
    private final GroqUrlSuggestionService  urlSuggestionService;
    private final ScrapedContentRepository  contentRepository;
    private final EmbeddingService          embeddingService;

    // ─── Called when user completes onboarding ────────────────────────────

    @Async
    public void bootstrapTopicContent(String topicGoal) {
        log.info("Bootstrapping content for new topic: {}", topicGoal);

        List<String> urls = urlSuggestionService.suggestUrls(
                "Introduction and overview of " + topicGoal,
                topicGoal,
                topicGoal + " fundamentals");

        List<ScrapedContent> scraped = scraperService.scrapeAll(
                urls, topicGoal, "overview", "ONBOARDING");

        log.info("Bootstrapped {} pages for topic: {}", scraped.size(), topicGoal);
        scraped.forEach(embeddingService::embedContent);
        log.info("Bootstrap + embed complete for topic: {}", topicGoal);
    }

    // ─── Called when RAG finds no results ────────────────────────────────

    @Async
    public void scrapeForQuestion(String userQuestion,
                                   String topicGoal,
                                   String conceptName) {
        log.info("On-demand scrape triggered — topic:{} concept:{} q:{}",
                topicGoal, conceptName, userQuestion);

        List<String> urls = urlSuggestionService.suggestUrls(
                userQuestion, topicGoal, conceptName);

        List<ScrapedContent> scraped = scraperService.scrapeAll(
                urls, topicGoal, conceptName, "ON_DEMAND");

        log.info("On-demand scrape complete for: {}", conceptName);
        scraped.forEach(embeddingService::embedContent);
        log.info("On-demand scrape + embed complete for: {}", conceptName);
    }

    // ─── Cache AI-generated knowledge as searchable content ──────────────

    @Async
    public void storeAiKnowledge(String question,
                                  String answer,
                                  String conceptName,
                                  String conceptTag) {
        try {
            String bodyText = String.format("Question: %s\n\nAnswer: %s", question, answer);

            // FIX: was MD5 — replaced with SHA-256
            String urlHash = sha256(question + conceptTag);

            // Skip if already stored
            if (contentRepository.existsByUrlHash(urlHash)) {
                log.debug("AI knowledge already stored for: {}", question);
                return;
            }

            String syntheticUrl = "groq://knowledge/" + urlHash;

            ScrapedContent content = ScrapedContent.builder()
                    .url(syntheticUrl)
                    .urlHash(urlHash)
                    .title(conceptName + " — " + truncate(question, 80))
                    .bodyText(bodyText)
                    .conceptTag(conceptTag)
                    .conceptName(conceptName)
                    .source("groq_knowledge")
                    .wordCount(countWords(bodyText))
                    .embedded(false)
                    .retrievalCount(0)
                    .scrapeReason("AI_KNOWLEDGE_CACHE")
                    .build();

            content = contentRepository.save(content);
            embeddingService.embedContent(content);

            log.info("AI knowledge cached — concept:{} tag:{} words:{}",
                    conceptName, conceptTag, content.getWordCount());

        } catch (Exception e) {
            log.error("Failed to store AI knowledge: {}", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    public long getContentCount(String topicGoal) {
        return contentRepository.countByConceptTagIgnoreCase(topicGoal);
    }

    public boolean hasEnoughContent(String topicGoal) {
        return getContentCount(topicGoal) >= 3;
    }

    /**
     * FIX: was MD5 — replaced with SHA-256.
     * SHA-256 is collision-resistant; MD5 is not.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Fallback to hashCode if somehow SHA-256 is unavailable (shouldn't happen)
            log.error("SHA-256 not available: {}", e.getMessage());
            return String.valueOf(input.hashCode());
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}