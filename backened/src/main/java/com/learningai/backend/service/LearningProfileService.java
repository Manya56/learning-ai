package com.learningai.backend.service;

import com.learningai.backend.dto.request.UpdateProfileRequest;
import com.learningai.backend.dto.response.LearningProfileResponse;
import com.learningai.backend.dto.response.LearningStyleResponse;
import com.learningai.backend.dto.response.ProfileStatsResponse;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.ProfileSnapshot;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.ProfileSnapshotRepository;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProfileService {

    private final LearningProfileRepository profileRepository;
    private final ProfileSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final LearningStyleInferenceService inferenceService;

    // ─── Get full profile ─────────────────────────────────────────────────

    public LearningProfileResponse getProfile(UUID userId) {
        LearningProfile profile = getProfileOrThrow(userId);
        return mapToResponse(profile);
    }

    // ─── Get stats ────────────────────────────────────────────────────────

    public ProfileStatsResponse getStats(UUID userId) {
        LearningProfile p = getProfileOrThrow(userId);

        // Top 3 weak concepts (lowest scores first)
        List<Map.Entry<String, Double>> top3Weak = p.getWeakConcepts()
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .collect(Collectors.toList());

        // Top 3 strong concepts (highest scores first)
        List<Map.Entry<String, Double>> top3Strong = p.getStrongConcepts()
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Current topic name
        String currentTopic = "Not started";
        List<String> topics = p.getRoadmapTopics();
        int topicIndex = p.getCurrentTopicIndex();
        if (topics != null && !topics.isEmpty()
                && topicIndex < topics.size()) {
            currentTopic = topics.get(topicIndex);
        }

        // Progress %
        double progress = (topics != null && !topics.isEmpty())
                ? (double) topicIndex / topics.size() * 100
                : 0.0;

        return ProfileStatsResponse.builder()
                .currentDifficulty(p.getCurrentDifficulty())
                .learningStyle(p.getLearningStyle())
                .overallAccuracy(p.getOverallAccuracy())
                .totalQuestionsAttempted(p.getTotalQuestionsAttempted())
                .avgTimePerQuestionMs(p.getAvgTimePerQuestionMs())
                .hintUsageRate(p.getHintUsageRate())
                .currentDayStreak(p.getCurrentDayStreak())
                .currentCorrectStreak(p.getCurrentCorrectStreak())
                .top3WeakConcepts(top3Weak)
                .top3StrongConcepts(top3Strong)
                .currentTopic(currentTopic)
                .currentTopicIndex(topicIndex)
                .totalTopics(topics != null ? topics.size() : 0)
                .roadmapProgressPercent(
                        Math.round(progress * 10.0) / 10.0)
                .build();
    }

    // ─── Update profile manually ──────────────────────────────────────────

    @Transactional
    public LearningProfileResponse updateProfile(UUID userId,
            UpdateProfileRequest request) {

        LearningProfile profile = getProfileOrThrow(userId);

        if (request.getPreferredLanguage() != null) {
            profile.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getGoalDescription() != null) {
            profile.setGoalDescription(request.getGoalDescription());
        }
        if (request.getLearningStyle() != null) {
            profile.setLearningStyle(request.getLearningStyle());
        }

        profileRepository.save(profile);
        log.info("Profile manually updated for user: {}", userId);
        return mapToResponse(profile);
    }

    // ─── Infer learning style ─────────────────────────────────────────────

    @Transactional
    public LearningStyleResponse inferAndUpdateStyle(UUID userId) {
        LearningProfile profile = getProfileOrThrow(userId);

        LearningStyleResponse result =
                inferenceService.inferStyle(profile);

        if (result.isStyleChanged()) {
            String oldStyle = profile.getLearningStyle();
            profile.setLearningStyle(result.getCurrentStyle());
            profileRepository.save(profile);

            // Save snapshot so we can show progress over time
            saveSnapshot(profile, "STYLE_CHANGED_FROM_" + oldStyle);
            log.info("Learning style updated: {} → {} for user: {}",
                    oldStyle, result.getCurrentStyle(), userId);
        }

        return result;
    }

    // ─── Record an attempt (called from quiz service later) ───────────────

    @Transactional
    public void recordAttempt(UUID userId,
                               String concept,
                               boolean isCorrect,
                               long timeTakenMs,
                               boolean hintUsed,
                               boolean isCodingQuestion) {

        LearningProfile profile = getProfileOrThrow(userId);

        // Update all behavioral counters
        inferenceService.updateOverallAccuracy(profile, isCorrect);
        inferenceService.updateAvgTime(profile, timeTakenMs);
        inferenceService.updateRecentWindow(profile, isCorrect);
        inferenceService.updateConceptScore(profile, concept, isCorrect);

        // Update hint rate (rolling average)
        double currentRate = profile.getHintUsageRate();
        profile.setHintUsageRate(
                (currentRate * 0.9) + (hintUsed ? 0.1 : 0.0));

        // Update type counters
        if (isCodingQuestion) {
            profile.setCodingAttemptsCount(
                    profile.getCodingAttemptsCount() + 1);
        } else {
            profile.setMcqAttemptsCount(
                    profile.getMcqAttemptsCount() + 1);
        }

        // Update correct streak
        if (isCorrect) {
            profile.setCurrentCorrectStreak(
                    profile.getCurrentCorrectStreak() + 1);
        } else {
            profile.setCurrentCorrectStreak(0);
        }

        // Check if difficulty should change
        String newDifficulty =
                inferenceService.adjustDifficulty(profile);
        boolean difficultyChanged =
                !newDifficulty.equals(profile.getCurrentDifficulty());

        if (difficultyChanged) {
            profile.setCurrentDifficulty(newDifficulty);
            saveSnapshot(profile, "DIFFICULTY_CHANGED");
        }

        // Re-infer style every 10 attempts
        int total = profile.getTotalQuestionsAttempted();
        if (total % 10 == 0 && total > 0) {
            LearningStyleResponse styleResult =
                    inferenceService.inferStyle(profile);
            if (styleResult.isStyleChanged()) {
                profile.setLearningStyle(styleResult.getCurrentStyle());
                saveSnapshot(profile, "STYLE_INFERRED");
            }
        }

        profile.setLastActiveAt(java.time.Instant.now());
        profileRepository.save(profile);
    }

    // ─── Record explanation read ──────────────────────────────────────────

    @Transactional
    public void recordExplanationRead(UUID userId) {
        LearningProfile profile = getProfileOrThrow(userId);
        profile.setExplanationReadCount(
                profile.getExplanationReadCount() + 1);
        profileRepository.save(profile);
    }

    // ─── Get snapshot history ─────────────────────────────────────────────

    public List<ProfileSnapshot> getSnapshotHistory(UUID userId) {
        return snapshotRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private LearningProfile getProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound(
                        "Learning profile not found"));
    }

    private void saveSnapshot(LearningProfile profile, String reason) {
        ProfileSnapshot snapshot = ProfileSnapshot.builder()
                .user(profile.getUser())
                .snapshotReason(reason)
                .difficulty(profile.getCurrentDifficulty())
                .learningStyle(profile.getLearningStyle())
                .accuracy(profile.getOverallAccuracy())
                .totalAttempted(profile.getTotalQuestionsAttempted())
                .weakConcepts(profile.getWeakConcepts())
                .strongConcepts(profile.getStrongConcepts())
                .build();
        snapshotRepository.save(snapshot);
    }

    public LearningProfileResponse mapToResponse(LearningProfile p) {
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