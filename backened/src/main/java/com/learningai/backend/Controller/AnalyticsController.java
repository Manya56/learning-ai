package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Learning behavior analytics dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Overview — streak, accuracy, study time, due revisions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getOverview(user.getId())));
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Activity heatmap — last 90 days")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> heatmap(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getHeatmap(user.getId())));
    }

    @GetMapping("/difficulty-history")
    @Operation(summary = "Difficulty level changes over time")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> difficultyHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getDifficultyHistory(user.getId())));
    }

    @GetMapping("/style-evolution")
    @Operation(summary = "Learning style changes over time")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> styleEvolution(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getStyleEvolution(user.getId())));
    }

    @GetMapping("/weak-concepts")
    @Operation(summary = "Weak + strong concepts with scores and status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> weakConcepts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getWeakConceptAnalysis(user.getId())));
    }

    @GetMapping("/weekly")
    @Operation(summary = "Weekly performance — last 4 weeks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> weekly(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getWeeklyPerformance(user.getId())));
    }
}