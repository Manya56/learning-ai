package com.learningai.backend.controller;

import com.learningai.backend.dto.request.UpdateProfileRequest;
import com.learningai.backend.dto.response.*;
import com.learningai.backend.entity.ProfileSnapshot;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.LearningProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Learning Profile",
     description = "Learning DNA profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final LearningProfileService profileService;

    @GetMapping
    @Operation(summary = "Get full Learning DNA profile")
    public ResponseEntity<ApiResponse<LearningProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getProfile(user.getId())));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get profile stats and progress")
    public ResponseEntity<ApiResponse<ProfileStatsResponse>> getStats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getStats(user.getId())));
    }

    @PatchMapping
    @Operation(summary = "Update profile preferences")
    public ResponseEntity<ApiResponse<LearningProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Profile updated",
                profileService.updateProfile(user.getId(), request)));
    }

    @GetMapping("/style")
    @Operation(summary = "Infer and update learning style from behavior")
    public ResponseEntity<ApiResponse<LearningStyleResponse>> inferStyle(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.inferAndUpdateStyle(user.getId())));
    }

    @GetMapping("/history")
    @Operation(summary = "Get profile DNA change history")
    public ResponseEntity<ApiResponse<List<ProfileSnapshot>>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getSnapshotHistory(user.getId())));
    }

    @PostMapping("/attempt")
    @Operation(summary = "Record a practice attempt — updates DNA")
    public ResponseEntity<ApiResponse<Void>> recordAttempt(
            @AuthenticationPrincipal User user,
            @RequestBody AttemptRequest request) {
        profileService.recordAttempt(
                user.getId(),
                request.getConcept(),
                request.isCorrect(),
                request.getTimeTakenMs(),
                request.isHintUsed(),
                request.isCodingQuestion()
        );
        return ResponseEntity.ok(
                ApiResponse.ok("Attempt recorded", null));
    }

    @PostMapping("/explanation-read")
    @Operation(summary = "Record that user read an explanation")
    public ResponseEntity<ApiResponse<Void>> recordExplanationRead(
            @AuthenticationPrincipal User user) {
        profileService.recordExplanationRead(user.getId());
        return ResponseEntity.ok(
                ApiResponse.ok("Recorded", null));
    }

    // Inner DTO
    @lombok.Data
    public static class AttemptRequest {
        private String concept;
        private boolean correct;
        private long timeTakenMs;
        private boolean hintUsed;
        private boolean codingQuestion;
    }
}