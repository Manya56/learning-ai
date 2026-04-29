package com.learningai.backend.service;

import com.learningai.backend.dto.response.ConceptResponse;
import com.learningai.backend.dto.response.TopicResponse;
import com.learningai.backend.entity.Concept;
import com.learningai.backend.entity.Topic;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.ConceptRepository;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ContentService
 *
 * FIX Issue 1: /api/content/topics was always returning DSA topics
 * regardless of the user's goal because the category filter was only
 * used when an explicit category was passed. The default getAllTopics()
 * returned ALL topics ordered by index.
 *
 * NEW: Added getTopicsForUser(userId) which reads the user's goal from
 * their LearningProfile and returns only topics matching that goal's category.
 * The controller now uses this method when no category is specified.
 *
 * Also: topics are seeded only for DSA right now. For non-DSA goals
 * (Finance, Dance, etc.) the DB will have no matching rows — so we return
 * an empty list with a clear message rather than returning DSA topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final TopicRepository           topicRepository;
    private final ConceptRepository         conceptRepository;
    private final LearningProfileRepository profileRepository;

    // ─── Get topics for the current user's goal ───────────────────────────
    // NEW: primary entry point — replaces getAllTopics() for authenticated users

    public List<TopicResponse> getTopicsForUser(UUID userId) {
        String category = profileRepository.findByUserId(userId)
                .map(p -> p.getGoal())
                .orElse(null);

        if (category == null) {
            // No profile yet — return all (onboarding not done)
            return getAllTopics();
        }

        List<TopicResponse> topics = topicRepository
                .findByCategoryOrderByOrderIndexAsc(category)
                .stream()
                .map(this::mapTopicNoConceptList)
                .collect(Collectors.toList());

        if (topics.isEmpty()) {
            // Goal exists but no seeded topics for it yet (e.g. Finance, Dance)
            // Return roadmap topics from profile as lightweight responses
            log.info("No DB topics for category '{}' — returning roadmap topics", category);
            return profileRepository.findByUserId(userId)
                    .map(p -> p.getRoadmapTopics() != null
                            ? p.getRoadmapTopics().stream()
                                    .map(name -> TopicResponse.builder()
                                            .name(name)
                                            .category(p.getGoal())
                                            .description("AI-generated roadmap topic")
                                            .conceptCount(0L)
                                            .build())
                                    .collect(Collectors.toList())
                            : List.<TopicResponse>of())
                    .orElse(List.of());
        }

        return topics;
    }

    // ─── Get all topics (admin / no auth context) ─────────────────────────

    @Cacheable("topics")
    public List<TopicResponse> getAllTopics() {
        return topicRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(this::mapTopicNoConceptList)
                .collect(Collectors.toList());
    }

    // ─── Get topics by explicit category ─────────────────────────────────

    @Cacheable(value = "topics", key = "#category")
    public List<TopicResponse> getTopicsByCategory(String category) {
        return topicRepository
                .findByCategoryOrderByOrderIndexAsc(category)
                .stream()
                .map(this::mapTopicNoConceptList)
                .collect(Collectors.toList());
    }

    // ─── Get single topic with all concepts ───────────────────────────────

    public TopicResponse getTopicWithConcepts(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> AppException.notFound("Topic not found"));

        List<ConceptResponse> concepts = conceptRepository
                .findByTopicIdOrderByOrderIndexAsc(topicId)
                .stream()
                .map(this::mapConcept)
                .collect(Collectors.toList());

        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .category(topic.getCategory())
                .description(topic.getDescription())
                .orderIndex(topic.getOrderIndex())
                .conceptCount((long) concepts.size())
                .concepts(concepts)
                .build();
    }

    public List<ConceptResponse> getConceptsByDifficulty(UUID topicId, String difficulty) {
        return conceptRepository
                .findByTopicIdAndDifficultyLevelOrderByOrderIndexAsc(topicId, difficulty.toUpperCase())
                .stream()
                .map(this::mapConcept)
                .collect(Collectors.toList());
    }

    public ConceptResponse getConcept(UUID conceptId) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> AppException.notFound("Concept not found"));
        return mapConcept(concept);
    }

    public List<ConceptResponse> searchConcepts(String query) {
        return conceptRepository
                .findByNameContainingIgnoreCase(query)
                .stream()
                .map(this::mapConcept)
                .collect(Collectors.toList());
    }

    // ─── Mappers ──────────────────────────────────────────────────────────

    private TopicResponse mapTopicNoConceptList(Topic t) {
        return TopicResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .category(t.getCategory())
                .description(t.getDescription())
                .orderIndex(t.getOrderIndex())
                .conceptCount(conceptRepository.countByTopicId(t.getId()))
                .build();
    }

    private ConceptResponse mapConcept(Concept c) {
        return ConceptResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .difficultyLevel(c.getDifficultyLevel())
                .orderIndex(c.getOrderIndex())
                .estimatedMinutes(c.getEstimatedMinutes())
                .tags(c.getTags())
                .topicId(c.getTopic().getId())
                .topicName(c.getTopic().getName())
                .build();
    }
}