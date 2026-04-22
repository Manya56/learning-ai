package com.learningai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateProblemRequest {

    @NotBlank(message = "Concept name is required")
    private String conceptName;

    // Optional — defaults to user's goal from Learning DNA
    private String topicGoal;

    // Optional — defaults to user's current difficulty
    private String difficulty;

    // Optional — java / python / text
    private String language;
}