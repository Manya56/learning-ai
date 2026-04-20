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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── Goal ──────────────────────────────────────────────────────────────
    @Column(nullable = false)
    private String goal;

    @Column(nullable = false)
    private String goalDescription;

    // ── Language ──────────────────────────────────────────────────────────
    @Column(nullable = false)
    private String preferredLanguage;

    // ── Difficulty: EASY / MEDIUM / HARD ──────────────────────────────────
    @Column(nullable = false)
    private String currentDifficulty;

    // ── Learning style: VISUAL / READING / PRACTICE ───────────────────────
    @Column(nullable = false)
    private String learningStyle;

    // ── Concept maps (JSONB) ──────────────────────────────────────────────
    // {"arrays": 0.3, "recursion": 0.2}  — score 0.0 to 1.0
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> weakConcepts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> strongConcepts;

    // ── Roadmap ───────────────────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> roadmapTopics;

    // Index of current topic in roadmapTopics list
    @Column(nullable = false)
    private Integer currentTopicIndex;

    // ── Behavioral stats ──────────────────────────────────────────────────

    // Rolling average time per question in ms
    @Column(nullable = false)
    private Long avgTimePerQuestionMs;

    // 0.0 to 1.0 — hints used / total questions
    @Column(nullable = false)
    private Double hintUsageRate;

    // 0.0 to 1.0 — correct / total attempts
    @Column(nullable = false)
    private Double overallAccuracy;

    // Total questions attempted across all sessions
    @Column(nullable = false)
    private Integer totalQuestionsAttempted;

    // Consecutive correct answers in current session
    @Column(nullable = false)
    private Integer currentCorrectStreak;

    // Streak of days with activity
    @Column(nullable = false)
    private Integer currentDayStreak;

    @Column
    private Instant lastActiveAt;

    // ── Style inference counters ───────────────────────────────────────────
    // These increment based on behavior and drive style detection

    // How many times user re-read explanations
    @Column(nullable = false)
    private Integer explanationReadCount;

    // How many times user asked for hints
    @Column(nullable = false)
    private Integer hintRequestCount;

    // How many coding problems attempted vs MCQ
    @Column(nullable = false)
    private Integer codingAttemptsCount;

    // How many MCQ attempts
    @Column(nullable = false)
    private Integer mcqAttemptsCount;

    // ── Difficulty adjustment counters ────────────────────────────────────
    // Track recent performance for sliding window difficulty change

    // Correct answers in last 10 questions
    @Column(nullable = false)
    private Integer recentCorrectCount;

    // Total of last 10 questions window
    @Column(nullable = false)
    private Integer recentWindowSize;

    // ── Timestamps ────────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        lastActiveAt = now;
        if (weakConcepts == null)            weakConcepts = Map.of();
        if (strongConcepts == null)          strongConcepts = Map.of();
        if (roadmapTopics == null)           roadmapTopics = List.of();
        if (currentTopicIndex == null)       currentTopicIndex = 0;
        if (avgTimePerQuestionMs == null)    avgTimePerQuestionMs = 0L;
        if (hintUsageRate == null)           hintUsageRate = 0.0;
        if (overallAccuracy == null)         overallAccuracy = 0.0;
        if (totalQuestionsAttempted == null) totalQuestionsAttempted = 0;
        if (currentCorrectStreak == null)    currentCorrectStreak = 0;
        if (currentDayStreak == null)        currentDayStreak = 0;
        if (explanationReadCount == null)    explanationReadCount = 0;
        if (hintRequestCount == null)        hintRequestCount = 0;
        if (codingAttemptsCount == null)     codingAttemptsCount = 0;
        if (mcqAttemptsCount == null)        mcqAttemptsCount = 0;
        if (recentCorrectCount == null)      recentCorrectCount = 0;
        if (recentWindowSize == null)        recentWindowSize = 0;
        if (learningStyle == null)           learningStyle = "PRACTICE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}