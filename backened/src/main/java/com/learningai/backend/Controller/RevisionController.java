package com.learningai.backend.controller;

import com.learningai.backend.dto.request.RevisionCompleteRequest;
import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.dto.response.RevisionCardResponse;
import com.learningai.backend.dto.response.RevisionStatsResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.SpacedRepetitionService;
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
@RequestMapping("/api/revision")
@RequiredArgsConstructor
@Tag(name = "Revision", description = "Spaced repetition — SM-2 + Ebbinghaus forgetting curve")
@SecurityRequirement(name = "bearerAuth")
public class RevisionController {

    private final SpacedRepetitionService revisionService;

    @GetMapping("/due")
    @Operation(summary = "Get due cards for today + stats + 7-day forecast")
    public ResponseEntity<ApiResponse<RevisionStatsResponse>> getDue(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(
                revisionService.getDueCards(user.getId())));
    }

    @PostMapping("/complete")
    @Operation(summary = "Submit review result — updates SM-2 state")
    public ResponseEntity<ApiResponse<RevisionCardResponse>> complete(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RevisionCompleteRequest request) {

        RevisionCardResponse result = revisionService.completeRevision(
                user.getId(),
                request.getConceptName(),
                request.getQuality());

        return ResponseEntity.ok(ApiResponse.ok(
                "Review recorded", result));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all revision cards with retention scores")
    public ResponseEntity<ApiResponse<List<RevisionCardResponse>>> getAll(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(
                revisionService.getAllCards(user.getId())));
    }
}