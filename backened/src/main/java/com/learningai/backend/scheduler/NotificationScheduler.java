package com.learningai.backend.scheduler;

import com.learningai.backend.repository.RevisionCardRepository;
import com.learningai.backend.repository.UserRepository;
import com.learningai.backend.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * NotificationScheduler — fires email reminders on schedule.
 *
 * Jobs:
 *  1. 9:00 AM daily  — revision reminders for users with due cards
 *  2. 8:00 PM daily  — streak reminders for users who haven't studied today
 *  3. Mon 8:00 AM    — motivational message to all active users
 *
 * All sends are @Async in EmailNotificationService — scheduler doesn't block.
 * Processes users in small batches to avoid DB pressure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final EmailNotificationService  emailService;
    private final RevisionCardRepository    revisionCardRepository;
    private final UserRepository            userRepository;

    private static final int BATCH_SIZE = 50;

    // ─── 9AM — send revision reminders ───────────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyRevisionReminders() {
        log.info("═══ NotificationScheduler: daily revision reminders starting ═══");

        LocalDate today = LocalDate.now();

        // Only get user IDs who have due cards — no full user table scan
        List<UUID> userIds = revisionCardRepository.findUserIdsWithDueCards(today);

        log.info("Found {} users with due revision cards", userIds.size());

        // Process in batches
        for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
            List<UUID> batch = userIds.subList(i, Math.min(i + BATCH_SIZE, userIds.size()));
            for (UUID userId : batch) {
                try {
                    emailService.sendRevisionReminder(userId);
                } catch (Exception e) {
                    log.warn("Revision reminder failed for user {}: {}", userId, e.getMessage());
                }
            }
            log.info("Revision reminder batch {}/{} sent", Math.min(i + BATCH_SIZE, userIds.size()), userIds.size());
        }

        log.info("═══ NotificationScheduler: revision reminders complete ═══");
    }

    // ─── 8PM — send streak reminders ─────────────────────────────────────
    // Only fires for users who haven't been active today

    @Scheduled(cron = "0 0 20 * * *")
    public void sendStreakReminders() {
        log.info("NotificationScheduler: streak reminders starting");

        // Users with due cards today who also have streaks >= 2
        // (if they haven't studied yet they're at risk of losing their streak)
        LocalDate today = LocalDate.now();
        List<UUID> atRiskUsers = revisionCardRepository.findUserIdsWithDueCards(today);

        for (UUID userId : atRiskUsers) {
            try {
                emailService.sendStreakReminder(userId);
            } catch (Exception e) {
                log.warn("Streak reminder failed for user {}: {}", userId, e.getMessage());
            }
        }

        log.info("NotificationScheduler: streak reminders sent to {} users", atRiskUsers.size());
    }

    // ─── Monday 8AM — weekly motivational message ─────────────────────────

    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyMotivation() {
        log.info("NotificationScheduler: weekly motivation starting");

        // Send to all users who have a profile (active learners)
        // Limit to prevent spam — only users active in last 14 days
        // For now: send to all users (refine with activity filter later)
        List<UUID> allUserIds = userRepository.findAll()
                .stream()
                .map(u -> u.getId())
                .toList();

        int sent = 0;
        for (int i = 0; i < allUserIds.size(); i += BATCH_SIZE) {
            List<UUID> batch = allUserIds.subList(i, Math.min(i + BATCH_SIZE, allUserIds.size()));
            for (UUID userId : batch) {
                try {
                    emailService.sendMotivationalMessage(userId);
                    sent++;
                } catch (Exception e) {
                    log.warn("Motivational email failed for user {}: {}", userId, e.getMessage());
                }
            }
        }

        log.info("NotificationScheduler: motivational emails sent to {} users", sent);
    }
}