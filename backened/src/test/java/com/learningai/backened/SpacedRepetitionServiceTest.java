package com.learningai.backened;

import com.learningai.backend.entity.RevisionCard;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.RevisionCardRepository;
import com.learningai.backend.repository.UserRepository;
import com.learningai.backend.service.DailyStatsService;
import com.learningai.backend.service.SpacedRepetitionService;
import com.learningai.backend.service.XpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpacedRepetitionServiceTest {

    @Mock private RevisionCardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private LearningProfileRepository profileRepository;
    @Mock private DailyStatsService dailyStatsService;
    @Mock private XpService xpService;

    @InjectMocks
    private SpacedRepetitionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CONCEPT = "Binary Search";

    private RevisionCard buildCard(double easeFactor, int intervalDays, int repetitions) {
        RevisionCard card = new RevisionCard();
        card.setId(UUID.randomUUID());
        card.setConceptName(CONCEPT);
        card.setEaseFactor(easeFactor);
        card.setIntervalDays(intervalDays);
        card.setRepetitions(repetitions);
        card.setStability(3.0);
        card.setTotalReviews(5);
        card.setStatus(RevisionCard.CardStatus.ACTIVE);
        card.setNextReviewAt(LocalDate.now());
        card.setLastReviewedAt(Instant.now());
        return card;
    }

    @BeforeEach
    void stubSave() {
        when(cardRepository.save(any(RevisionCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @ParameterizedTest(name = "quality {0} resets interval to 1")
    @ValueSource(ints = {0, 1, 2})
    void applySmTwo_failedQuality_resetsIntervalAndRepetitions(int quality) {
        RevisionCard card = buildCard(2.5, 15, 3);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, quality);

        assertThat(card.getIntervalDays()).isEqualTo(1);
        assertThat(card.getRepetitions()).isEqualTo(0);
    }

    // repetitions == 0 with passing quality → SM-2 hardcodes first interval to 1
    @Test
    void applySmTwo_firstRepetition_setsIntervalToOne() {
        RevisionCard card = buildCard(2.5, 1, 0);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, 3);

        assertThat(card.getIntervalDays()).isEqualTo(1);
    }

    // repetitions == 1 with passing quality → SM-2 hardcodes second interval to 6
    @Test
    void applySmTwo_secondRepetition_setsIntervalToSix() {
        RevisionCard card = buildCard(2.5, 1, 1);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, 3);

        assertThat(card.getIntervalDays()).isEqualTo(6);
    }

    // nextReviewAt must always be today + intervalDays after applySmTwo
    @Test
    void applySmTwo_setsNextReviewAtToTodayPlusInterval() {
        RevisionCard card = buildCard(2.5, 1, 1);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, 3);

        assertThat(card.getNextReviewAt()).isEqualTo(LocalDate.now().plusDays(6));
    }

    // SM-2 formula for q=5: newEF = EF + (0.1 - 0*(0.08 + 0*0.02)) = EF + 0.1
    @Test
    void applySmTwo_quality5_increasesEaseFactor() {
        RevisionCard card = buildCard(2.5, 10, 2);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, 5);

        assertThat(card.getEaseFactor()).isEqualTo(2.6, within(0.001));
    }

    // For q=0: delta = 0.1 - 5*(0.08 + 5*0.02) = -0.8 → 1.3 - 0.8 = 0.5, clamped to MIN_EASE_FACTOR
    @Test
    void applySmTwo_easeFactorNeverDropsBelowMinimum() {
        RevisionCard card = buildCard(1.3, 1, 0);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, 0);

        assertThat(card.getEaseFactor()).isGreaterThanOrEqualTo(1.3);
    }

    // EF: 2.9 + 0.1 (quality 5) = 3.0 >= MASTERY_EASE; interval: round(8 * 2.9) = 23 >= MASTERY_INTERVAL
    @Test
    void completeRevision_setsStatusToMastered_whenBothThresholdsReached() {
        RevisionCard card = buildCard(2.9, 8, 2);
        when(cardRepository.findByUserIdAndConceptName(USER_ID, CONCEPT))
                .thenReturn(Optional.of(card));

        service.completeRevision(USER_ID, CONCEPT, 5);

        assertThat(card.getStatus()).isEqualTo(RevisionCard.CardStatus.MASTERED);
    }
}
