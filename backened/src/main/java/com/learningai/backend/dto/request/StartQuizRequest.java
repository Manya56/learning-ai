package com.learningai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartQuizRequest {
    @NotBlank(message = "Concept name is required")
    private String conceptName;
}