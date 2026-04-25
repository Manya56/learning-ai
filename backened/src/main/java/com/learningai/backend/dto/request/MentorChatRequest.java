package com.learningai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MentorChatRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message too long")
    private String message;

    // Optional — user can set mentor personality
    // ENCOURAGING / STRICT / SOCRATIC
    // If null — uses user's profile setting
    private String personality;

    // Optional — start fresh session
    private boolean newSession = false;
}