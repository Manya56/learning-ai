package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class LearningProfileResponse {

    private UUID profileId;
    private UUID userId;
    private String fullName;
    private String email;
    private String goal;
    private String goalDescription;
    private String preferredLanguage;
    private String currentDifficulty;
    private String learningStyle;
    private Map<String, Double> weakConcepts;
    private Map<String, Double> strongConcepts;
    private List<String> roadmapTopics;
    private Long avgTimePerQuestionMs;
    private Double hintUsageRate;
    private Double overallAccuracy;
    private Integer totalQuestionsAttempted;
    private Integer currentStreak;
    private Instant createdAt;
    private Instant updatedAt;
}