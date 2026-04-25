package com.learningai.backend.repository;

import com.learningai.backend.entity.MentorSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentorSessionRepository
        extends JpaRepository<MentorSession, UUID> {

    // Latest active session for user
    Optional<MentorSession> findTopByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, MentorSession.SessionStatus status);

    // Last 10 sessions for history
    List<MentorSession> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    // Count sessions
    long countByUserId(UUID userId);
}