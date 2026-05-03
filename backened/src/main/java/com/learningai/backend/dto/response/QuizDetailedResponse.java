package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuizDetailedResponse {
    private UUID sessionId;
    private String conceptName;
    private String difficulty;
    private String learningStyle;
    private Integer totalQuestions;
    private Integer totalCorrect;
    private Double accuracyPercent;
    private Long timeTakenMs;
    private Instant startedAt;
    private Instant completedAt;
    private String status;
    private List<QuizQuestionWithAnswer> questions;

    @Data
    @Builder
    public static class QuizQuestionWithAnswer {
        private int questionIndex;
        private String question;
        private List<String> options;
        private int correctAnswerIndex;
        private Integer selectedAnswerIndex; // null if not answered
        private String explanation;
        private boolean answeredCorrectly;
    }
}