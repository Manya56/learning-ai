package com.learningai.backend.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightScraperClient {

    private final ObjectMapper objectMapper;

    @Value("${scraper.service.url:http://localhost:8001}")
    private String scraperServiceUrl;

    // ─── Scrape a single JS-rendered URL ─────────────────────────────────

    public Optional<PlaywrightResult> scrape(String url,
                                              String conceptTag,
                                              String conceptName) {
        try {
            WebClient client = WebClient.create(scraperServiceUrl);

            Map<String, Object> body = Map.of(
                "url",          url,
                "concept_tag",  conceptTag,
                "concept_name", conceptName != null ? conceptName : ""
            );

            JsonNode response = client
                    .post()
                    .uri("/scrape")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null ||
                !response.path("success").asBoolean()) {
                log.warn("Playwright scrape failed for: {} — {}",
                        url, response != null
                                ? response.path("error").asText()
                                : "null response");
                return Optional.empty();
            }

            return Optional.of(PlaywrightResult.builder()
                    .url(url)
                    .title(response.path("title").asText())
                    .bodyText(response.path("body_text").asText())
                    .wordCount(response.path("word_count").asInt())
                    .urlHash(response.path("url_hash").asText())
                    .build());

        } catch (Exception e) {
            log.error("Playwright client error for {}: {}",
                    url, e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Batch scrape ─────────────────────────────────────────────────────

    public List<PlaywrightResult> scrapeAll(List<String> urls,
                                             String conceptTag,
                                             String conceptName) {
        try {
            WebClient client = WebClient.create(scraperServiceUrl);

            Map<String, Object> body = Map.of(
                "urls",         urls,
                "concept_tag",  conceptTag,
                "concept_name", conceptName != null ? conceptName : ""
            );

            JsonNode response = client
                    .post()
                    .uri("/scrape/batch")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return List.of();

            List<PlaywrightResult> results = new java.util.ArrayList<>();
            for (JsonNode r : response.path("results")) {
                if (r.path("success").asBoolean()) {
                    results.add(PlaywrightResult.builder()
                            .url(r.path("url").asText())
                            .title(r.path("title").asText())
                            .bodyText(r.path("body_text").asText())
                            .wordCount(r.path("word_count").asInt())
                            .urlHash(r.path("url_hash").asText())
                            .build());
                }
            }

            log.info("Playwright batch: {}/{} succeeded",
                    results.size(), urls.size());
            return results;

        } catch (Exception e) {
            log.error("Playwright batch error: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Check if URL needs Playwright ───────────────────────────────────

    public boolean needsPlaywright(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("neetcode.io") ||
               lower.contains("visualgo.net") ||
               lower.contains("leetcode.com") ||
               lower.contains("roadmap.sh") ||
               lower.contains("refactoring.guru") ||
               lower.contains("realpython.com");
    }

    // ─── Inner DTO ────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PlaywrightResult {
        private String url;
        private String title;
        private String bodyText;
        private Integer wordCount;
        private String urlHash;
    }
}