package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningStyleResponse {

    private String currentStyle;
    private String previousStyle;
    private boolean styleChanged;

    // Scores for each style (0.0 to 1.0)
    private double visualScore;
    private double readingScore;
    private double practiceScore;

    private String reasoning;
}