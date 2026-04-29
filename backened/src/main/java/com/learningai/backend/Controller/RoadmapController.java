package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.RoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@Tag(name = "Roadmap", description = "Learning roadmap — progress tracking and daily plan")
@SecurityRequirement(name = "bearerAuth")
public class RoadmapController {

    private final RoadmapService roadmapService;

    @GetMapping
    @Operation(summary = "Get full roadmap with all topics and overall progress")
    public ResponseEntity<ApiResponse<RoadmapService.RoadmapStateResponse>> getRoadmap(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                roadmapService.getRoadmapState(user.getId())));
    }

    @GetMapping("/current-topic")
    @Operation(summary = "Get current topic with next concept to study and what API to call")
    public ResponseEntity<ApiResponse<RoadmapService.CurrentTopicResponse>> getCurrentTopic(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                roadmapService.getCurrentTopic(user.getId())));
    }

    @GetMapping("/daily-plan")
    @Operation(summary = "Get today's study plan — tasks with exact API calls to make")
    public ResponseEntity<ApiResponse<RoadmapService.DailyPlanResponse>> getDailyPlan(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                roadmapService.getDailyPlan(user.getId())));
    }

    @PostMapping("/complete-concept")
    @Operation(summary = "Mark a concept complete after quiz/practice — auto-unlocks next topic")
    public ResponseEntity<ApiResponse<RoadmapService.ConceptCompleteResponse>> completeConcept(
            @AuthenticationPrincipal User user,
            @RequestBody CompleteConceptRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                roadmapService.markConceptComplete(
                        user.getId(),
                        request.getConceptName(),
                        request.getActivityType(),
                        request.getScore())));
    }

    @PostMapping("/initialize")
    @Operation(summary = "Manually initialize roadmap (called automatically after onboarding)")
    public ResponseEntity<ApiResponse<Void>> initialize(
            @AuthenticationPrincipal User user) {
        roadmapService.initializeRoadmap(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Roadmap initialized", null));
    }

    @Data
    public static class CompleteConceptRequest {
        @NotBlank
        private String conceptName;
        // QUIZ or PRACTICE
        @NotBlank
        private String activityType;
        // score 0.0 to 1.0
        private double score;
    }
}