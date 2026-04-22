package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coding_attempts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CodingAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The concept/topic this problem is about
    @Column(nullable = false)
    private String conceptName;

    // The full problem statement shown to user
    @Column(columnDefinition = "TEXT", nullable = false)
    private String problemStatement;

    // CODING, MATH, WRITTEN, DIAGRAM_EXPLAIN
    @Column(nullable = false)
    private String problemType;

    // java / python / text / math
    @Column(nullable = false)
    private String language;

    // What the user submitted
    @Column(columnDefinition = "TEXT", nullable = false)
    private String userSubmission;

    // AI score 0-10
    @Column(nullable = false)
    private Integer score;

    // Full AI feedback — strengths, issues, suggestions
    @Column(columnDefinition = "TEXT")
    private String feedback;

    // Whether all test cases / criteria passed
    @Column(nullable = false)
    private Boolean passed;

    // Time from problem shown to submission
    @Column(nullable = false)
    private Long timeTakenMs;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false, updatable = false)
    private Instant attemptedAt;

    @PrePersist
    public void prePersist() {
        attemptedAt = Instant.now();
        if (passed == null) passed = false;
    }
}