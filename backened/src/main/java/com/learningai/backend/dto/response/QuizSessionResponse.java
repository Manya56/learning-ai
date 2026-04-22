package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class QuizSessionResponse {
    private UUID sessionId;
    private String conceptName;
    private String difficulty;
    private String learningStyle;
    private List<QuizQuestionPublicDto> questions;
    private String status;
    private Integer totalQuestions;
    private Instant startedAt;
}