package com.learningai.backend.controller;

import com.learningai.backend.dto.request.StartQuizRequest;
import com.learningai.backend.dto.request.SubmitAnswerRequest;
import com.learningai.backend.dto.response.*;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "Quiz session engine — practice loop")
@SecurityRequirement(name = "bearerAuth")
public class QuizController {

        private final QuizService quizService;

        @PostMapping("/start")
        @Operation(summary = "Start a new quiz session for a concept")
        public ResponseEntity<ApiResponse<QuizSessionResponse>> start(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody StartQuizRequest request) {

                QuizSessionResponse session = quizService.startSession(
                                user.getId(), request.getConceptName(), request.getTopicGoal());

                return ResponseEntity.ok(ApiResponse.ok(
                                "Quiz session started", session));
        }

        @PostMapping("/{sessionId}/answer")
        @Operation(summary = "Submit an answer for a question")
        public ResponseEntity<ApiResponse<AnswerFeedbackResponse>> submitAnswer(
                        @AuthenticationPrincipal User user,
                        @PathVariable UUID sessionId,
                        @Valid @RequestBody SubmitAnswerRequest request) {

                AnswerFeedbackResponse feedback = quizService.submitAnswer(
                                user.getId(),
                                sessionId,
                                request.getQuestionIndex(),
                                request.getSelectedAnswerIndex(),
                                request.getTimeTakenMs());

                return ResponseEntity.ok(ApiResponse.ok(
                                "Answer recorded", feedback));
        }

        @GetMapping("/{sessionId}/hint")
        @Operation(summary = "Get progressive hint for a question (1, 2, or 3)")
        public ResponseEntity<ApiResponse<HintResponse>> getHint(
                        @AuthenticationPrincipal User user,
                        @PathVariable UUID sessionId,
                        @RequestParam int questionIndex,
                        @RequestParam(defaultValue = "1") int hintNumber) {

                HintResponse hint = quizService.getHint(
                                user.getId(), sessionId, questionIndex, hintNumber);

                return ResponseEntity.ok(ApiResponse.ok(
                                "Hint generated", hint));
        }

        @PostMapping("/{sessionId}/complete")
        @Operation(summary = "Complete a quiz session — returns full result + DNA update")
        public ResponseEntity<ApiResponse<QuizResultResponse>> complete(
                        @AuthenticationPrincipal User user,
                        @PathVariable UUID sessionId) {

                QuizResultResponse result = quizService.completeSession(
                                user.getId(), sessionId);

                return ResponseEntity.ok(ApiResponse.ok(
                                "Session complete!", result));
        }

        @GetMapping("/history")
        @Operation(summary = "Get last 10 quiz sessions")
        public ResponseEntity<ApiResponse<List<QuizHistoryResponse>>> history(
                        @AuthenticationPrincipal User user) {

                return ResponseEntity.ok(ApiResponse.ok(
                                quizService.getHistory(user.getId())));
        }

        @GetMapping("/{sessionId}/details")
        @Operation(summary = "Get detailed quiz session results including questions and answers")
        public ResponseEntity<ApiResponse<QuizDetailedResponse>> getSessionDetails(
                        @AuthenticationPrincipal User user,
                        @PathVariable UUID sessionId) {

                QuizDetailedResponse details = quizService.getSessionDetails(user.getId(), sessionId);
                return ResponseEntity.ok(ApiResponse.ok("Session details retrieved", details));
        }
}