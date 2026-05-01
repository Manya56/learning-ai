package com.learningai.backend.service;

import com.learningai.backend.dto.response.RevisionCardResponse;
import com.learningai.backend.dto.response.RevisionStatsResponse;
import com.learningai.backend.entity.RevisionCard;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.RevisionCardRepository;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpacedRepetitionService {

    private final RevisionCardRepository    cardRepository;
    private final UserRepository            userRepository;
    private final LearningProfileRepository profileRepository;
    private final DailyStatsService         dailyStatsService;
    private final XpService                 xpService;   // NEW

    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double INITIAL_EASE    = 2.5;
    private static final int    MASTERY_INTERVAL = 21;
    private static final double MASTERY_EASE     = 3.0;

    public RevisionStatsResponse getDueCards(UUID userId) {
        LocalDate today = LocalDate.now();
        List<RevisionCard> dueCards = cardRepository.findDueCards(userId, today);
        long totalCards    = cardRepository.countByUserIdAndStatus(userId, RevisionCard.CardStatus.ACTIVE)
                           + cardRepository.countByUserIdAndStatus(userId, RevisionCard.CardStatus.MASTERED);
        long masteredCards = cardRepository.countByUserIdAndStatus(userId, RevisionCard.CardStatus.MASTERED);
        long overdueCards  = dueCards.stream().filter(c -> c.getNextReviewAt().isBefore(today)).count();
        return RevisionStatsResponse.builder()
                .totalCards(totalCards).dueToday(dueCards.size())
                .masteredCards(masteredCards).overdueCards(overdueCards)
                .dueCards(dueCards.stream().map(c -> mapToResponse(c, today)).collect(Collectors.toList()))
                .weekForecast(buildWeekForecast(userId, today))
                .build();
    }

    @Transactional
    public RevisionCardResponse completeRevision(UUID userId, String conceptName, int quality) {
        RevisionCard card = cardRepository.findByUserIdAndConceptName(userId, conceptName)
                .orElseThrow(() -> AppException.notFound("Revision card not found: " + conceptName));
        applySmTwo(card, quality);
        if (card.getEaseFactor() >= MASTERY_EASE && card.getIntervalDays() >= MASTERY_INTERVAL) {
            card.setStatus(RevisionCard.CardStatus.MASTERED);
            // Award concept mastered XP
            try { xpService.awardConceptMastered(userId, conceptName); }
            catch (Exception e) { log.warn("Concept mastered XP failed: {}", e.getMessage()); }
            log.info("Concept MASTERED: {} by user:{}", conceptName, userId);
        }
        card.setLastReviewedAt(Instant.now());
        card.setTotalReviews(card.getTotalReviews() + 1);
        card.setLastQuality(quality);
        card = cardRepository.save(card);
        // Award revision XP
        try { xpService.awardRevisionXp(userId, conceptName, quality); }
        catch (Exception e) { log.warn("Revision XP failed: {}", e.getMessage()); }
        try { dailyStatsService.recordRevision(userId); }
        catch (Exception e) { log.warn("Daily stats revision failed: {}", e.getMessage()); }
        return mapToResponse(card, LocalDate.now());
    }

    @Transactional
    public RevisionCard createOrUpdateCard(UUID userId, String conceptName,
                                            String topicGoal, boolean wasCorrect) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        Optional<RevisionCard> existing = cardRepository.findByUserIdAndConceptName(userId, conceptName);
        if (existing.isPresent()) {
            RevisionCard card = existing.get();
            int quality = wasCorrect ? 4 : 1;
            applySmTwo(card, quality);
            card.setLastReviewedAt(Instant.now());
            card.setTotalReviews(card.getTotalReviews() + 1);
            return cardRepository.save(card);
        }
        LocalDate firstReview = wasCorrect ? LocalDate.now().plusDays(3) : LocalDate.now().plusDays(1);
        RevisionCard card = RevisionCard.builder()
                .user(user).conceptName(conceptName).topicGoal(topicGoal)
                .easeFactor(INITIAL_EASE).intervalDays(wasCorrect ? 3 : 1)
                .repetitions(0).nextReviewAt(firstReview)
                .stability(wasCorrect ? 3.0 : 1.0)
                .status(RevisionCard.CardStatus.ACTIVE).build();
        return cardRepository.save(card);
    }

    public List<RevisionCardResponse> getAllCards(UUID userId) {
        LocalDate today = LocalDate.now();
        return cardRepository.findByUserIdOrderByNextReviewAtAsc(userId)
                .stream().map(c -> mapToResponse(c, today)).collect(Collectors.toList());
    }

    private void applySmTwo(RevisionCard card, int quality) {
        if (quality >= 3) {
            int newInterval;
            if (card.getRepetitions() == 0)      newInterval = 1;
            else if (card.getRepetitions() == 1)  newInterval = 6;
            else newInterval = (int) Math.round(card.getIntervalDays() * card.getEaseFactor());
            card.setIntervalDays(newInterval);
            card.setRepetitions(card.getRepetitions() + 1);
        } else {
            card.setRepetitions(0);
            card.setIntervalDays(1);
        }
        double newEF = card.getEaseFactor() + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        card.setEaseFactor(Math.max(MIN_EASE_FACTOR, newEF));
        if (quality >= 3) card.setStability(card.getStability() * card.getEaseFactor());
        else              card.setStability(Math.max(1.0, card.getStability() * 0.5));
        card.setNextReviewAt(LocalDate.now().plusDays(card.getIntervalDays()));
    }

    private double calculateRetention(RevisionCard card) {
        if (card.getLastReviewedAt() == null) return 1.0;
        long daysSince = ChronoUnit.DAYS.between(
                card.getLastReviewedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now());
        return Math.max(0.0, Math.min(1.0, Math.exp(-daysSince / card.getStability())));
    }

    private List<RevisionStatsResponse.DayForecast> buildWeekForecast(UUID userId, LocalDate today) {
        List<RevisionCard> allCards = cardRepository.findByUserIdOrderByNextReviewAtAsc(userId);
        List<RevisionStatsResponse.DayForecast> forecast = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.plusDays(i);
            long count = allCards.stream()
                    .filter(c -> c.getNextReviewAt().equals(day) && c.getStatus() == RevisionCard.CardStatus.ACTIVE)
                    .count();
            forecast.add(RevisionStatsResponse.DayForecast.builder()
                    .date(day.toString()).cardsDue((int) count).build());
        }
        return forecast;
    }

    private RevisionCardResponse mapToResponse(RevisionCard card, LocalDate today) {
        double retention = calculateRetention(card);
        long daysSince = card.getLastReviewedAt() != null
                ? ChronoUnit.DAYS.between(
                        card.getLastReviewedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(), today)
                : 0;
        boolean overdue = card.getNextReviewAt().isBefore(today);
        int daysOverdue = overdue ? (int) ChronoUnit.DAYS.between(card.getNextReviewAt(), today) : 0;
        return RevisionCardResponse.builder()
                .cardId(card.getId()).conceptName(card.getConceptName())
                .topicGoal(card.getTopicGoal()).status(card.getStatus().name())
                .easeFactor(Math.round(card.getEaseFactor() * 100.0) / 100.0)
                .intervalDays(card.getIntervalDays()).repetitions(card.getRepetitions())
                .lastQuality(card.getLastQuality()).nextReviewAt(card.getNextReviewAt())
                .retentionPercent(Math.round(retention * 1000.0) / 10.0)
                .daysSinceLastReview((int) daysSince).overdue(overdue).daysOverdue(daysOverdue)
                .build();
    }
}