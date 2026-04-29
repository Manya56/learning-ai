package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.dto.response.ConceptResponse;
import com.learningai.backend.dto.response.TopicResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * ContentController
 *
 * FIX Issue 1: GET /api/content/topics now calls getTopicsForUser(userId)
 * which reads the user's goal from their LearningProfile and returns
 * only topics for that goal — not all DSA topics for everyone.
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Tag(name = "Content", description = "Topics and concepts content API")
@SecurityRequirement(name = "bearerAuth")
public class ContentController {

    private final ContentService contentService;

    /**
     * FIX: was calling getAllTopics() — now calls getTopicsForUser()
     * so Finance user sees Finance topics, DSA user sees DSA topics.
     */
    @GetMapping("/topics")
    @Operation(summary = "Get topics for the current user's learning goal")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getAllTopics(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.ok(contentService.getTopicsForUser(user.getId())));
    }

    @GetMapping("/topics/category/{category}")
    @Operation(summary = "Get topics by explicit category e.g. DSA, Finance")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(
                ApiResponse.ok(contentService.getTopicsByCategory(category)));
    }

    @GetMapping("/topics/{topicId}")
    @Operation(summary = "Get topic with all its concepts")
    public ResponseEntity<ApiResponse<TopicResponse>> getTopic(
            @PathVariable UUID topicId) {
        return ResponseEntity.ok(
                ApiResponse.ok(contentService.getTopicWithConcepts(topicId)));
    }

    @GetMapping("/topics/{topicId}/concepts")
    @Operation(summary = "Get concepts filtered by difficulty")
    public ResponseEntity<ApiResponse<List<ConceptResponse>>> getConcepts(
            @PathVariable UUID topicId,
            @RequestParam(defaultValue = "EASY") String difficulty) {
        return ResponseEntity.ok(
                ApiResponse.ok(contentService.getConceptsByDifficulty(topicId, difficulty)));
    }

    @GetMapping("/concepts/{conceptId}")
    @Operation(summary = "Get single concept by ID")
    public ResponseEntity<ApiResponse<ConceptResponse>> getConcept(
            @PathVariable UUID conceptId) {
        return ResponseEntity.ok(
                ApiResponse.ok(contentService.getConcept(conceptId)));
    }

    @GetMapping("/concepts/search")
    @Operation(summary = "Search concepts by name")
    public ResponseEntity<ApiResponse<List<ConceptResponse>>> search(
            @RequestParam String query) {
        return ResponseEntity.ok(
                ApiResponse.ok(contentService.searchConcepts(query)));
    }
}