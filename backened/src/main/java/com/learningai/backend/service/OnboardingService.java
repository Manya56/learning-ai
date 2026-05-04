package com.learningai.backend.service;

import com.learningai.backend.dto.request.KnowledgeQuizAnswerRequest;
import com.learningai.backend.dto.request.OnboardingRequest;
import com.learningai.backend.dto.response.LearningProfileResponse;
import com.learningai.backend.dto.response.OnboardingResponse;
import com.learningai.backend.dto.response.QuizQuestionResponse;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.UserRepository;
import com.learningai.backend.service.scraper.ContentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final LearningProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ContentPipelineService contentPipelineService;
    private final RoadmapService roadmapService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUIZ_ANSWERS_KEY = "onboarding:quiz:answers:";
    private static final Duration QUIZ_TTL = Duration.ofMinutes(30);

    // ─── Step 1: Generate knowledge quiz ──────────────────────────────────

    public List<QuizQuestionResponse> getKnowledgeQuiz(
            UUID userId, OnboardingRequest request) {

        User user = getUser(userId);

        if (profileRepository.existsByUser(user)) {
            throw AppException.conflict("User already completed onboarding");
        }

        log.info("Generating knowledge quiz for user: {} goal: {}",
                userId, request.getGoal());

        List<QuizQuestionResponse> questions = aiService.generateKnowledgeQuiz(
                request.getGoal(),
                request.getPriorKnowledgeLevel());

        // FIX: store correct answers server-side in Redis
        // Key = onboarding:quiz:answers:{userId}
        // Value = list of correct answer indices
        List<Integer> correctAnswers = questions.stream()
                .map(QuizQuestionResponse::getCorrectAnswerIndex)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(
                QUIZ_ANSWERS_KEY + userId,
                correctAnswers,
                QUIZ_TTL);

        log.info("Quiz generated and correct answers cached server-side for user: {}", userId);

        return questions;
    }

    // ─── Step 2: Submit quiz answers + complete onboarding ────────────────

    @Transactional
    public OnboardingResponse completeOnboarding(UUID userId,
            OnboardingRequest onboardingRequest,
            KnowledgeQuizAnswerRequest quizAnswers) {

        User user = getUser(userId);
        if (profileRepository.existsByUser(user))
            throw AppException.conflict("User already completed onboarding");

        // Server-side answer lookup
        String cacheKey = QUIZ_ANSWERS_KEY + userId;
        List<Integer> serverAnswers = null;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list)
                serverAnswers = list.stream()
                        .map(o -> Integer.parseInt(o.toString()))
                        .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load cached answers for {}: {}", userId, e.getMessage());
        }

        int score = serverAnswers != null && !serverAnswers.isEmpty()
                ? calculateScore(quizAnswers.getAnswers(), serverAnswers)
                : calculateScore(quizAnswers.getAnswers(), quizAnswers.getCorrectAnswers());

        if (serverAnswers != null)
            redisTemplate.delete(cacheKey);

        String difficulty = mapScoreToDifficulty(score, quizAnswers.getAnswers().size());
        log.info("User {} scored {}/{} → difficulty:{}", userId, score,
                quizAnswers.getAnswers().size(), difficulty);

        List<String> roadmapTopics;
        try {
            roadmapTopics = aiService.generateRoadmapTopics(
                    onboardingRequest.getGoal(), difficulty,
                    onboardingRequest.getGoalDescription());
        } catch (Exception e) {
            log.error("Roadmap generation failed: {}", e.getMessage());
            throw AppException.badRequest("Could not generate your roadmap. Please try again.");
        }

        LearningProfile profile = LearningProfile.builder()
                .user(user).goal(onboardingRequest.getGoal())
                .goalDescription(onboardingRequest.getGoalDescription())
                .preferredLanguage(onboardingRequest.getPreferredLanguage())
                .currentDifficulty(difficulty).learningStyle("PRACTICE")
                .roadmapTopics(roadmapTopics).build();

        profile = profileRepository.save(profile);
        log.info("Profile created for user: {}", userId);

        // NEW: initialize roadmap rows with per-topic concepts
        final UUID finalUserId = userId;
        try {
            roadmapService.initializeRoadmap(finalUserId);
            log.info("Roadmap initialized for user: {}", finalUserId);
        } catch (Exception e) {
            // Non-fatal — auto-initializes on first /api/roadmap call
            log.warn("Roadmap init failed (will retry on first use): {}", e.getMessage());
        }

        contentPipelineService.bootstrapTopicContent(onboardingRequest.getGoal());

        return OnboardingResponse.builder()
                .profileId(profile.getId()).goal(profile.getGoal())
                .preferredLanguage(profile.getPreferredLanguage())
                .currentDifficulty(profile.getCurrentDifficulty())
                .learningStyle(profile.getLearningStyle())
                .roadmapTopics(profile.getRoadmapTopics())
                .message("Onboarding complete! Your personalized roadmap is ready.")
                .build();
    }

    // ─── Get profile ──────────────────────────────────────────────────────

    public LearningProfileResponse getProfile(UUID userId) {
        User user = getUser(userId);
        LearningProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> AppException.notFound(
                        "Learning profile not found. Complete onboarding first."));
        return mapToResponse(profile);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private int calculateScore(List<Integer> answers, List<Integer> correct) {
        if (correct == null || correct.isEmpty())
            return 0;
        int score = 0;
        for (int i = 0; i < answers.size() && i < correct.size(); i++)
            if (answers.get(i).equals(correct.get(i)))
                score++;
        return score;
    }

    private String mapScoreToDifficulty(int score, int total) {
        double percentage = total > 0 ? (double) score / total * 100 : 0;
        if (percentage >= 70)
            return "HARD";
        if (percentage >= 40)
            return "MEDIUM";
        return "EASY";
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    private LearningProfileResponse mapToResponse(LearningProfile p) {
        return LearningProfileResponse.builder()
                .profileId(p.getId())
                .userId(p.getUser().getId())
                .fullName(p.getUser().getFullName())
                .email(p.getUser().getEmail())
                .goal(p.getGoal())
                .goalDescription(p.getGoalDescription())
                .preferredLanguage(p.getPreferredLanguage())
                .currentDifficulty(p.getCurrentDifficulty())
                .learningStyle(p.getLearningStyle())
                .weakConcepts(p.getWeakConcepts())
                .strongConcepts(p.getStrongConcepts())
                .roadmapTopics(p.getRoadmapTopics())
                .avgTimePerQuestionMs(p.getAvgTimePerQuestionMs())
                .hintUsageRate(p.getHintUsageRate())
                .overallAccuracy(p.getOverallAccuracy())
                .totalQuestionsAttempted(p.getTotalQuestionsAttempted())
                .currentStreak(p.getCurrentDayStreak())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}