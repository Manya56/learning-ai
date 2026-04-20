package com.learningai.backend.service;

import com.learningai.backend.dto.response.LearningStyleResponse;
import com.learningai.backend.entity.LearningProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LearningStyleInferenceService {

    // Minimum attempts before we trust the inference
    private static final int MIN_ATTEMPTS_FOR_INFERENCE = 10;

    // ─── Main inference method ────────────────────────────────────────────

    public LearningStyleResponse inferStyle(LearningProfile profile) {

        int total = profile.getTotalQuestionsAttempted();

        // Not enough data yet — keep default
        if (total < MIN_ATTEMPTS_FOR_INFERENCE) {
            return LearningStyleResponse.builder()
                    .currentStyle(profile.getLearningStyle())
                    .previousStyle(profile.getLearningStyle())
                    .styleChanged(false)
                    .visualScore(0.0)
                    .readingScore(0.0)
                    .practiceScore(0.0)
                    .reasoning("Not enough data yet. " +
                               "Need at least " + MIN_ATTEMPTS_FOR_INFERENCE +
                               " attempts. Current: " + total)
                    .build();
        }

        // ── Score each style 0.0 to 1.0 ──────────────────────────────────

        double practiceScore = computePracticeScore(profile);
        double readingScore  = computeReadingScore(profile);
        double visualScore   = computeVisualScore(profile);

        // ── Pick the highest score ────────────────────────────────────────
        String inferredStyle;
        if (practiceScore >= readingScore && practiceScore >= visualScore) {
            inferredStyle = "PRACTICE";
        } else if (readingScore >= practiceScore && readingScore >= visualScore) {
            inferredStyle = "READING";
        } else {
            inferredStyle = "VISUAL";
        }

        String previousStyle = profile.getLearningStyle();
        boolean changed = !inferredStyle.equals(previousStyle);

        String reasoning = buildReasoning(
                inferredStyle, practiceScore, readingScore,
                visualScore, profile);

        log.info("Style inferred for user: {} → {} (changed: {})",
                profile.getUser().getId(), inferredStyle, changed);

        return LearningStyleResponse.builder()
                .currentStyle(inferredStyle)
                .previousStyle(previousStyle)
                .styleChanged(changed)
                .practiceScore(round(practiceScore))
                .readingScore(round(readingScore))
                .visualScore(round(visualScore))
                .reasoning(reasoning)
                .build();
    }

    // ─── Practice score ───────────────────────────────────────────────────
    // High coding attempts, low hint usage, fast answers = PRACTICE learner

    private double computePracticeScore(LearningProfile profile) {
        int total = profile.getTotalQuestionsAttempted();

        // Coding ratio — how many coding vs MCQ
        double codingRatio = total > 0
                ? (double) profile.getCodingAttemptsCount() / total
                : 0.0;

        // Speed signal — fast answers suggest learn-by-doing preference
        // Baseline: 30 seconds per question
        double speedScore = profile.getAvgTimePerQuestionMs() > 0
                ? Math.min(1.0, 30000.0 / profile.getAvgTimePerQuestionMs())
                : 0.5;

        // Low hint usage = prefers figuring it out alone
        double lowHintScore = 1.0 - profile.getHintUsageRate();

        // Weighted combination
        return (codingRatio * 0.4) +
               (speedScore  * 0.3) +
               (lowHintScore * 0.3);
    }

    // ─── Reading score ────────────────────────────────────────────────────
    // Reads explanations often, takes time, uses hints for understanding

    private double computeReadingScore(LearningProfile profile) {
        int total = profile.getTotalQuestionsAttempted();

        // Explanation read ratio
        double explanationRatio = total > 0
                ? Math.min(1.0,
                    (double) profile.getExplanationReadCount() / total)
                : 0.0;

        // Slow answers suggest reading/thinking carefully
        // Baseline: 45 seconds = reading learner
        double slowScore = profile.getAvgTimePerQuestionMs() > 0
                ? Math.min(1.0,
                    profile.getAvgTimePerQuestionMs() / 45000.0)
                : 0.5;

        // Medium hint usage — uses hints to understand, not skip
        double hintScore = 1.0 - Math.abs(profile.getHintUsageRate() - 0.4);

        return (explanationRatio * 0.5) +
               (slowScore         * 0.3) +
               (hintScore         * 0.2);
    }

    // ─── Visual score ─────────────────────────────────────────────────────
    // For now inferred as the remainder — will be enhanced later
    // when we add diagram/visual content tracking

    private double computeVisualScore(LearningProfile profile) {
        // Visual learners tend to have medium speed and medium hints
        double speedNorm = profile.getAvgTimePerQuestionMs() > 0
                ? Math.min(1.0,
                    profile.getAvgTimePerQuestionMs() / 60000.0)
                : 0.5;

        double hintMedium = 1.0 -
                Math.abs(profile.getHintUsageRate() - 0.5);

        // MCQ preference over coding
        int total = profile.getTotalQuestionsAttempted();
        double mcqRatio = total > 0
                ? (double) profile.getMcqAttemptsCount() / total
                : 0.0;

        return (mcqRatio    * 0.4) +
               (speedNorm   * 0.3) +
               (hintMedium  * 0.3);
    }

    // ─── Difficulty adjustment ────────────────────────────────────────────
    // Called after every answer submission

    public String adjustDifficulty(LearningProfile profile) {
        String current = profile.getCurrentDifficulty();

        int window  = profile.getRecentWindowSize();
        int correct = profile.getRecentCorrectCount();

        // Need at least 5 answers in window before adjusting
        if (window < 5) return current;

        double recentAccuracy = (double) correct / window;

        String newDifficulty;
        if (recentAccuracy >= 0.80 && !"HARD".equals(current)) {
            // 80%+ correct → increase difficulty
            newDifficulty = "EASY".equals(current) ? "MEDIUM" : "HARD";
            log.info("Difficulty UP: {} → {} (accuracy: {}%)",
                    current, newDifficulty,
                    Math.round(recentAccuracy * 100));
        } else if (recentAccuracy <= 0.40 && !"EASY".equals(current)) {
            // 40% or below → decrease difficulty
            newDifficulty = "HARD".equals(current) ? "MEDIUM" : "EASY";
            log.info("Difficulty DOWN: {} → {} (accuracy: {}%)",
                    current, newDifficulty,
                    Math.round(recentAccuracy * 100));
        } else {
            newDifficulty = current;
        }

        return newDifficulty;
    }

    // ─── Update concept scores ────────────────────────────────────────────
    // Called after each answer — moves concept between weak/strong maps

    public void updateConceptScore(LearningProfile profile,
                                    String concept,
                                    boolean isCorrect) {

        java.util.Map<String, Double> weak   =
                new java.util.HashMap<>(profile.getWeakConcepts());
        java.util.Map<String, Double> strong =
                new java.util.HashMap<>(profile.getStrongConcepts());

        // Current score for this concept (default 0.5)
        double currentScore = weak.getOrDefault(concept,
                strong.getOrDefault(concept, 0.5));

        // Exponential moving average — new score weighted 30%
        double newScore = isCorrect
                ? (currentScore * 0.7) + (1.0 * 0.3)   // pull toward 1.0
                : (currentScore * 0.7) + (0.0 * 0.3);  // pull toward 0.0

        newScore = Math.max(0.0, Math.min(1.0, newScore));

        // Move between maps based on threshold
        if (newScore >= 0.7) {
            strong.put(concept, newScore);
            weak.remove(concept);
        } else {
            weak.put(concept, newScore);
            strong.remove(concept);
        }

        profile.setWeakConcepts(weak);
        profile.setStrongConcepts(strong);
    }

    // ─── Rolling accuracy update ──────────────────────────────────────────
    // Updates overall accuracy using exponential moving average

    public void updateOverallAccuracy(LearningProfile profile,
                                       boolean isCorrect) {
        double current = profile.getOverallAccuracy();
        double updated = (current * 0.9) + (isCorrect ? 0.1 : 0.0);
        profile.setOverallAccuracy(Math.max(0.0, Math.min(1.0, updated)));
        profile.setTotalQuestionsAttempted(
                profile.getTotalQuestionsAttempted() + 1);
    }

    // ─── Rolling avg time update ──────────────────────────────────────────

    public void updateAvgTime(LearningProfile profile, long timeTakenMs) {
        long current = profile.getAvgTimePerQuestionMs();
        long updated = current == 0
                ? timeTakenMs
                : (long) ((current * 0.8) + (timeTakenMs * 0.2));
        profile.setAvgTimePerQuestionMs(updated);
    }

    // ─── Recent window update ─────────────────────────────────────────────
    // Sliding window of last 10 answers for difficulty adjustment

    public void updateRecentWindow(LearningProfile profile,
                                    boolean isCorrect) {
        int window  = profile.getRecentWindowSize();
        int correct = profile.getRecentCorrectCount();

        if (window >= 10) {
            // Estimate oldest answer to remove (assume 50/50 split
            // as approximation — good enough for MVP)
            window  = 9;
            correct = Math.max(0, correct - 1);
        }

        profile.setRecentWindowSize(window + 1);
        profile.setRecentCorrectCount(correct + (isCorrect ? 1 : 0));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String buildReasoning(String style,
                                   double practice,
                                   double reading,
                                   double visual,
                                   LearningProfile profile) {
        return String.format(
                "Inferred %s learner. Scores — Practice: %.0f%%, " +
                "Reading: %.0f%%, Visual: %.0f%%. " +
                "Based on %d attempts, %.0f%% hint usage, " +
                "avg %ds per question.",
                style,
                practice * 100, reading * 100, visual * 100,
                profile.getTotalQuestionsAttempted(),
                profile.getHintUsageRate() * 100,
                profile.getAvgTimePerQuestionMs() / 1000
        );
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}