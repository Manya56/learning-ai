package com.learningai.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {

    @NotNull
    @Min(0) @Max(4)
    private Integer questionIndex;

    @NotNull
    @Min(0) @Max(3)
    private Integer selectedAnswerIndex;

    @NotNull
    @Min(0)
    private Long timeTakenMs;
}