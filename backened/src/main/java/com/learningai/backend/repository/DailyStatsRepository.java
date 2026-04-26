package com.learningai.backend.repository;

import com.learningai.backend.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyStatsRepository
        extends JpaRepository<DailyStats, UUID> {

    Optional<DailyStats> findByUserIdAndStatDate(
            UUID userId, LocalDate date);

    List<DailyStats> findByUserIdAndStatDateBetweenOrderByStatDateAsc(
            UUID userId, LocalDate from, LocalDate to);

    // Last 90 days for heatmap
    @Query("SELECT d FROM DailyStats d " +
           "WHERE d.user.id = :userId " +
           "AND d.statDate >= :from " +
           "ORDER BY d.statDate ASC")
    List<DailyStats> findRecentStats(UUID userId, LocalDate from);
}