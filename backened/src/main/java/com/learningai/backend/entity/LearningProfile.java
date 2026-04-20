package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "learning_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Link to User — one profile per user
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // What the user wants to learn
    @Column(nullable = false)
    private String goal;

    // e.g. "Learn DSA", "Master Spring Boot", "Crack system design"
    @Column(nullable = false)
    private String goalDescription;

    // Primary language for content delivery
    @Column(nullable = false)
    private String preferredLanguage;

    // EASY, MEDIUM, HARD — set after knowledge quiz
    @Column(nullable = false)
    private String currentDifficulty;

    // Learning style inferred from behavior: VISUAL, READING, PRACTICE
    @Column
    private String learningStyle;

    // JSONB — list of weak concept IDs with scores
    // e.g. {"arrays": 0.3, "recursion": 0.2}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> weakConcepts;

    // JSONB — list of strong concept IDs with scores
    // e.g. {"loops": 0.9, "variables": 0.95}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> strongConcepts;

    // JSONB — topics in the roadmap
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> roadmapTopics;

    // Average time per question in milliseconds
    @Column
    private Long avgTimePerQuestionMs;

    // Ratio of hints used vs total questions (0.0 to 1.0)
    @Column
    private Double hintUsageRate;

    // Overall accuracy across all attempts (0.0 to 1.0)
    @Column
    private Double overallAccuracy;

    // Total questions attempted
    @Column
    private Integer totalQuestionsAttempted;

    // Current learning streak in days
    @Column
    private Integer currentStreak;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (weakConcepts == null)   weakConcepts = Map.of();
        if (strongConcepts == null) strongConcepts = Map.of();
        if (roadmapTopics == null)  roadmapTopics = List.of();
        if (avgTimePerQuestionMs == null) avgTimePerQuestionMs = 0L;
        if (hintUsageRate == null)        hintUsageRate = 0.0;
        if (overallAccuracy == null)      overallAccuracy = 0.0;
        if (totalQuestionsAttempted == null) totalQuestionsAttempted = 0;
        if (currentStreak == null)        currentStreak = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}