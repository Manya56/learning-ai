package com.learningai.backend.scheduler;

import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ScrapedContentRepository;
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

    private final ScrapedContentRepository contentRepository;
    private final EmbeddingService         embeddingService;

    // ─── Weekly re-embed of stale content ────────────────────────────────
    // Every Sunday at 2AM

    @Scheduled(cron = "0 0 2 * * SUN")
    public void refreshStaleContent() {
        log.info("ScrapingScheduler: weekly content refresh starting");

        // FIX: DB-level filter — no longer loads entire table into memory
        Instant staleThreshold = Instant.now().minus(7, ChronoUnit.DAYS);
        List<ScrapedContent> stale = contentRepository
                .findByScrapedAtBefore(staleThreshold);

        log.info("Found {} stale content items (older than 7 days)", stale.size());

        // Re-mark as unembedded so they get re-processed
        for (ScrapedContent content : stale) {
            try {
                content.setEmbedded(false);
                contentRepository.save(content);
            } catch (Exception e) {
                log.warn("Failed to mark content {} as unembedded: {}",
                        content.getId(), e.getMessage());
            }
        }

        // Embed anything that failed before
        List<ScrapedContent> unembedded = contentRepository
                .findByEmbeddedFalseOrderByScrapedAtAsc();

        if (!unembedded.isEmpty()) {
            log.info("Re-embedding {} items", unembedded.size());
            // Process in batches to avoid overwhelming memory
            int batchSize = 20;
            for (int i = 0; i < unembedded.size(); i += batchSize) {
                int end = Math.min(i + batchSize, unembedded.size());
                List<ScrapedContent> batch = unembedded.subList(i, end);
                batch.forEach(c -> {
                    try {
                        embeddingService.embedContent(c);
                    } catch (Exception e) {
                        log.warn("Failed to embed content {}: {}", c.getId(), e.getMessage());
                    }
                });
            }
        }

        log.info("ScrapingScheduler: weekly refresh complete");
    }

    // ─── Daily embedding check ────────────────────────────────────────────
    // Daily at 1:30AM — embed anything missed during the day

    @Scheduled(cron = "0 30 1 * * *")
    public void embedPendingContent() {
        List<ScrapedContent> pending = contentRepository
                .findByEmbeddedFalseOrderByScrapedAtAsc();

        if (pending.isEmpty()) {
            log.debug("No pending content to embed");
            return;
        }

        log.info("ScrapingScheduler: embedding {} pending items", pending.size());
        embeddingService.embedAllPending();
    }
}