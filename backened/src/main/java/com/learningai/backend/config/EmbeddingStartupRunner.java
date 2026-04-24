package com.learningai.backend.config;

import com.learningai.backend.service.scraper.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2) // Run after DataSeeder (Order 1)
@RequiredArgsConstructor
public class EmbeddingStartupRunner implements CommandLineRunner {

    private final EmbeddingService embeddingService;

    @Override
    public void run(String... args) {
        log.info("Checking for unembedded content...");
        // Run async so it doesn't block startup
        embedAsync();
    }

    @Async
    public void embedAsync() {
        try {
            embeddingService.embedAllPending();
        } catch (Exception e) {
            log.error("Startup embedding failed: {}", e.getMessage());
        }
    }
}