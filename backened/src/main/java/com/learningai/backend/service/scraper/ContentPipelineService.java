package com.learningai.backend.service.scraper;

import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ScrapedContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPipelineService {

    private final WebScraperService scraperService;
    private final GroqUrlSuggestionService urlSuggestionService;
    private final ScrapedContentRepository contentRepository;
    private final EmbeddingService embeddingService;

    // ─── Called when user completes onboarding ────────────────────────────
    // Runs in background — doesn't block onboarding response

    @Async
    public void bootstrapTopicContent(String topicGoal) {
        log.info("Bootstrapping content for new topic: {}", topicGoal);

        // Ask Groq for the best intro URLs for this topic
        List<String> urls = urlSuggestionService.suggestUrls(
                "Introduction and overview of " + topicGoal,
                topicGoal,
                topicGoal + " fundamentals");

        List<ScrapedContent> scraped = scraperService.scrapeAll(
                urls, topicGoal, "overview", "ONBOARDING");

        log.info("Bootstrapped {} pages for topic: {}",
                scraped.size(), topicGoal);

        scraped.forEach(embeddingService::embedContent);
        log.info("Bootstrap + embed complete for topic: {}", topicGoal);
    }

    // ─── Called when RAG finds no results ────────────────────────────────
    // On-demand scraping triggered by a user question

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

    // ─── Count available content for a topic ─────────────────────────────

    public long getContentCount(String topicGoal) {
        return contentRepository.countByConceptTagIgnoreCase(topicGoal);
    }

    // ─── Check if topic has enough content ───────────────────────────────

    public boolean hasEnoughContent(String topicGoal) {
        return getContentCount(topicGoal) >= 3;
    }

    @Async
    public void storeAiKnowledge(String question,
            String answer,
            String conceptName,
            String conceptTag) {
        try {
            // Build a clean document from Q+A
            String bodyText = String.format(
                    "Question: %s\n\nAnswer: %s", question, answer);

            // Generate a stable hash from question
            String urlHash = md5(question + conceptTag);

            // Skip if already stored
            if (contentRepository.existsByUrlHash(urlHash)) {
                log.debug("AI knowledge already stored for: {}", question);
                return;
            }

            // Create a fake URL for identification
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

            // Embed immediately so it's searchable right away
            embeddingService.embedContent(content);

            log.info("AI knowledge cached — concept:{} tag:{} words:{}",
                    conceptName, conceptTag,
                    content.getWordCount());

        } catch (Exception e) {
            log.error("Failed to store AI knowledge: {}", e.getMessage());
        }
    }

    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() > maxLen
                ? text.substring(0, maxLen) + "..."
                : text;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank())
            return 0;
        return text.trim().split("\\s+").length;
    }
}