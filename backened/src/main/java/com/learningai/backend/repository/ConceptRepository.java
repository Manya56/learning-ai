package com.learningai.backend.repository;

import com.learningai.backend.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, UUID> {

    List<Concept> findByTopicIdOrderByOrderIndexAsc(UUID topicId);

    List<Concept> findByTopicIdAndDifficultyLevelOrderByOrderIndexAsc(
            UUID topicId, String difficultyLevel);

    List<Concept> findByDifficultyLevelOrderByOrderIndexAsc(
            String difficultyLevel);

    // Find concepts by name — used in Learning DNA concept tracking
    List<Concept> findByNameContainingIgnoreCase(String name);

    // Get all concepts for a category
    @Query("SELECT c FROM Concept c " +
           "JOIN c.topic t " +
           "WHERE t.category = :category " +
           "ORDER BY t.orderIndex ASC, c.orderIndex ASC")
    List<Concept> findByCategoryOrdered(String category);

    // Count concepts per topic
    long countByTopicId(UUID topicId);
}