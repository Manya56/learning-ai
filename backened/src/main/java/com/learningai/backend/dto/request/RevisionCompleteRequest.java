package com.learningai.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RevisionCompleteRequest {

    @NotBlank(message = "Concept name is required")
    private String conceptName;

    // SM-2 quality rating 0-5:
    // 0 = complete blackout
    // 1 = wrong, but remembered on seeing answer
    // 2 = wrong, but easy to recall
    // 3 = correct with significant difficulty
    // 4 = correct after hesitation
    // 5 = perfect, instant recall
    @NotNull
    @Min(0) @Max(5)
    private Integer quality;
}