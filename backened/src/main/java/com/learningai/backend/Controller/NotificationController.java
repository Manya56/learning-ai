package com.learningai.backend.controller;

import com.learningai.backend.dto.response.ApiResponse;
import com.learningai.backend.entity.User;
import com.learningai.backend.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Email notification management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final EmailNotificationService emailService;

    @PostMapping("/test/revision")
    @Operation(summary = "Send yourself a test revision reminder email")
    public ResponseEntity<ApiResponse<Void>> testRevision(
            @AuthenticationPrincipal User user) {
        emailService.sendRevisionReminder(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Test revision email sent", null));
    }

    @PostMapping("/test/streak")
    @Operation(summary = "Send yourself a test streak reminder email")
    public ResponseEntity<ApiResponse<Void>> testStreak(
            @AuthenticationPrincipal User user) {
        emailService.sendStreakReminder(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Test streak email sent", null));
    }

    @PostMapping("/test/motivation")
    @Operation(summary = "Send yourself a test motivational email")
    public ResponseEntity<ApiResponse<Void>> testMotivation(
            @AuthenticationPrincipal User user) {
        emailService.sendMotivationalMessage(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Test motivational email sent", null));
    }
}