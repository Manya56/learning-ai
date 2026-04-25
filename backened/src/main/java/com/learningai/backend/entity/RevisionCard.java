package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "revision_cards",
       indexes = {
           @Index(name = "idx_revision_user",
                  columnList = "user_id"),
           @Index(name = "idx_revision_next_review",
                  columnList = "next_review_at"),
           @Index(name = "idx_revision_user_concept",
                  columnList = "user_id, concept_name",
                  unique = true)
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RevisionCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Concept being tracked
    @Column(nullable = false)
    private String conceptName;

    // Topic this concept belongs to
    @Column
    private String topicGoal;

    // ── SM-2 Algorithm fields ─────────────────────────────────────────────

    // Ease factor — starts at 2.5, min 1.3
    // Higher = easier = reviewed less frequently
    @Column(nullable = false)
    private Double easeFactor;

    // Current interval in days
    // 1 → 6 → easeFactor*prev → ...
    @Column(nullable = false)
    private Integer intervalDays;

    // How many times reviewed successfully
    @Column(nullable = false)
    private Integer repetitions;

    // Quality of last review 0-5
    // 0-2 = failed, 3-5 = passed
    @Column
    private Integer lastQuality;

    // Next scheduled review date
    @Column(nullable = false)
    private LocalDate nextReviewAt;

    // ── Forgetting curve fields ───────────────────────────────────────────

    // Stability — how long memory lasts (in days)
    // Used for Ebbinghaus R = e^(-t/S)
    @Column(nullable = false)
    private Double stability;

    // Last time this was reviewed
    @Column
    private Instant lastReviewedAt;

    // Total review count
    @Column(nullable = false)
    private Integer totalReviews;

    // ── Status ────────────────────────────────────────────────────────────

    // ACTIVE / MASTERED / SUSPENDED
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt   = now;
        updatedAt   = now;
        if (easeFactor == null)   easeFactor   = 2.5;
        if (intervalDays == null) intervalDays = 1;
        if (repetitions == null)  repetitions  = 0;
        if (totalReviews == null) totalReviews  = 0;
        if (stability == null)    stability     = 1.0;
        if (status == null)       status        = CardStatus.ACTIVE;
        if (nextReviewAt == null) nextReviewAt  = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public enum CardStatus {
        ACTIVE,    // Being reviewed
        MASTERED,  // easeFactor >= 3.0 && intervalDays >= 21
        SUSPENDED  // User chose to skip
    }
}