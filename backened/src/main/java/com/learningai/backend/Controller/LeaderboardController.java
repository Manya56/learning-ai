package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.LeaderboardService;
import com.learningai.backend.service.XpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "XP leaderboard and gamification")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final XpService          xpService;

    @GetMapping("/weekly")
    @Operation(summary = "Top 20 weekly leaderboard + your rank (pinned at bottom)")
    public ResponseEntity<ApiResponse<LeaderboardService.LeaderboardResponse>> weekly(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                leaderboardService.getWeeklyLeaderboard(user.getId())));
    }

    @GetMapping("/all-time")
    @Operation(summary = "Top 20 all-time leaderboard + your rank")
    public ResponseEntity<ApiResponse<LeaderboardService.LeaderboardResponse>> allTime(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                leaderboardService.getAllTimeLeaderboard(user.getId())));
    }

    @GetMapping("/my-xp")
    @Operation(summary = "Your XP summary — weekly, all-time, rank, recent events")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myXp(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "weeklyXp",   xpService.getWeeklyXp(user.getId()),
                "allTimeXp",  xpService.getAllTimeXp(user.getId()),
                "weeklyRank", xpService.getWeeklyRank(user.getId()),
                "currentWeek", xpService.getCurrentWeek()
        )));
    }
}