package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfileStatsResponse {

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ConceptScore {
        private String concept;
        private Double score;
    }

    private String currentDifficulty;
    private String learningStyle;
    private Double overallAccuracy;
    private Integer totalQuestionsAttempted;
    private Long avgTimePerQuestionMs;
    private Double hintUsageRate;
    private Integer currentDayStreak;
    private Integer currentCorrectStreak;

    // Top 3 weakest concepts
    private List<ConceptScore> top3WeakConcepts;
    private List<ConceptScore> top3StrongConcepts;

    // Current topic in roadmap
    private String currentTopic;
    private Integer currentTopicIndex;
    private Integer totalTopics;

    // Progress percentage
    private Double roadmapProgressPercent;
}