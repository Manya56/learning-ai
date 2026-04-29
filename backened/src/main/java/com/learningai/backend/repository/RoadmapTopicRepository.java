package com.learningai.backend.repository;

import com.learningai.backend.entity.RoadmapTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoadmapTopicRepository extends JpaRepository<RoadmapTopic, UUID> {

    // All topics for a user ordered by position
    List<RoadmapTopic> findByUserIdOrderByTopicOrderAsc(UUID userId);

    // Find a specific topic by name for a user
    Optional<RoadmapTopic> findByUserIdAndTopicNameIgnoreCase(UUID userId, String topicName);

    // Find current active topic (IN_PROGRESS)
    Optional<RoadmapTopic> findFirstByUserIdAndStatusOrderByTopicOrderAsc(
            UUID userId, RoadmapTopic.TopicStatus status);

    // Find next unlocked topic after a given order index
    Optional<RoadmapTopic> findFirstByUserIdAndTopicOrderGreaterThanAndStatusOrderByTopicOrderAsc(
            UUID userId, int topicOrder, RoadmapTopic.TopicStatus status);

    // Count completed topics
    long countByUserIdAndStatus(UUID userId, RoadmapTopic.TopicStatus status);

    // Check if roadmap already initialized for user
    boolean existsByUserId(UUID userId);

    // Find by user + order index
    Optional<RoadmapTopic> findByUserIdAndTopicOrder(UUID userId, int topicOrder);

    @Query("SELECT r FROM RoadmapTopic r WHERE r.user.id = :userId " +
           "AND r.status IN ('UNLOCKED', 'IN_PROGRESS') " +
           "ORDER BY r.topicOrder ASC")
    List<RoadmapTopic> findAvailableTopics(@Param("userId") UUID userId);
}