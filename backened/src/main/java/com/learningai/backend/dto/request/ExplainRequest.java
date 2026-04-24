package com.learningai.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExplainRequest {

    @NotBlank(message = "Concept name is required")
    private String conceptName;

    @NotBlank(message = "Question is required")
    private String question;

    // Optional — override user's preferred language
    private String language;
}