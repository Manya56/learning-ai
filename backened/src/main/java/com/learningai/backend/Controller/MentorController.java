package com.learningai.backend.controller;

import com.learningai.backend.dto.request.MentorChatRequest;
import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.dto.response.MentorChatResponse;
import com.learningai.backend.dto.response.MentorHistoryResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.MentorService;
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
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
@Tag(name = "Mentor", description = "Aria — AI mentor with memory and personality")
@SecurityRequirement(name = "bearerAuth")
public class MentorController {

    private final MentorService mentorService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with Aria — DNA-aware, remembers context")
    public ResponseEntity<ApiResponse<MentorChatResponse>> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody MentorChatRequest request) {

        MentorChatResponse response = mentorService.chat(
                user.getId(),
                request.getMessage(),
                request.getPersonality(),
                request.isNewSession());

        return ResponseEntity.ok(ApiResponse.ok(
                "Aria replied", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get last 10 mentor sessions with messages")
    public ResponseEntity<ApiResponse<List<MentorHistoryResponse>>> history(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(
                mentorService.getHistory(user.getId())));
    }

    @DeleteMapping("/context")
    @Operation(summary = "Clear conversation context — start fresh")
    public ResponseEntity<ApiResponse<Void>> clearContext(
            @AuthenticationPrincipal User user) {

        mentorService.clearContext(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(
                "Context cleared", null));
    }
}