package com.learningai.backend.service;

import com.learningai.backend.entity.DailyStats;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.User;
import com.learningai.backend.repository.DailyStatsRepository;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStatsService {

    private final DailyStatsRepository      statsRepository;
    private final UserRepository            userRepository;
    private final LearningProfileRepository profileRepository;

    // ─── Record an attempt into today's stats ────────────────────────────

    @Transactional
    public void recordAttempt(UUID userId,
                               String conceptName,
                               boolean correct,
                               long timeTakenMs) {
        LocalDate today = LocalDate.now();
        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) return;

        LearningProfile profile = profileRepository
                .findByUserId(userId).orElse(null);

        DailyStats stats = statsRepository
                .findByUserIdAndStatDate(userId, today)
                .orElseGet(() -> DailyStats.builder()
                        .user(user)
                        .statDate(today)
                        .questionsAttempted(0)
                        .accuracy(0.0)
                        .timeSpentMs(0L)
                        .conceptsStudied(0)
                        .revisionsDone(0)
                        .streakDay(profile != null
                                ? profile.getCurrentDayStreak() : 0)
                        .difficulty(profile != null
                                ? profile.getCurrentDifficulty() : "EASY")
                        .build());

        // Update stats
        int total = stats.getQuestionsAttempted() + 1;
        double newAccuracy = stats.getQuestionsAttempted() == 0
                ? (correct ? 1.0 : 0.0)
                : ((stats.getAccuracy() * stats.getQuestionsAttempted())
                        + (correct ? 1.0 : 0.0)) / total;

        stats.setQuestionsAttempted(total);
        stats.setAccuracy(newAccuracy);
        stats.setTimeSpentMs(stats.getTimeSpentMs() + timeTakenMs);
        stats.setTopConceptStudied(conceptName);

        if (profile != null) {
            stats.setStreakDay(profile.getCurrentDayStreak());
            stats.setDifficulty(profile.getCurrentDifficulty());
        }

        statsRepository.save(stats);
    }

    // ─── Record a revision ────────────────────────────────────────────────

    @Transactional
    public void recordRevision(UUID userId) {
        LocalDate today = LocalDate.now();
        statsRepository.findByUserIdAndStatDate(userId, today)
                .ifPresent(stats -> {
                    stats.setRevisionsDone(
                            stats.getRevisionsDone() + 1);
                    statsRepository.save(stats);
                });
    }
}