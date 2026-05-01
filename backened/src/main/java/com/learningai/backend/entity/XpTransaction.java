package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * XpTransaction — one row per XP-earning event.
 *
 * XP rules:
 *   correct answer (no hint)  = 15 XP
 *   correct answer (hint used) = 10 XP
 *   wrong answer               =  0 XP
 *   concept mastered           = 50 XP
 *   topic completed            = 200 XP
 *   revision completed         = 8 XP
 *   day 7 streak bonus         = 2x multiplier on that day's XP
 */
@Entity
@Table(name = "xp_transactions",
       indexes = {
           @Index(name = "idx_xp_user",      columnList = "user_id"),
           @Index(name = "idx_xp_user_week", columnList = "user_id, week_number")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // XP earned in this event
    @Column(nullable = false)
    private Integer xpEarned;

    // Reason: CORRECT_ANSWER / CORRECT_NO_HINT / CONCEPT_MASTERED /
    //         TOPIC_COMPLETED / REVISION / STREAK_BONUS
    @Column(nullable = false)
    private String reason;

    // e.g. concept name, topic name
    @Column
    private String context;

    // ISO week number (for weekly leaderboard partitioning)
    // e.g. "2026-W18"
    @Column(nullable = false)
    private String weekNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}