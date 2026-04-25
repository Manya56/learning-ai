package com.learningai.backend.repository;

import com.learningai.backend.entity.RevisionCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevisionCardRepository
        extends JpaRepository<RevisionCard, UUID> {

    // All cards due today or overdue
    @Query("SELECT r FROM RevisionCard r " +
           "WHERE r.user.id = :userId " +
           "AND r.nextReviewAt <= :today " +
           "AND r.status = 'ACTIVE' " +
           "ORDER BY r.nextReviewAt ASC")
    List<RevisionCard> findDueCards(
            @Param("userId") UUID userId,
            @Param("today")  LocalDate today);

    // Find specific card for a user + concept
    Optional<RevisionCard> findByUserIdAndConceptName(
            UUID userId, String conceptName);

    // All cards for a user
    List<RevisionCard> findByUserIdOrderByNextReviewAtAsc(UUID userId);

    // Count due cards
    @Query("SELECT COUNT(r) FROM RevisionCard r " +
           "WHERE r.user.id = :userId " +
           "AND r.nextReviewAt <= :today " +
           "AND r.status = 'ACTIVE'")
    long countDueCards(
            @Param("userId") UUID userId,
            @Param("today")  LocalDate today);

    // All active users with due cards — for scheduler
    @Query("SELECT DISTINCT r.user.id FROM RevisionCard r " +
           "WHERE r.nextReviewAt <= :today " +
           "AND r.status = 'ACTIVE'")
    List<UUID> findUserIdsWithDueCards(
            @Param("today") LocalDate today);

    // Stats
    long countByUserIdAndStatus(UUID userId,
            RevisionCard.CardStatus status);
}