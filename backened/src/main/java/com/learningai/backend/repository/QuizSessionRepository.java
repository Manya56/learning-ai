package com.learningai.backend.repository;

import com.learningai.backend.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizSessionRepository
        extends JpaRepository<QuizSession, UUID> {

    // Last 10 sessions for a user
    List<QuizSession> findTop10ByUserIdOrderByStartedAtDesc(UUID userId);

    // Active session for a user (should only ever be 1)
    Optional<QuizSession> findByUserIdAndStatus(
            UUID userId, QuizSession.SessionStatus status);

    // Count sessions for a concept
    long countByUserIdAndConceptName(UUID userId, String conceptName);
}