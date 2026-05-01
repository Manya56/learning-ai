package com.learningai.backend.service;

import com.learningai.backend.entity.User;
import com.learningai.backend.entity.XpTransaction;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.UserRepository;
import com.learningai.backend.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.UUID;

/**
 * XpService — awards XP, persists to DB, updates Redis leaderboard.
 *
 * Redis structure:
 *   Sorted Set key: leaderboard:weekly:2026-W18
 *   Member: userId (as string)
 *   Score: total XP that week (double)
 *
 *   All-time sorted set: leaderboard:alltime
 *
 * TTL on weekly key: 14 days (auto-expires old leaderboards)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XpService {

    private final XpTransactionRepository   xpRepository;
    private final UserRepository            userRepository;
    private final LearningProfileRepository profileRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // XP values
    private static final int XP_CORRECT_WITH_HINT    = 10;
    private static final int XP_CORRECT_NO_HINT      = 15;
    private static final int XP_CONCEPT_MASTERED     = 50;
    private static final int XP_TOPIC_COMPLETED      = 200;
    private static final int XP_REVISION_COMPLETED   = 8;

    // ─── Award XP for a quiz answer ───────────────────────────────────────

    @Transactional
    public int awardAnswerXp(UUID userId, boolean correct, boolean hintUsed) {
        if (!correct) return 0;

        int base = hintUsed ? XP_CORRECT_WITH_HINT : XP_CORRECT_NO_HINT;
        int xp   = applyStreakMultiplier(userId, base);

        persist(userId, xp,
                hintUsed ? "CORRECT_ANSWER" : "CORRECT_NO_HINT",
                "quiz answer");
        addToLeaderboard(userId, xp);

        log.debug("XP awarded: {} to user:{} (correct={} hint={})",
                xp, userId, correct, hintUsed);
        return xp;
    }

    // ─── Award XP for concept mastered ───────────────────────────────────

    @Transactional
    public int awardConceptMastered(UUID userId, String conceptName) {
        int xp = applyStreakMultiplier(userId, XP_CONCEPT_MASTERED);
        persist(userId, xp, "CONCEPT_MASTERED", conceptName);
        addToLeaderboard(userId, xp);
        log.info("Concept mastered XP: {} to user:{} concept:{}",
                xp, userId, conceptName);
        return xp;
    }

    // ─── Award XP for topic completed ─────────────────────────────────────

    @Transactional
    public int awardTopicCompleted(UUID userId, String topicName) {
        int xp = applyStreakMultiplier(userId, XP_TOPIC_COMPLETED);
        persist(userId, xp, "TOPIC_COMPLETED", topicName);
        addToLeaderboard(userId, xp);
        log.info("Topic completed XP: {} to user:{} topic:{}",
                xp, userId, topicName);
        return xp;
    }

    // ─── Award XP for revision ────────────────────────────────────────────

    @Transactional
    public int awardRevisionXp(UUID userId, String conceptName, int quality) {
        if (quality < 3) return 0; // failed revision — no XP
        int xp = applyStreakMultiplier(userId, XP_REVISION_COMPLETED);
        persist(userId, xp, "REVISION", conceptName);
        addToLeaderboard(userId, xp);
        return xp;
    }

    // ─── Get user's XP this week ──────────────────────────────────────────

    public int getWeeklyXp(UUID userId) {
        String week = getCurrentWeek();
        // Try Redis first (faster)
        try {
            Double score = redisTemplate.opsForZSet()
                    .score(weeklyKey(week), userId.toString());
            if (score != null) return score.intValue();
        } catch (Exception e) {
            log.warn("Redis unavailable for XP read: {}", e.getMessage());
        }
        // Fallback to DB
        return xpRepository.sumXpByUserAndWeek(userId, week);
    }

    // ─── Get user's all-time XP ───────────────────────────────────────────

    public int getAllTimeXp(UUID userId) {
        return xpRepository.sumXpByUser(userId);
    }

    // ─── Get user's weekly rank ───────────────────────────────────────────

    public long getWeeklyRank(UUID userId) {
        try {
            Long rank = redisTemplate.opsForZSet()
                    .reverseRank(weeklyKey(getCurrentWeek()), userId.toString());
            return rank != null ? rank + 1 : -1; // 1-based rank
        } catch (Exception e) {
            log.warn("Redis unavailable for rank: {}", e.getMessage());
            return -1;
        }
    }

    // ─── Streak multiplier ────────────────────────────────────────────────
    // Day 7 of streak = 2x XP

    private int applyStreakMultiplier(UUID userId, int base) {
        try {
            int streak = profileRepository.findByUserId(userId)
                    .map(p -> p.getCurrentDayStreak())
                    .orElse(0);
            if (streak > 0 && streak % 7 == 0) {
                log.info("🔥 Streak bonus 2x applied for user:{} streak:{}",
                        userId, streak);
                return base * 2;
            }
        } catch (Exception e) {
            log.warn("Could not check streak for multiplier: {}", e.getMessage());
        }
        return base;
    }

    // ─── Persist XP transaction ───────────────────────────────────────────

    private void persist(UUID userId, int xp, String reason, String context) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            XpTransaction tx = XpTransaction.builder()
                    .user(user)
                    .xpEarned(xp)
                    .reason(reason)
                    .context(context)
                    .weekNumber(getCurrentWeek())
                    .build();
            xpRepository.save(tx);
        } catch (Exception e) {
            log.error("Failed to persist XP transaction: {}", e.getMessage());
        }
    }

    // ─── Add score to Redis sorted set ───────────────────────────────────

    private void addToLeaderboard(UUID userId, int xp) {
        try {
            String weekKey    = weeklyKey(getCurrentWeek());
            String allTimeKey = "leaderboard:alltime";
            String member     = userId.toString();

            // Increment existing score (ZINCRBY)
            redisTemplate.opsForZSet().incrementScore(weekKey, member, xp);
            redisTemplate.opsForZSet().incrementScore(allTimeKey, member, xp);

            // Set TTL on weekly key: 14 days so old weeks auto-expire
            redisTemplate.expire(weekKey, java.time.Duration.ofDays(14));

        } catch (Exception e) {
            log.warn("Redis leaderboard update failed (non-fatal): {}", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    public String getCurrentWeek() {
        LocalDate now   = LocalDate.now();
        int weekNum     = now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        int year        = now.getYear();
        return String.format("%d-W%02d", year, weekNum);
    }

    private String weeklyKey(String week) {
        return "leaderboard:weekly:" + week;
    }
}