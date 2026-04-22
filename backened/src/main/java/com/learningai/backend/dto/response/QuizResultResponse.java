package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class QuizResultResponse {
    private UUID sessionId;
    private String conceptName;
    private int totalQuestions;
    private int totalCorrect;
    private double accuracyPercent;
    private long timeTakenMs;
    private String difficulty;         // difficulty when session started
    private String updatedDifficulty;  // difficulty after DNA update
    private boolean difficultyChanged; // true if engine changed it
}