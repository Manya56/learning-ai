package com.learningai.backend.controller;

import com.learningai.backend.dto.request.KnowledgeQuizAnswerRequest;
import com.learningai.backend.dto.request.OnboardingRequest;
import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.dto.response.LearningProfileResponse;
import com.learningai.backend.dto.response.OnboardingResponse;
import com.learningai.backend.dto.response.QuizQuestionResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "User onboarding and Learning DNA setup")
@SecurityRequirement(name = "bearerAuth")
public class OnboardingController {

    private final OnboardingService onboardingService;

    // Step 1 — Get knowledge assessment quiz
    @PostMapping("/quiz")
    @Operation(summary = "Generate knowledge assessment quiz based on goal")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> getKnowledgeQuiz(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OnboardingRequest request) {

        List<QuizQuestionResponse> questions =
                onboardingService.getKnowledgeQuiz(user.getId(), request);

        return ResponseEntity.ok(ApiResponse.ok(
                "Knowledge quiz generated", questions));
    }

    // Step 2 — Submit answers and complete onboarding
    @PostMapping("/complete")
    @Operation(summary = "Submit quiz answers and complete onboarding")
    public ResponseEntity<ApiResponse<OnboardingResponse>> completeOnboarding(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OnboardingCompleteRequest request) {

        OnboardingResponse response = onboardingService.completeOnboarding(
                user.getId(),
                request.getOnboarding(),
                request.getQuizAnswers()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Onboarding complete!", response));
    }

    // Get current learning profile
    @GetMapping("/profile")
    @Operation(summary = "Get current user's learning profile")
    public ResponseEntity<ApiResponse<LearningProfileResponse>> getProfile(
            @AuthenticationPrincipal User user) {

        LearningProfileResponse profile =
                onboardingService.getProfile(user.getId());

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // Inner DTO for complete request
    @lombok.Data
    public static class OnboardingCompleteRequest {
        @Valid
        private OnboardingRequest onboarding;
        @Valid
        private KnowledgeQuizAnswerRequest quizAnswers;
    }
}