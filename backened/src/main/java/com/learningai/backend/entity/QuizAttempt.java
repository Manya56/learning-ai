package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Which question index (0-based) in the session
    @Column(nullable = false)
    private Integer questionIndex;

    // Which option index the user picked (0-3)
    @Column(nullable = false)
    private Integer selectedAnswerIndex;

    @Column(nullable = false)
    private Boolean correct;

    // Time taken in milliseconds
    @Column(nullable = false)
    private Long timeTakenMs;

    // How many hints were used for this question (0, 1, 2, or 3)
    @Column(nullable = false)
    private Integer hintsUsed;

    // Concept name — denormalized for fast DNA updates
    @Column(nullable = false)
    private String conceptName;

    @Column(nullable = false, updatable = false)
    private Instant answeredAt;

    @PrePersist
    public void prePersist() {
        answeredAt = Instant.now();
        if (hintsUsed == null) hintsUsed = 0;
    }
}