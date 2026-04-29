package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "roadmap_topics",
       indexes = {
           @Index(name = "idx_roadmap_user",      columnList = "user_id"),
           @Index(name = "idx_roadmap_user_order", columnList = "user_id, topic_order")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoadmapTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Topic name e.g. "Arrays", "Financial Statements", "Chord Progressions"
    @Column(nullable = false)
    private String topicName;

    // Goal this topic belongs to e.g. "DSA", "Finance", "Music"
    @Column(nullable = false)
    private String goal;

    // Position in the roadmap (0-based)
    @Column(nullable = false)
    private Integer topicOrder;

    // LOCKED / UNLOCKED / IN_PROGRESS / COMPLETED
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TopicStatus status;

    // Concept names that are part of this topic (AI-generated)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> concepts;

    // Which concepts user has completed (subset of concepts)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> completedConcepts;

    // Aggregate scores for this topic (0.0 to 1.0)
    @Column(nullable = false)
    private Double quizScore;

    @Column(nullable = false)
    private Double practiceScore;

    // Progress 0-100
    @Column(nullable = false)
    private Integer progressPercent;

    // How many quizzes taken on this topic
    @Column(nullable = false)
    private Integer quizzesTaken;

    // How many practice problems attempted
    @Column(nullable = false)
    private Integer practiceAttempts;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null)           status           = TopicStatus.LOCKED;
        if (concepts == null)         concepts         = List.of();
        if (completedConcepts == null) completedConcepts = List.of();
        if (quizScore == null)        quizScore        = 0.0;
        if (practiceScore == null)    practiceScore    = 0.0;
        if (progressPercent == null)  progressPercent  = 0;
        if (quizzesTaken == null)     quizzesTaken     = 0;
        if (practiceAttempts == null) practiceAttempts = 0;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

    public enum TopicStatus {
        LOCKED,       // not yet reachable
        UNLOCKED,     // reachable but not started
        IN_PROGRESS,  // user has started
        COMPLETED     // user finished (progressPercent >= 80)
    }
}