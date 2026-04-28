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

    private final LearningProfileRepository     profileRepository;
    private final UserRepository                userRepository;
    private final AiService                     aiService;
    private final ContentPipelineService        contentPipelineService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String   QUIZ_ANSWERS_KEY = "onboarding:quiz:answers:";
    private static final Duration QUIZ_TTL         = Duration.ofMinutes(30);

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
    public OnboardingResponse completeOnboarding(
            UUID userId,
            OnboardingRequest onboardingRequest,
            KnowledgeQuizAnswerRequest quizAnswers) {

        User user = getUser(userId);

        if (profileRepository.existsByUser(user)) {
            throw AppException.conflict("User already completed onboarding");
        }

        // FIX: load correct answers from server-side Redis cache
        String cacheKey = QUIZ_ANSWERS_KEY + userId;

        @SuppressWarnings("unchecked")
        List<Integer> serverCorrectAnswers = null;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                // Redis may return ArrayList<Integer> or ArrayList<LinkedHashMap>
                if (cached instanceof List<?> list) {
                    serverCorrectAnswers = list.stream()
                            .map(o -> Integer.parseInt(o.toString()))
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Failed to parse cached answers for user {}: {}", userId, e.getMessage());
            }
        }

        // Calculate score
        int score;
        int total = quizAnswers.getAnswers().size();

        if (serverCorrectAnswers != null && !serverCorrectAnswers.isEmpty()) {
            // Use server-side answers — tamper-proof
            score = calculateQuizScore(quizAnswers.getAnswers(), serverCorrectAnswers);
            log.info("Score calculated from server-side answers for user: {}", userId);
            // Clean up Redis
            redisTemplate.delete(cacheKey);
        } else {
            // Redis expired or quiz was re-requested — fall back to client-sent answers
            // This is the fallback path (e.g. Redis down or TTL expired)
            log.warn("Server-side answers not found for user {} — falling back to client answers", userId);
            score = calculateQuizScore(quizAnswers.getAnswers(), quizAnswers.getCorrectAnswers());
        }

        String difficulty = mapScoreToDifficulty(score, total);

        log.info("User {} scored {}/{} → difficulty: {}", userId, score, total, difficulty);

        // Generate roadmap
        List<String> roadmapTopics;
        try {
            roadmapTopics = aiService.generateRoadmapTopics(
                    onboardingRequest.getGoal(),
                    difficulty,
                    onboardingRequest.getGoalDescription());
        } catch (Exception e) {
            log.error("Roadmap generation failed for user {}: {}", userId, e.getMessage());
            throw AppException.badRequest(
                    "Could not generate your learning roadmap right now. Please try again.");
        }

        // Create Learning DNA profile
        LearningProfile profile = LearningProfile.builder()
                .user(user)
                .goal(onboardingRequest.getGoal())
                .goalDescription(onboardingRequest.getGoalDescription())
                .preferredLanguage(onboardingRequest.getPreferredLanguage())
                .currentDifficulty(difficulty)
                .learningStyle("PRACTICE") // default, refined over time
                .roadmapTopics(roadmapTopics)
                .build();

        profile = profileRepository.save(profile);
        log.info("Learning profile created for user: {}", userId);

        // Fire async content bootstrap
        contentPipelineService.bootstrapTopicContent(onboardingRequest.getGoal());
        log.info("Content pipeline triggered for goal: {}", onboardingRequest.getGoal());

        return OnboardingResponse.builder()
                .profileId(profile.getId())
                .goal(profile.getGoal())
                .preferredLanguage(profile.getPreferredLanguage())
                .currentDifficulty(profile.getCurrentDifficulty())
                .learningStyle(profile.getLearningStyle())
                .roadmapTopics(profile.getRoadmapTopics())
                .message("Onboarding complete! Your learning path is ready.")
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

    private int calculateQuizScore(List<Integer> answers, List<Integer> correctAnswers) {
        if (correctAnswers == null || correctAnswers.isEmpty()) return 0;
        int score = 0;
        for (int i = 0; i < answers.size(); i++) {
            if (i < correctAnswers.size() &&
                answers.get(i).equals(correctAnswers.get(i))) {
                score++;
            }
        }
        return score;
    }

    private String mapScoreToDifficulty(int score, int total) {
        double percentage = total > 0 ? (double) score / total * 100 : 0;
        if (percentage >= 70) return "HARD";
        if (percentage >= 40) return "MEDIUM";
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