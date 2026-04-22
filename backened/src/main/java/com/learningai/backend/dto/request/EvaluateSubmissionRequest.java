package com.learningai.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluateSubmissionRequest {

    @NotBlank(message = "Problem statement is required")
    private String problemStatement;

    @NotBlank(message = "Submission is required")
    private String userSubmission;

    @NotBlank(message = "Concept name is required")
    private String conceptName;

    // CODING / MATH / WRITTEN
    @NotBlank
    private String problemType;

    // java / python / text / math
    @NotBlank
    private String language;

    @NotNull
    @Min(0)
    private Long timeTakenMs;
}