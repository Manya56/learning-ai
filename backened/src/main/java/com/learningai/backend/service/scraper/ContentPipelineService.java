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

    private final WebScraperService         scraperService;
    private final GroqUrlSuggestionService  urlSuggestionService;
    private final ScrapedContentRepository  contentRepository;
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
            topicGoal + " fundamentals"
        );

        List<ScrapedContent> scraped = scraperService.scrapeAll(
            urls, topicGoal, "overview", "ONBOARDING"
        );

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
            userQuestion, topicGoal, conceptName
        );

        List<ScrapedContent> scraped = scraperService.scrapeAll(
            urls, topicGoal, conceptName, "ON_DEMAND"
        );

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
}