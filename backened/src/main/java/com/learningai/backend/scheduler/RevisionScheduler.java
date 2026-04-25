package com.learningai.backend.scheduler;

import com.learningai.backend.repository.RevisionCardRepository;
import com.learningai.backend.service.SpacedRepetitionService;
import com.learningai.backend.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RevisionScheduler {

    private final RevisionCardRepository    cardRepository;
    private final SpacedRepetitionService   revisionService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─── Daily at 9AM — cache due cards per user ──────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    public void cacheDueRevisions() {
        LocalDate today = LocalDate.now();
        log.info("RevisionScheduler: caching due cards for {}",
                today);

        List<UUID> userIds = cardRepository
                .findUserIdsWithDueCards(today);

        int totalCards = 0;
        for (UUID userId : userIds) {
            long dueCount = cardRepository
                    .countDueCards(userId, today);

            // Cache count in Redis — mobile app reads this for badge
            String key = Constants.CACHE_REVISION + userId;
            redisTemplate.opsForValue().set(
                    key,
                    dueCount,
                    Duration.ofHours(24));

            totalCards += dueCount;
        }

        log.info("RevisionScheduler: {} users, {} total due cards",
                userIds.size(), totalCards);
    }

    // ─── Every hour — check for critically low retention ─────────────────

    @Scheduled(cron = "0 0 * * * *")
    public void checkCriticalRetention() {
        // Cards with retention < 30% get flagged as urgent
        // This runs hourly to catch cards people forgot during the day
        LocalDate today = LocalDate.now();

        List<UUID> userIds = cardRepository
                .findUserIdsWithDueCards(today);

        for (UUID userId : userIds) {
            String urgentKey = Constants.CACHE_REVISION +
                    "urgent:" + userId;
            long overdueCount = cardRepository
                    .findDueCards(userId, today)
                    .stream()
                    .filter(c -> c.getNextReviewAt()
                            .isBefore(today.minusDays(2)))
                    .count();

            if (overdueCount > 0) {
                redisTemplate.opsForValue().set(
                        urgentKey,
                        overdueCount,
                        Duration.ofHours(1));
            }
        }
    }
}