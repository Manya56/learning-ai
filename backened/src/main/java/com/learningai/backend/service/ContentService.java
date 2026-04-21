package com.learningai.backend.service;

import com.learningai.backend.dto.response.ConceptResponse;
import com.learningai.backend.dto.response.TopicResponse;
import com.learningai.backend.entity.Concept;
import com.learningai.backend.entity.Topic;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.ConceptRepository;
import com.learningai.backend.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;

    // ─── Get all topics ───────────────────────────────────────────────────

    @Cacheable("topics")
    public List<TopicResponse> getAllTopics() {
        return topicRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(this::mapTopicNoConceptList)
                .collect(Collectors.toList());
    }

    // ─── Get topics by category ───────────────────────────────────────────

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
                .orElseThrow(() -> AppException.notFound(
                        "Topic not found"));

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

    // ─── Get concepts filtered by difficulty ──────────────────────────────

    public List<ConceptResponse> getConceptsByDifficulty(
            UUID topicId, String difficulty) {

        return conceptRepository
                .findByTopicIdAndDifficultyLevelOrderByOrderIndexAsc(
                        topicId, difficulty.toUpperCase())
                .stream()
                .map(this::mapConcept)
                .collect(Collectors.toList());
    }

    // ─── Get single concept ───────────────────────────────────────────────

    public ConceptResponse getConcept(UUID conceptId) {
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> AppException.notFound(
                        "Concept not found"));
        return mapConcept(concept);
    }

    // ─── Search concepts by name ──────────────────────────────────────────

    public List<ConceptResponse> searchConcepts(String query) {
        return conceptRepository
                .findByNameContainingIgnoreCase(query)
                .stream()
                .map(this::mapConcept)
                .collect(Collectors.toList());
    }

    // ─── Get next concept for user (based on Learning DNA) ───────────────

    public ConceptResponse getNextConcept(
            String currentDifficulty,
            List<String> weakConceptNames,
            List<String> completedConceptIds) {

        // First try to find a weak concept at current difficulty
        for (String weakName : weakConceptNames) {
            List<Concept> matches = conceptRepository
                    .findByNameContainingIgnoreCase(weakName);

            for (Concept c : matches) {
                if (c.getDifficultyLevel()
                        .equals(currentDifficulty)
                    && !completedConceptIds.contains(
                            c.getId().toString())) {
                    return mapConcept(c);
                }
            }
        }

        // Fallback — find any concept at current difficulty
        // not yet completed
        List<Concept> atDifficulty = conceptRepository
                .findByDifficultyLevelOrderByOrderIndexAsc(
                        currentDifficulty);

        return atDifficulty.stream()
                .filter(c -> !completedConceptIds.contains(
                        c.getId().toString()))
                .map(this::mapConcept)
                .findFirst()
                .orElseThrow(() -> AppException.notFound(
                        "No more concepts at this difficulty"));
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