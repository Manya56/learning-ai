package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mentor_sessions",
       indexes = {
           @Index(name = "idx_mentor_session_user",
                  columnList = "user_id")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MentorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Full conversation history stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<MentorMessage> messages;

    // Snapshot of user DNA at session start
    // Stored so we can show "you improved since last chat"
    @Column
    private String snapshotDifficulty;

    @Column
    private String snapshotLearningStyle;

    @Column
    private Double snapshotAccuracy;

    // Session topic — what was mainly discussed
    @Column
    private String sessionTopic;

    // ACTIVE / CLOSED
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant lastMessageAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        lastMessageAt = Instant.now();
        if (status == null) status = SessionStatus.ACTIVE;
    }

    public enum SessionStatus {
        ACTIVE, CLOSED
    }

    // ── Embedded message ──────────────────────────────────────────────────
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class MentorMessage {
        private String role;      // "user" or "assistant"
        private String content;
        private Instant timestamp;
    }
}