package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_stats",
       indexes = {
           @Index(name = "idx_daily_stats_user_date",
                  columnList = "user_id, stat_date",
                  unique = true)
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    private Integer questionsAttempted;

    @Column(nullable = false)
    private Double accuracy;

    @Column(nullable = false)
    private Long timeSpentMs;

    @Column(nullable = false)
    private Integer conceptsStudied;

    @Column(nullable = false)
    private Integer revisionsDone;

    @Column(nullable = false)
    private Integer streakDay;

    @Column
    private String topConceptStudied;

    @Column(nullable = false)
    private String difficulty;

    @PrePersist
    public void prePersist() {
        if (questionsAttempted == null) questionsAttempted = 0;
        if (accuracy == null)           accuracy           = 0.0;
        if (timeSpentMs == null)        timeSpentMs        = 0L;
        if (conceptsStudied == null)    conceptsStudied    = 0;
        if (revisionsDone == null)      revisionsDone      = 0;
        if (streakDay == null)          streakDay          = 0;
        if (difficulty == null)         difficulty         = "EASY";
    }
}