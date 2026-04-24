package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.scraper.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.learningai.backend.repository.LearningProfileRepository;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Semantic search over scraped content")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

        private final EmbeddingService embeddingService;
        private final LearningProfileRepository profileRepository;

        @GetMapping("/semantic")
        public ResponseEntity<ApiResponse<List<EmbeddingService.SearchResult>>> search(
                        @AuthenticationPrincipal User user,
                        @RequestParam String query,
                        @RequestParam(required = false) String conceptTag,
                        @RequestParam(defaultValue = "5") int limit) {

                // Priority: explicit tag > user's goal > null (pure global)
                String primaryTag = conceptTag;

                if (primaryTag == null || primaryTag.isBlank()) {
                        primaryTag = profileRepository
                                        .findByUserId(user.getId())
                                        .map(p -> p.getGoal())
                                        .orElse(null);
                }

                List<EmbeddingService.SearchResult> results = embeddingService.search(query, primaryTag, limit);

                return ResponseEntity.ok(ApiResponse.ok(
                                "Search complete — " + results.size() + " results",
                                results));
        }

        @PostMapping("/embed/trigger")
        @Operation(summary = "Manually trigger embedding of all pending content")
        public ResponseEntity<ApiResponse<String>> triggerEmbed() {
                embeddingService.embedAllPending();
                return ResponseEntity.ok(ApiResponse.ok(
                                "Embedding triggered", "Check logs for progress"));
        }
}