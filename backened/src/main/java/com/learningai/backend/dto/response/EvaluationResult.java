package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class EvaluationResult {

    private UUID attemptId;       // set after saving
    private int score;            // 0-10
    private boolean passed;       // score >= 6
    private String strengths;
    private String issues;
    private String suggestions;
    private String correctedSolution;
    private List<LineFeedback> lineFeedback;

    @Data @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LineFeedback {
        private String line;
        private String comment;
    }
}