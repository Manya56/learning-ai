package com.learningai.backend.repository;

import com.learningai.backend.entity.CodingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodingAttemptRepository
        extends JpaRepository<CodingAttempt, UUID> {

    List<CodingAttempt> findTop10ByUserIdOrderByAttemptedAtDesc(UUID userId);

    List<CodingAttempt> findByUserIdAndConceptNameOrderByAttemptedAtDesc(
            UUID userId, String conceptName);

    // Average score for a user on a concept
    @Query("SELECT AVG(a.score) FROM CodingAttempt a " +
           "WHERE a.user.id = :userId " +
           "AND a.conceptName = :conceptName")
    Double avgScoreByUserAndConcept(UUID userId, String conceptName);

    // Count passed attempts
    long countByUserIdAndPassed(UUID userId, boolean passed);
}