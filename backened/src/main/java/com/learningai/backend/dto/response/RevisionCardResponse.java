package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder
public class RevisionCardResponse {

    private UUID cardId;
    private String conceptName;
    private String topicGoal;
    private String status;

    // SM-2 state
    private Double easeFactor;
    private Integer intervalDays;
    private Integer repetitions;
    private Integer lastQuality;
    private LocalDate nextReviewAt;

    // Forgetting curve
    private Double retentionPercent; // 0-100
    private Integer daysSinceLastReview;

    // Is this overdue?
    private boolean overdue;
    private Integer daysOverdue;
}