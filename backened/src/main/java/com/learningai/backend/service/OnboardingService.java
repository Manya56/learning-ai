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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final LearningProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final AiService aiService;


    // ─── Step 1: Generate knowledge quiz ────────────────────────────────────

    public List<QuizQuestionResponse> getKnowledgeQuiz(
            UUID userId, OnboardingRequest request) {

        User user = getUser(userId);

        if (profileRepository.existsByUser(user)) {
            throw AppException.conflict(
                    "User already completed onboarding");
        }

        log.info("Generating knowledge quiz for user: {} goal: {}",
                userId, request.getGoal());

        return aiService.generateKnowledgeQuiz(
                request.getGoal(),
                request.getPriorKnowledgeLevel()
        );
    }

    // ─── Step 2: Submit quiz answers + complete onboarding ──────────────────

    @Transactional
    public OnboardingResponse completeOnboarding(
            UUID userId,
            OnboardingRequest onboardingRequest,
            KnowledgeQuizAnswerRequest quizAnswers) {

        User user = getUser(userId);

        if (profileRepository.existsByUser(user)) {
            throw AppException.conflict(
                    "User already completed onboarding");
        }

        // Calculate score from quiz answers
        int score = calculateQuizScore(
                quizAnswers.getAnswers(),
                quizAnswers.getCorrectAnswers()
        );

        // Map score to difficulty
        String difficulty = mapScoreToDifficulty(score,
                quizAnswers.getAnswers().size());

        log.info("User {} scored {}/{} → difficulty: {}",
                userId, score,
                quizAnswers.getAnswers().size(), difficulty);

        // Generate roadmap topics via Claude
        List<String> roadmapTopics = aiService.generateRoadmapTopics(
                onboardingRequest.getGoal(),
                difficulty,
                onboardingRequest.getGoalDescription()
        );

        // Create and save the Learning DNA profile
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

    // ─── Get profile ─────────────────────────────────────────────────────────

    public LearningProfileResponse getProfile(UUID userId) {
        User user = getUser(userId);
        LearningProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> AppException.notFound(
                        "Learning profile not found. Complete onboarding first."));
        return mapToResponse(profile);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int calculateQuizScore(List<Integer> answers,
                                    List<Integer> correctAnswers) {
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
        double percentage = (double) score / total * 100;
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
                .currentStreak(p.getCurrentStreak())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}