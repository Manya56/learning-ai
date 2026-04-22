package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The concept being quizzed
    @Column(nullable = false)
    private String conceptName;

    // Difficulty at time of session creation
    @Column(nullable = false)
    private String difficulty;

    // Learning style at time of session creation
    @Column(nullable = false)
    private String learningStyle;

    // All 5 questions stored as JSONB — so we don't hit DB per question
    // Structure: [{questionNumber, question, options[], correctAnswerIndex, explanation, hints[]}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<QuizQuestionData> questions;

    // PENDING → IN_PROGRESS → COMPLETED / ABANDONED
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    // Final score (set on complete)
    private Integer totalCorrect;
    private Integer totalQuestions;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        startedAt = Instant.now();
        if (status == null) status = SessionStatus.IN_PROGRESS;
        if (totalQuestions == null) totalQuestions = 0;
        if (totalCorrect == null)   totalCorrect   = 0;
    }

    public enum SessionStatus {
        IN_PROGRESS, COMPLETED, ABANDONED
    }

    // ── Embedded question data (stored as JSONB) ──────────────────────────
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class QuizQuestionData {
        private int questionNumber;
        private String question;
        private List<String> options;
        private int correctAnswerIndex;
        private String explanation;
        private List<String> hints; // 3 progressive hints
    }
}