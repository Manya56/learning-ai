package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CodingAttemptResponse {
    private UUID id;
    private String conceptName;
    private String problemStatement;
    private String problemType;
    private String language;
    private String userSubmission;
    private Integer score;
    private String feedback;
    private Boolean passed;
    private Long timeTakenMs;
    private String difficulty;
    private Instant attemptedAt;
}