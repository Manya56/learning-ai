package com.learningai.backend.service;

import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.RoadmapTopic;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.RoadmapTopicRepository;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RoadmapService — the engine that drives a user through their learning roadmap.
 *
 * WHAT THIS DOES:
 * 1. On first call, initializes RoadmapTopic rows from the profile's roadmapTopics list
 * 2. Returns the current state of the full roadmap with per-topic progress
 * 3. Returns what the user should do NEXT (next concept, next action)
 * 4. When user completes a concept (quiz/practice), records it and checks topic completion
 * 5. When a topic is complete, unlocks the next one automatically
 * 6. Keeps LearningProfile.currentTopicIndex in sync
 *
 * TOPIC COMPLETION RULES:
 * - A topic is considered complete when progressPercent >= 80
 * - progressPercent = (completedConcepts / totalConcepts) * 100
 * - Each concept needs at least ONE quiz attempt to be counted complete
 *
 * UNLOCK RULES:
 * - Topic 0 is always UNLOCKED after onboarding
 * - Topic N is UNLOCKED when topic N-1 is COMPLETED
 * - User can only be IN_PROGRESS on ONE topic at a time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapTopicRepository    roadmapTopicRepository;
    private final LearningProfileRepository profileRepository;
    private final UserRepository            userRepository;
    private final AiService                 aiService;

    // A topic is complete when this % of its concepts are done
    private static final int COMPLETION_THRESHOLD_PERCENT = 80;

    // ─── Initialize roadmap for a new user ───────────────────────────────
    // Called once after onboarding completes

    @Transactional
    public void initializeRoadmap(UUID userId) {
        if (roadmapTopicRepository.existsByUserId(userId)) {
            log.info("Roadmap already initialized for user: {}", userId);
            return;
        }

        User user = getUser(userId);
        LearningProfile profile = getProfile(userId);
        List<String> topics = profile.getRoadmapTopics();

        if (topics == null || topics.isEmpty()) {
            log.warn("No roadmap topics found for user: {}", userId);
            return;
        }

        log.info("Initializing roadmap for user:{} goal:{} topics:{}",
                userId, profile.getGoal(), topics.size());

        for (int i = 0; i < topics.size(); i++) {
            String topicName = topics.get(i);

            // Generate concept list for this topic using AI
            List<String> concepts = generateConceptsForTopic(
                    topicName, profile.getGoal(), profile.getCurrentDifficulty());

            RoadmapTopic rt = RoadmapTopic.builder()
                    .user(user)
                    .topicName(topicName)
                    .goal(profile.getGoal())
                    .topicOrder(i)
                    // First topic unlocked, rest locked
                    .status(i == 0
                            ? RoadmapTopic.TopicStatus.UNLOCKED
                            : RoadmapTopic.TopicStatus.LOCKED)
                    .concepts(concepts)
                    .completedConcepts(new ArrayList<>())
                    .build();

            roadmapTopicRepository.save(rt);
        }

        log.info("Roadmap initialized — {} topics created for user:{}",
                topics.size(), userId);
    }

    // ─── Get full roadmap state ───────────────────────────────────────────

    public RoadmapStateResponse getRoadmapState(UUID userId) {
        LearningProfile profile = getProfile(userId);

        // Auto-initialize if not done yet
        if (!roadmapTopicRepository.existsByUserId(userId)) {
            initializeRoadmap(userId);
        }

        List<RoadmapTopic> allTopics = roadmapTopicRepository
                .findByUserIdOrderByTopicOrderAsc(userId);

        // Current active topic
        RoadmapTopic currentTopic = allTopics.stream()
                .filter(t -> t.getStatus() == RoadmapTopic.TopicStatus.IN_PROGRESS)
                .findFirst()
                .orElse(allTopics.stream()
                        .filter(t -> t.getStatus() == RoadmapTopic.TopicStatus.UNLOCKED)
                        .findFirst()
                        .orElse(null));

        long completed = allTopics.stream()
                .filter(t -> t.getStatus() == RoadmapTopic.TopicStatus.COMPLETED)
                .count();

        double overallProgress = allTopics.isEmpty() ? 0.0
                : (double) completed / allTopics.size() * 100;

        // Build next action for the user
        NextActionResponse nextAction = buildNextAction(currentTopic, profile);

        return RoadmapStateResponse.builder()
                .goal(profile.getGoal())
                .totalTopics(allTopics.size())
                .completedTopics((int) completed)
                .overallProgressPercent(Math.round(overallProgress * 10.0) / 10.0)
                .currentDifficulty(profile.getCurrentDifficulty())
                .learningStyle(profile.getLearningStyle())
                .currentTopic(currentTopic != null ? mapToTopicDto(currentTopic) : null)
                .nextAction(nextAction)
                .topics(allTopics.stream()
                        .map(this::mapToTopicDto)
                        .collect(Collectors.toList()))
                .build();
    }

    // ─── Get current topic with next concept to study ─────────────────────

    public CurrentTopicResponse getCurrentTopic(UUID userId) {
        if (!roadmapTopicRepository.existsByUserId(userId)) {
            initializeRoadmap(userId);
        }

        LearningProfile profile = getProfile(userId);

        // Find IN_PROGRESS topic first, else UNLOCKED
        RoadmapTopic topic = roadmapTopicRepository
                .findFirstByUserIdAndStatusOrderByTopicOrderAsc(
                        userId, RoadmapTopic.TopicStatus.IN_PROGRESS)
                .orElseGet(() -> roadmapTopicRepository
                        .findFirstByUserIdAndStatusOrderByTopicOrderAsc(
                                userId, RoadmapTopic.TopicStatus.UNLOCKED)
                        .orElseThrow(() ->
                                AppException.notFound("All topics completed! Amazing work.")));

        // Mark as IN_PROGRESS when user first touches it
        if (topic.getStatus() == RoadmapTopic.TopicStatus.UNLOCKED) {
            topic.setStatus(RoadmapTopic.TopicStatus.IN_PROGRESS);
            topic.setStartedAt(Instant.now());
            roadmapTopicRepository.save(topic);
        }

        // Next concept = first concept not yet completed
        List<String> remaining = getRemainingConcepts(topic);
        String nextConcept = remaining.isEmpty() ? null : remaining.get(0);

        // Suggest what to do with this concept
        NextActionResponse action = buildConceptAction(nextConcept, topic, profile);

        return CurrentTopicResponse.builder()
                .topicId(topic.getId())
                .topicName(topic.getTopicName())
                .topicOrder(topic.getTopicOrder())
                .status(topic.getStatus().name())
                .concepts(topic.getConcepts())
                .completedConcepts(topic.getCompletedConcepts())
                .remainingConcepts(remaining)
                .nextConcept(nextConcept)
                .progressPercent(topic.getProgressPercent())
                .quizScore(topic.getQuizScore())
                .practiceScore(topic.getPracticeScore())
                .nextAction(action)
                .build();
    }

    // ─── Mark a concept as complete (called after quiz/practice) ─────────

    @Transactional
    public ConceptCompleteResponse markConceptComplete(UUID userId,
                                                        String conceptName,
                                                        String activityType,
                                                        double score) {

        if (!roadmapTopicRepository.existsByUserId(userId)) {
            initializeRoadmap(userId);
        }

        LearningProfile profile = getProfile(userId);

        // Find the topic this concept belongs to
        RoadmapTopic topic = findTopicForConcept(userId, conceptName);
        if (topic == null) {
            log.warn("No roadmap topic found for concept: {} user: {}",
                    conceptName, userId);
            // Not a fatal error — concept may not be in roadmap
            return ConceptCompleteResponse.builder()
                    .conceptName(conceptName)
                    .recorded(false)
                    .message("Concept not found in your roadmap — still recorded in Learning DNA")
                    .build();
        }

        // Add to completed concepts if not already there
        List<String> completed = new ArrayList<>(
                topic.getCompletedConcepts() != null
                        ? topic.getCompletedConcepts()
                        : List.of());

        boolean alreadyDone = completed.stream()
                .anyMatch(c -> c.equalsIgnoreCase(conceptName));

        if (!alreadyDone) {
            completed.add(conceptName);
            topic.setCompletedConcepts(completed);
        }

        // Update scores
        if ("QUIZ".equals(activityType)) {
            double updated = topic.getQuizzesTaken() == 0
                    ? score
                    : (topic.getQuizScore() * topic.getQuizzesTaken() + score)
                      / (topic.getQuizzesTaken() + 1);
            topic.setQuizScore(Math.round(updated * 100.0) / 100.0);
            topic.setQuizzesTaken(topic.getQuizzesTaken() + 1);
        } else if ("PRACTICE".equals(activityType)) {
            double updated = topic.getPracticeAttempts() == 0
                    ? score
                    : (topic.getPracticeScore() * topic.getPracticeAttempts() + score)
                      / (topic.getPracticeAttempts() + 1);
            topic.setPracticeScore(Math.round(updated * 100.0) / 100.0);
            topic.setPracticeAttempts(topic.getPracticeAttempts() + 1);
        }

        // Recalculate progress
        int totalConcepts = topic.getConcepts() != null
                ? topic.getConcepts().size() : 1;
        int progress = (int) Math.round(
                (double) completed.size() / totalConcepts * 100);
        topic.setProgressPercent(Math.min(100, progress));

        // Mark topic as IN_PROGRESS if not already
        if (topic.getStatus() == RoadmapTopic.TopicStatus.UNLOCKED) {
            topic.setStatus(RoadmapTopic.TopicStatus.IN_PROGRESS);
            topic.setStartedAt(Instant.now());
        }

        boolean topicJustCompleted = false;

        // Check topic completion
        if (topic.getProgressPercent() >= COMPLETION_THRESHOLD_PERCENT
                && topic.getStatus() != RoadmapTopic.TopicStatus.COMPLETED) {

            topic.setStatus(RoadmapTopic.TopicStatus.COMPLETED);
            topic.setCompletedAt(Instant.now());
            topicJustCompleted = true;

            // Unlock next topic
            unlockNextTopic(userId, topic.getTopicOrder());

            // Advance currentTopicIndex in LearningProfile
            profile.setCurrentTopicIndex(topic.getTopicOrder() + 1);
            profileRepository.save(profile);

            log.info("Topic COMPLETED: '{}' by user:{} — unlocking next",
                    topic.getTopicName(), userId);
        }

        roadmapTopicRepository.save(topic);

        // Get next concept
        List<String> remaining = getRemainingConcepts(topic);
        String nextConcept = remaining.isEmpty() ? null : remaining.get(0);

        // If topic just completed, next concept is from new topic
        String nextTopicName = null;
        if (topicJustCompleted) {
            RoadmapTopic nextTopic = roadmapTopicRepository
                    .findFirstByUserIdAndStatusOrderByTopicOrderAsc(
                            userId, RoadmapTopic.TopicStatus.UNLOCKED)
                    .orElse(null);
            if (nextTopic != null) {
                nextTopicName = nextTopic.getTopicName();
                nextConcept = nextTopic.getConcepts() != null
                        && !nextTopic.getConcepts().isEmpty()
                        ? nextTopic.getConcepts().get(0) : null;
            }
        }

        return ConceptCompleteResponse.builder()
                .conceptName(conceptName)
                .recorded(true)
                .topicName(topic.getTopicName())
                .topicProgressPercent(topic.getProgressPercent())
                .topicCompleted(topicJustCompleted)
                .nextTopicUnlocked(topicJustCompleted ? nextTopicName : null)
                .nextConceptToStudy(nextConcept)
                .message(buildCompletionMessage(topicJustCompleted, nextTopicName, nextConcept))
                .build();
    }

    // ─── Get daily study plan ─────────────────────────────────────────────
    // Tells the user exactly what to do today

    public DailyPlanResponse getDailyPlan(UUID userId) {
        if (!roadmapTopicRepository.existsByUserId(userId)) {
            initializeRoadmap(userId);
        }

        LearningProfile profile = getProfile(userId);

        RoadmapTopic currentTopic = roadmapTopicRepository
                .findFirstByUserIdAndStatusOrderByTopicOrderAsc(
                        userId, RoadmapTopic.TopicStatus.IN_PROGRESS)
                .orElseGet(() -> roadmapTopicRepository
                        .findFirstByUserIdAndStatusOrderByTopicOrderAsc(
                                userId, RoadmapTopic.TopicStatus.UNLOCKED)
                        .orElse(null));

        if (currentTopic == null) {
            return DailyPlanResponse.builder()
                    .message("🎉 You've completed your entire roadmap! Consider setting a new goal.")
                    .tasks(List.of())
                    .build();
        }

        List<String> remaining = getRemainingConcepts(currentTopic);
        List<DailyTask> tasks = new ArrayList<>();

        // Pick up to 3 concepts for today
        List<String> todayConcepts = remaining.stream()
                .limit(3)
                .collect(Collectors.toList());

        for (String concept : todayConcepts) {
            // 1. Learn task (explain)
            tasks.add(DailyTask.builder()
                    .order(tasks.size() + 1)
                    .taskType("LEARN")
                    .conceptName(concept)
                    .topicName(currentTopic.getTopicName())
                    .description("Read the explanation for: " + concept)
                    .apiCall("POST /api/learn/explain")
                    .apiBody("""
                            { "conceptName": "%s", "question": "Explain %s in detail" }
                            """.formatted(concept, concept).trim())
                    .estimatedMinutes(10)
                    .build());

            // 2. Quiz task
            tasks.add(DailyTask.builder()
                    .order(tasks.size() + 1)
                    .taskType("QUIZ")
                    .conceptName(concept)
                    .topicName(currentTopic.getTopicName())
                    .description("Take a quiz on: " + concept)
                    .apiCall("POST /api/quiz/start")
                    .apiBody("""
                            { "conceptName": "%s" }
                            """.formatted(concept).trim())
                    .estimatedMinutes(15)
                    .build());

            // 3. Practice task
            tasks.add(DailyTask.builder()
                    .order(tasks.size() + 1)
                    .taskType("PRACTICE")
                    .conceptName(concept)
                    .topicName(currentTopic.getTopicName())
                    .description("Solve a practice problem on: " + concept)
                    .apiCall("POST /api/practice/generate")
                    .apiBody("""
                            { "conceptName": "%s", "topicGoal": "%s" }
                            """.formatted(concept, profile.getGoal()).trim())
                    .estimatedMinutes(20)
                    .build());
        }

        // Add revision task if there are due cards
        tasks.add(DailyTask.builder()
                .order(tasks.size() + 1)
                .taskType("REVISION")
                .conceptName("Spaced Repetition Review")
                .topicName(currentTopic.getTopicName())
                .description("Review due revision cards")
                .apiCall("GET /api/revision/due")
                .apiBody(null)
                .estimatedMinutes(10)
                .build());

        int totalMinutes = tasks.stream()
                .mapToInt(DailyTask::getEstimatedMinutes).sum();

        return DailyPlanResponse.builder()
                .currentTopicName(currentTopic.getTopicName())
                .topicProgressPercent(currentTopic.getProgressPercent())
                .conceptsToStudyToday(todayConcepts)
                .totalEstimatedMinutes(totalMinutes)
                .tasks(tasks)
                .message("Here's your study plan for today! Focus on: " +
                         currentTopic.getTopicName())
                .build();
    }

    // ─── AI: generate concepts for a topic ───────────────────────────────

    private List<String> generateConceptsForTopic(String topicName,
                                                    String goal,
                                                    String difficulty) {
        try {
            String systemPrompt = """
                    You are a curriculum designer.
                    List the key concepts a student must learn for this topic.

                    Rules:
                    - Return 4 to 6 concept names
                    - Each concept is 2-5 words, specific and learnable
                    - Order from basic to advanced
                    - Works for ANY domain: DSA, Finance, Music, Art, etc.

                    Respond ONLY with valid JSON:
                    { "concepts": ["Concept 1", "Concept 2", "Concept 3"] }
                    """;

            String userMessage = "Goal: %s. Topic: %s. Difficulty: %s."
                    .formatted(goal, topicName, difficulty);

            String raw = aiService.call(systemPrompt, userMessage);
            String cleaned = raw.replaceAll("(?s)```json\\s*", "")
                                .replaceAll("(?s)```\\s*", "").trim();

            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(cleaned);

            List<String> concepts = new ArrayList<>();
            root.path("concepts").forEach(c -> concepts.add(c.asText()));

            log.info("Generated {} concepts for topic: {}", concepts.size(), topicName);
            return concepts;

        } catch (Exception e) {
            log.warn("Concept generation failed for topic {}: {}", topicName, e.getMessage());
            // Return a sensible default so roadmap doesn't break
            return List.of(
                    topicName + " Fundamentals",
                    topicName + " Core Concepts",
                    topicName + " Applications",
                    topicName + " Advanced Topics"
            );
        }
    }

    // ─── Unlock the next topic ────────────────────────────────────────────

    @Transactional
    protected void unlockNextTopic(UUID userId, int completedOrder) {
        roadmapTopicRepository
                .findByUserIdAndTopicOrder(userId, completedOrder + 1)
                .ifPresent(next -> {
                    if (next.getStatus() == RoadmapTopic.TopicStatus.LOCKED) {
                        next.setStatus(RoadmapTopic.TopicStatus.UNLOCKED);
                        roadmapTopicRepository.save(next);
                        log.info("Topic UNLOCKED: '{}' for user:{}",
                                next.getTopicName(), userId);
                    }
                });
    }

    // ─── Find which topic a concept belongs to ────────────────────────────

    private RoadmapTopic findTopicForConcept(UUID userId, String conceptName) {
        List<RoadmapTopic> allTopics = roadmapTopicRepository
                .findByUserIdOrderByTopicOrderAsc(userId);

        // First try: find topic whose concepts list contains this concept
        for (RoadmapTopic topic : allTopics) {
            if (topic.getConcepts() != null) {
                boolean found = topic.getConcepts().stream()
                        .anyMatch(c -> c.equalsIgnoreCase(conceptName));
                if (found) return topic;
            }
        }

        // Second try: find current IN_PROGRESS topic (concept may be user-typed)
        return allTopics.stream()
                .filter(t -> t.getStatus() == RoadmapTopic.TopicStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);
    }

    // ─── Get concepts not yet completed ──────────────────────────────────

    private List<String> getRemainingConcepts(RoadmapTopic topic) {
        if (topic.getConcepts() == null) return List.of();
        List<String> done = topic.getCompletedConcepts() != null
                ? topic.getCompletedConcepts() : List.of();
        return topic.getConcepts().stream()
                .filter(c -> done.stream().noneMatch(d -> d.equalsIgnoreCase(c)))
                .collect(Collectors.toList());
    }

    // ─── Build next action ────────────────────────────────────────────────

    private NextActionResponse buildNextAction(RoadmapTopic topic,
                                                LearningProfile profile) {
        if (topic == null) {
            return NextActionResponse.builder()
                    .actionType("COMPLETE")
                    .message("You've completed all topics! 🎉")
                    .build();
        }

        List<String> remaining = getRemainingConcepts(topic);
        if (remaining.isEmpty()) {
            return NextActionResponse.builder()
                    .actionType("TOPIC_COMPLETE")
                    .topicName(topic.getTopicName())
                    .message("Topic complete! Move to next topic.")
                    .apiCall("GET /api/roadmap/current-topic")
                    .build();
        }

        String nextConcept = remaining.get(0);
        return NextActionResponse.builder()
                .actionType("LEARN")
                .topicName(topic.getTopicName())
                .conceptName(nextConcept)
                .message("Study next: " + nextConcept + " in " + topic.getTopicName())
                .apiCall("POST /api/learn/explain")
                .apiBody("{\"conceptName\": \"" + nextConcept + "\", " +
                         "\"question\": \"Explain " + nextConcept + "\"}")
                .build();
    }

    private NextActionResponse buildConceptAction(String concept,
                                                   RoadmapTopic topic,
                                                   LearningProfile profile) {
        if (concept == null) {
            return NextActionResponse.builder()
                    .actionType("TOPIC_COMPLETE")
                    .topicName(topic.getTopicName())
                    .message("All concepts done in this topic!")
                    .apiCall("GET /api/roadmap/current-topic")
                    .build();
        }

        return NextActionResponse.builder()
                .actionType("LEARN")
                .topicName(topic.getTopicName())
                .conceptName(concept)
                .message("Start with: " + concept)
                .apiCall("POST /api/learn/explain")
                .apiBody("{\"conceptName\": \"" + concept + "\", " +
                         "\"question\": \"Explain " + concept + "\"}")
                .build();
    }

    private String buildCompletionMessage(boolean topicDone,
                                           String nextTopic,
                                           String nextConcept) {
        if (topicDone && nextTopic != null) {
            return "🎉 Topic completed! Next topic unlocked: " + nextTopic +
                   ". Start with: " + (nextConcept != null ? nextConcept : "first concept");
        }
        if (topicDone) {
            return "🏆 Roadmap complete! All topics finished!";
        }
        return "✅ Concept recorded! Keep going — next: " +
               (nextConcept != null ? nextConcept : "continue topic");
    }

    // ─── Mappers ──────────────────────────────────────────────────────────

    private RoadmapTopicDto mapToTopicDto(RoadmapTopic t) {
        return RoadmapTopicDto.builder()
                .id(t.getId())
                .topicName(t.getTopicName())
                .topicOrder(t.getTopicOrder())
                .status(t.getStatus().name())
                .concepts(t.getConcepts())
                .completedConcepts(t.getCompletedConcepts())
                .progressPercent(t.getProgressPercent())
                .quizScore(t.getQuizScore())
                .practiceScore(t.getPracticeScore())
                .quizzesTaken(t.getQuizzesTaken())
                .startedAt(t.getStartedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private LearningProfile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound(
                        "Complete onboarding first"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    // ─── Response DTOs (inner) ────────────────────────────────────────────

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class RoadmapStateResponse {
        private String goal;
        private int totalTopics;
        private int completedTopics;
        private double overallProgressPercent;
        private String currentDifficulty;
        private String learningStyle;
        private RoadmapTopicDto currentTopic;
        private NextActionResponse nextAction;
        private List<RoadmapTopicDto> topics;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class CurrentTopicResponse {
        private java.util.UUID topicId;
        private String topicName;
        private int topicOrder;
        private String status;
        private List<String> concepts;
        private List<String> completedConcepts;
        private List<String> remainingConcepts;
        private String nextConcept;
        private int progressPercent;
        private double quizScore;
        private double practiceScore;
        private NextActionResponse nextAction;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class ConceptCompleteResponse {
        private String conceptName;
        private boolean recorded;
        private String topicName;
        private int topicProgressPercent;
        private boolean topicCompleted;
        private String nextTopicUnlocked;
        private String nextConceptToStudy;
        private String message;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class DailyPlanResponse {
        private String currentTopicName;
        private int topicProgressPercent;
        private List<String> conceptsToStudyToday;
        private int totalEstimatedMinutes;
        private List<DailyTask> tasks;
        private String message;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class DailyTask {
        private int order;
        private String taskType;   // LEARN / QUIZ / PRACTICE / REVISION
        private String conceptName;
        private String topicName;
        private String description;
        private String apiCall;    // which API to hit
        private String apiBody;    // what body to send
        private int estimatedMinutes;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class NextActionResponse {
        private String actionType;  // LEARN / QUIZ / PRACTICE / TOPIC_COMPLETE / COMPLETE
        private String topicName;
        private String conceptName;
        private String message;
        private String apiCall;
        private String apiBody;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class RoadmapTopicDto {
        private java.util.UUID id;
        private String topicName;
        private int topicOrder;
        private String status;       // LOCKED / UNLOCKED / IN_PROGRESS / COMPLETED
        private List<String> concepts;
        private List<String> completedConcepts;
        private int progressPercent;
        private double quizScore;
        private double practiceScore;
        private int quizzesTaken;
        private Instant startedAt;
        private Instant completedAt;
    }
}