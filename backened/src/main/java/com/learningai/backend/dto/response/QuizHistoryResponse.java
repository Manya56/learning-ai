package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class QuizHistoryResponse {
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
}