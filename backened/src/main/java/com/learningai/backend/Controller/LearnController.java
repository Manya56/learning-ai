package com.learningai.backend.controller;

import com.learningai.backend.dto.request.ExplainRequest;
import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.dto.response.ExplainResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.LearningProfileService;
import com.learningai.backend.service.scraper.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/learn")
@RequiredArgsConstructor
@Tag(name = "Learn", description = "RAG-powered explanations — any topic")
@SecurityRequirement(name = "bearerAuth")
public class LearnController {

        private final RagService ragService;
        private final LearningProfileService profileService;

        @PostMapping("/explain")
        @Operation(summary = "Get a DNA-aware explanation using RAG pipeline")
        public ResponseEntity<ApiResponse<ExplainResponse>> explain(
                        @AuthenticationPrincipal User user,
                        @Valid @RequestBody ExplainRequest request) {

                RagService.RagResponse rag = ragService.answer(
                                user.getId(),
                                request.getQuestion(),
                                request.getConceptName());

                ExplainResponse response = ExplainResponse.builder()
                                .answer(rag.getAnswer())
                                .conceptName(rag.getConceptName())
                                .topicGoal(rag.getTopicGoal())
                                .sourceType(rag.getSourceType())
                                .freshlyScraped("SCRAPED_FRESH".equals(
                                                rag.getSourceType()))
                                .sources(rag.getSources().stream()
                                                .map(s -> ExplainResponse.SourceDto.builder()
                                                                .title(s.getTitle())
                                                                .url(s.getUrl())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .detectedLanguage(rag.getDetectedLanguage())
                                .languageCode(rag.getLanguageCode())
                                .wasTranslated(rag.isWasTranslated())
                                .build();

                profileService.recordExplanationRead(user.getId());

                return ResponseEntity.ok(ApiResponse.ok(
                                "Explanation ready", response));
        }
}