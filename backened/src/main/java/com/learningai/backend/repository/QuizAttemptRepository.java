package com.learningai.backend.repository;

import com.learningai.backend.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findBySessionIdOrderByQuestionIndexAsc(UUID sessionId);

    List<QuizAttempt> findByUserIdOrderByAnsweredAtDesc(UUID userId);

    // Accuracy for a specific concept
    @Query("SELECT COUNT(a) FROM QuizAttempt a " +
           "WHERE a.user.id = :userId " +
           "AND a.conceptName = :concept " +
           "AND a.correct = true")
    long countCorrectByUserAndConcept(UUID userId, String concept);
}