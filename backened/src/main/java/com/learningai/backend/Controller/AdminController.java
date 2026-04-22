package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ScrapedContentRepository;
import com.learningai.backend.service.scraper.ContentPipelineService;
import com.learningai.backend.service.scraper.GroqUrlSuggestionService;
import com.learningai.backend.service.scraper.WebScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin tools — scraping, content management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final WebScraperService        scraperService;
    private final GroqUrlSuggestionService urlSuggestionService;
    private final ContentPipelineService   pipelineService;
    private final ScrapedContentRepository contentRepository;

    @PostMapping("/scrape")
    @Operation(summary = "Manually scrape a URL and tag it to a topic")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scrape(
            @RequestParam String url,
            @RequestParam String conceptTag,
            @RequestParam(defaultValue = "") String conceptName) {

        Optional<ScrapedContent> result = scraperService.scrape(
                url, conceptTag, conceptName, "MANUAL");

        if (result.isPresent()) {
            ScrapedContent c = result.get();
            return ResponseEntity.ok(ApiResponse.ok(
                "Scraped successfully",
                Map.of(
                    "id",        c.getId(),
                    "title",     c.getTitle(),
                    "wordCount", c.getWordCount(),
                    "source",    c.getSource()
                )
            ));
        }

        return ResponseEntity.ok(
            ApiResponse.error("Scraping failed or duplicate", "SCRAPE_FAILED"));
    }

    @PostMapping("/scrape/topic")
    @Operation(summary = "Ask Groq to suggest URLs and scrape them for a topic")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scrapeTopic(
            @RequestParam String topicGoal,
            @RequestParam String conceptName,
            @RequestParam(defaultValue = "What are the fundamentals?")
                String question) {

        List<String> suggestedUrls = urlSuggestionService.suggestUrls(
                question, topicGoal, conceptName);

        List<ScrapedContent> scraped = scraperService.scrapeAll(
                suggestedUrls, topicGoal, conceptName, "ADMIN_GROQ");

        return ResponseEntity.ok(ApiResponse.ok(
            "Topic scrape complete",
            Map.of(
                "urlsSuggested", suggestedUrls.size(),
                "pagesScraped",  scraped.size(),
                "urls",          suggestedUrls
            )
        ));
    }

    @GetMapping("/content")
    @Operation(summary = "List all scraped content for a topic")
    public ResponseEntity<ApiResponse<List<ScrapedContent>>> listContent(
            @RequestParam String conceptTag) {

        List<ScrapedContent> content =
            contentRepository
                .findByConceptTagIgnoreCaseOrderByRetrievalCountDesc(
                    conceptTag);

        return ResponseEntity.ok(ApiResponse.ok(content));
    }

    @GetMapping("/content/stats")
    @Operation(summary = "Stats on how much content is indexed per topic")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(
            @RequestParam String conceptTag) {

        long count = contentRepository
                .countByConceptTagIgnoreCase(conceptTag);

        long unembedded = contentRepository
                .findByEmbeddedFalseOrderByScrapedAtAsc()
                .stream()
                .filter(c -> c.getConceptTag()
                        .equalsIgnoreCase(conceptTag))
                .count();

        return ResponseEntity.ok(ApiResponse.ok(
            Map.of(
                "totalPages",      count,
                "unembedded",      unembedded,
                "readyForRag",     count - unembedded,
                "hasEnoughContent", pipelineService.hasEnoughContent(conceptTag)
            )
        ));
    }

    @DeleteMapping("/content/{id}")
    @Operation(summary = "Delete a scraped content entry")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable java.util.UUID id) {
        contentRepository.deleteById(id);
        return ResponseEntity.ok(
            ApiResponse.ok("Deleted", null));
    }
}