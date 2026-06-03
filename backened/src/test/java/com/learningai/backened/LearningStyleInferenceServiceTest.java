package com.learningai.backened;

import com.learningai.backend.dto.response.LearningStyleResponse;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.service.LearningStyleInferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LearningStyleInferenceServiceTest {

    private LearningStyleInferenceService service;

    @BeforeEach
    void setUp() {
        service = new LearningStyleInferenceService();
    }

    private LearningProfile buildProfile() {
        return LearningProfile.builder()
                .currentDifficulty("EASY")
                .learningStyle("READING")
                .totalQuestionsAttempted(25)
                .recentWindowSize(10)
                .recentCorrectCount(8)
                .codingAttemptsCount(0)
                .mcqAttemptsCount(0)
                .explanationReadCount(0)
                .avgTimePerQuestionMs(20000L)
                .hintUsageRate(0.2)
                .overallAccuracy(0.75)
                .weakConcepts(new HashMap<>())
                .strongConcepts(new HashMap<>())
                .build();
    }

    // window=10, correct=8 → accuracy=0.80 >= UPGRADE_THRESHOLD, total=20 >= 20, EASY → MEDIUM
    @Test
    void adjustDifficulty_upgradesEasyToMedium_whenAccuracyIsAtOrAboveThreshold() {
        LearningProfile profile = buildProfile();
        profile.setCurrentDifficulty("EASY");
        profile.setRecentWindowSize(10);
        profile.setRecentCorrectCount(8);
        profile.setTotalQuestionsAttempted(20);

        String result = service.adjustDifficulty(profile);

        assertThat(result).isEqualTo("MEDIUM");
    }

    // total < 20 → guard returns current difficulty regardless of accuracy
    @Test
    void adjustDifficulty_noChange_whenTotalAttemptsBelow20() {
        LearningProfile profile = buildProfile();
        profile.setCurrentDifficulty("EASY");
        profile.setRecentWindowSize(10);
        profile.setRecentCorrectCount(10);
        profile.setTotalQuestionsAttempted(15);

        String result = service.adjustDifficulty(profile);

        assertThat(result).isEqualTo("EASY");
    }

    // concept not in weak or strong → current defaults to 0.5
    // isCorrect=true → newScore = (0.5 * 0.7) + 0.3 = 0.65
    @Test
    void updateConceptScore_correctAnswer_appliesEmaFormula() {
        LearningProfile profile = buildProfile();
        String concept = "recursion";

        service.updateConceptScore(profile, concept, true);

        double expected = (0.5 * 0.7) + 0.3;
        assertThat(profile.getWeakConcepts().get(concept))
                .isEqualTo(expected, within(0.001));
    }

    // codingRatio=0.9, speedScore=1.0 (15s < 30s), lowHintScore=1.0 → practiceScore=0.96 dominates
    @Test
    void inferStyle_returnsPractice_whenCodingRatioIsHigh() {
        LearningProfile profile = buildProfile();
        profile.setTotalQuestionsAttempted(20);
        profile.setCodingAttemptsCount(18);
        profile.setMcqAttemptsCount(0);
        profile.setExplanationReadCount(0);
        profile.setAvgTimePerQuestionMs(15000L);
        profile.setHintUsageRate(0.0);

        LearningStyleResponse response = service.inferStyle(profile);

        assertThat(response.getCurrentStyle()).isEqualTo("PRACTICE");
    }
}
