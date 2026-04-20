package com.learningai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnboardingRequest {

    @NotBlank(message = "Goal is required")
    // e.g. "DSA", "Spring Boot", "System Design", "Python"
    private String goal;

    @NotBlank(message = "Goal description is required")
    // e.g. "I want to crack FAANG interviews in 3 months"
    private String goalDescription;

    @NotBlank(message = "Preferred language is required")
    // e.g. "English", "Hindi", "Spanish"
    private String preferredLanguage;

    @NotNull(message = "Prior knowledge level is required")
    // 1 = Complete beginner, 2 = Some knowledge, 3 = Intermediate
    private Integer priorKnowledgeLevel;
}