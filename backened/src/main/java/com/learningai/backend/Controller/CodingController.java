package com.learningai.backend.controller;

import com.learningai.backend.dto.request.EvaluateSubmissionRequest;
import com.learningai.backend.dto.request.GenerateProblemRequest;
import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.dto.response.CodingAttemptResponse;
import com.learningai.backend.dto.response.EvaluationResult;
import com.learningai.backend.dto.response.ProblemResponse;
import com.learningai.backend.entity.CodingAttempt;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.CodingService;
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
@RequestMapping("/api/practice")
@RequiredArgsConstructor
@Tag(name = "Practice", description = "Problem generation + AI evaluation — any topic")
@SecurityRequirement(name = "bearerAuth")
public class CodingController {

        private final CodingService codingService;

        @PostMapping("/generate")
        @Operation(summary = "Generate a practice problem — coding, math, or written")
        public ResponseEntity<ApiResponse<ProblemResponse>> generate(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody GenerateProblemRequest request) {

                ProblemResponse problem = codingService.generateProblem(user.getId(), request);

                return ResponseEntity.ok(ApiResponse.ok(
                                "Problem generated", problem));
        }

        @PostMapping("/evaluate")
        @Operation(summary = "Submit and evaluate an answer — updates Learning DNA")
        public ResponseEntity<ApiResponse<EvaluationResult>> evaluate(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody EvaluateSubmissionRequest request) {

                EvaluationResult result = codingService.evaluateSubmission(user.getId(), request);

                return ResponseEntity.ok(ApiResponse.ok(
                                "Submission evaluated", result));
        }

        @GetMapping("/history")
        @Operation(summary = "Get last 10 practice attempts")
        public ResponseEntity<ApiResponse<List<CodingAttemptResponse>>> history(
                        @AuthenticationPrincipal User user) {

                return ResponseEntity.ok(ApiResponse.ok(
                                codingService.getHistory(user.getId())));
        }

        @GetMapping("/history/{conceptName}")
        @Operation(summary = "Get attempts for a specific concept")
        public ResponseEntity<ApiResponse<List<CodingAttemptResponse>>> conceptHistory(
                        @AuthenticationPrincipal User user,
                        @PathVariable String conceptName) {

                return ResponseEntity.ok(ApiResponse.ok(
                                codingService.getConceptHistory(
                                                user.getId(), conceptName)));
        }
}