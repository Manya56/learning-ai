package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProfileStatsResponse {

    private String currentDifficulty;
    private String learningStyle;
    private Double overallAccuracy;
    private Integer totalQuestionsAttempted;
    private Long avgTimePerQuestionMs;
    private Double hintUsageRate;
    private Integer currentDayStreak;
    private Integer currentCorrectStreak;

    // Top 3 weakest concepts
    private List<Map.Entry<String, Double>> top3WeakConcepts;

    // Top 3 strongest concepts
    private List<Map.Entry<String, Double>> top3StrongConcepts;

    // Current topic in roadmap
    private String currentTopic;
    private Integer currentTopicIndex;
    private Integer totalTopics;

    // Progress percentage
    private Double roadmapProgressPercent;
}