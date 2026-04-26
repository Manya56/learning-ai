package com.learningai.backend.scheduler;

import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ScrapedContentRepository;
import com.learningai.backend.service.scraper.ContentPipelineService;
import com.learningai.backend.service.scraper.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingScheduler {

    private final ScrapedContentRepository  contentRepository;
    private final ContentPipelineService    pipelineService;
    private final EmbeddingService          embeddingService;

    // ─── Weekly re-scrape of all indexed topics ───────────────────────────

    @Scheduled(cron = "0 0 2 * * SUN") // Every Sunday at 2AM
    public void refreshStaleContent() {
        log.info("ScrapingScheduler: weekly content refresh starting");

        // Find content older than 7 days
        Instant staleThreshold = Instant.now()
                .minus(7, ChronoUnit.DAYS);

        List<ScrapedContent> stale = contentRepository
                .findAll().stream()
                .filter(c -> c.getScrapedAt().isBefore(staleThreshold))
                .toList();

        log.info("Found {} stale content items to refresh", stale.size());

        // Re-embed any content that failed embedding
        List<ScrapedContent> unembedded = contentRepository
                .findByEmbeddedFalseOrderByScrapedAtAsc();

        if (!unembedded.isEmpty()) {
            log.info("Re-embedding {} failed items", unembedded.size());
            unembedded.forEach(embeddingService::embedContent);
        }

        log.info("ScrapingScheduler: weekly refresh complete");
    }

    // ─── Daily embedding check — embed anything missed ────────────────────

    @Scheduled(cron = "0 30 1 * * *") // Daily at 1:30AM
    public void embedPendingContent() {
        List<ScrapedContent> pending = contentRepository
                .findByEmbeddedFalseOrderByScrapedAtAsc();

        if (pending.isEmpty()) {
            log.debug("No pending content to embed");
            return;
        }

        log.info("ScrapingScheduler: embedding {} pending items",
                pending.size());
        embeddingService.embedAllPending();
    }
}