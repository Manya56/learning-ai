package com.learningai.backend.service;

import com.learningai.backend.dto.response.LearningStyleResponse;
import com.learningai.backend.entity.LearningProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LearningStyleInferenceService {

    private static final int MIN_ATTEMPTS_FOR_INFERENCE = 10;

    // FIX Issue 6: minimum 10 answers (was 5) before any difficulty change
    private static final int  MIN_WINDOW_FOR_DIFFICULTY = 10;

    // Thresholds for difficulty change
    private static final double UPGRADE_THRESHOLD   = 0.80; // 80%+ → go harder
    private static final double DOWNGRADE_THRESHOLD = 0.35; // 35%- → go easier (was 40%)

    // ─── Style inference ─────────────────────────────────────────────────

    public LearningStyleResponse inferStyle(LearningProfile profile) {
        int total = profile.getTotalQuestionsAttempted();

        if (total < MIN_ATTEMPTS_FOR_INFERENCE) {
            return LearningStyleResponse.builder()
                    .currentStyle(profile.getLearningStyle())
                    .previousStyle(profile.getLearningStyle())
                    .styleChanged(false)
                    .reasoning("Not enough data yet (" + total + "/" +
                               MIN_ATTEMPTS_FOR_INFERENCE + " attempts)")
                    .build();
        }

        double practiceScore = computePracticeScore(profile);
        double readingScore  = computeReadingScore(profile);
        double visualScore   = computeVisualScore(profile);

        String inferredStyle;
        if (practiceScore >= readingScore && practiceScore >= visualScore) {
            inferredStyle = "PRACTICE";
        } else if (readingScore >= practiceScore && readingScore >= visualScore) {
            inferredStyle = "READING";
        } else {
            inferredStyle = "VISUAL";
        }

        String previousStyle = profile.getLearningStyle();
        boolean changed      = !inferredStyle.equals(previousStyle);

        return LearningStyleResponse.builder()
                .currentStyle(inferredStyle)
                .previousStyle(previousStyle)
                .styleChanged(changed)
                .practiceScore(round(practiceScore))
                .readingScore(round(readingScore))
                .visualScore(round(visualScore))
                .reasoning(buildReasoning(inferredStyle, practiceScore,
                        readingScore, visualScore, profile))
                .build();
    }

    // ─── Difficulty adjustment ────────────────────────────────────────────
    // FIX Issue 6: only adjusts after enough cross-concept data

    public String adjustDifficulty(LearningProfile profile) {
        String current = profile.getCurrentDifficulty();
        int window     = profile.getRecentWindowSize();
        int correct    = profile.getRecentCorrectCount();

        // FIX: need at least MIN_WINDOW_FOR_DIFFICULTY answers across
        // multiple questions before judging overall difficulty fitness
        if (window < MIN_WINDOW_FOR_DIFFICULTY) {
            log.debug("Difficulty unchanged — window {} < {} required",
                    window, MIN_WINDOW_FOR_DIFFICULTY);
            return current;
        }

        double recentAccuracy = (double) correct / window;

        // FIX: also check total attempts — don't change difficulty
        // if user has < 20 total attempts (too early to judge)
        int total = profile.getTotalQuestionsAttempted();
        if (total < 20) {
            log.debug("Difficulty unchanged — only {} total attempts", total);
            return current;
        }

        if (recentAccuracy >= UPGRADE_THRESHOLD && !"HARD".equals(current)) {
            String newDiff = "EASY".equals(current) ? "MEDIUM" : "HARD";
            log.info("Difficulty UP: {} → {} (accuracy {}% over {} questions)",
                    current, newDiff, Math.round(recentAccuracy * 100), window);
            return newDiff;
        }

        if (recentAccuracy <= DOWNGRADE_THRESHOLD && !"EASY".equals(current)) {
            String newDiff = "HARD".equals(current) ? "MEDIUM" : "EASY";
            log.info("Difficulty DOWN: {} → {} (accuracy {}% over {} questions)",
                    current, newDiff, Math.round(recentAccuracy * 100), window);
            return newDiff;
        }

        return current;
    }

    // ─── Concept score update ─────────────────────────────────────────────
    // This is CORRECT as-is — one bad quiz on ONE concept → that concept weak
    // It does NOT affect overall difficulty (that's adjustDifficulty above)

    public void updateConceptScore(LearningProfile profile,
                                    String concept, boolean isCorrect) {

        java.util.Map<String, Double> weak   = new java.util.HashMap<>(profile.getWeakConcepts());
        java.util.Map<String, Double> strong = new java.util.HashMap<>(profile.getStrongConcepts());

        double current  = weak.getOrDefault(concept, strong.getOrDefault(concept, 0.5));
        double newScore = isCorrect
                ? (current * 0.7) + 0.3   // pull toward 1.0
                : (current * 0.7);         // pull toward 0.0

        newScore = Math.max(0.0, Math.min(1.0, newScore));

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

    // ─── Rolling accuracy (EMA) ───────────────────────────────────────────

    public void updateOverallAccuracy(LearningProfile profile, boolean isCorrect) {
        double current = profile.getOverallAccuracy();
        double updated = (current * 0.9) + (isCorrect ? 0.1 : 0.0);
        profile.setOverallAccuracy(Math.max(0.0, Math.min(1.0, updated)));
        profile.setTotalQuestionsAttempted(profile.getTotalQuestionsAttempted() + 1);
    }

    // ─── Rolling avg time ─────────────────────────────────────────────────

    public void updateAvgTime(LearningProfile profile, long timeTakenMs) {
        long current = profile.getAvgTimePerQuestionMs();
        long updated = current == 0 ? timeTakenMs
                : (long) ((current * 0.8) + (timeTakenMs * 0.2));
        profile.setAvgTimePerQuestionMs(updated);
    }

    // ─── Sliding window ───────────────────────────────────────────────────
    // FIX Issue 6: window tracks CROSS-CONCEPT answers, not just one quiz
    // A single 5-question quiz now only fills half the window — not enough to change difficulty

    public void updateRecentWindow(LearningProfile profile, boolean isCorrect) {
        int window  = profile.getRecentWindowSize();
        int correct = profile.getRecentCorrectCount();

        if (window >= MIN_WINDOW_FOR_DIFFICULTY) {
            // Slide window: remove approximate oldest answer
            window  = MIN_WINDOW_FOR_DIFFICULTY - 1;
            // Assume oldest answer had same accuracy as current rate
            double rate = window > 0 ? (double) correct / window : 0.5;
            correct = (int) Math.round(rate * (MIN_WINDOW_FOR_DIFFICULTY - 1));
        }

        profile.setRecentWindowSize(window + 1);
        profile.setRecentCorrectCount(correct + (isCorrect ? 1 : 0));
    }

    // ─── Score computations ───────────────────────────────────────────────

    private double computePracticeScore(LearningProfile profile) {
        int total = profile.getTotalQuestionsAttempted();
        double codingRatio = total > 0
                ? (double) profile.getCodingAttemptsCount() / total : 0.0;
        double speedScore = profile.getAvgTimePerQuestionMs() > 0
                ? Math.min(1.0, 30000.0 / profile.getAvgTimePerQuestionMs()) : 0.5;
        double lowHintScore = 1.0 - profile.getHintUsageRate();
        return (codingRatio * 0.4) + (speedScore * 0.3) + (lowHintScore * 0.3);
    }

    private double computeReadingScore(LearningProfile profile) {
        int total = profile.getTotalQuestionsAttempted();
        double explanationRatio = total > 0
                ? Math.min(1.0, (double) profile.getExplanationReadCount() / total) : 0.0;
        double slowScore = profile.getAvgTimePerQuestionMs() > 0
                ? Math.min(1.0, profile.getAvgTimePerQuestionMs() / 45000.0) : 0.5;
        double hintScore = 1.0 - Math.abs(profile.getHintUsageRate() - 0.4);
        return (explanationRatio * 0.5) + (slowScore * 0.3) + (hintScore * 0.2);
    }

    private double computeVisualScore(LearningProfile profile) {
        int total = profile.getTotalQuestionsAttempted();
        double speedNorm = profile.getAvgTimePerQuestionMs() > 0
                ? Math.min(1.0, profile.getAvgTimePerQuestionMs() / 60000.0) : 0.5;
        double hintMedium = 1.0 - Math.abs(profile.getHintUsageRate() - 0.5);
        double mcqRatio = total > 0
                ? (double) profile.getMcqAttemptsCount() / total : 0.0;
        return (mcqRatio * 0.4) + (speedNorm * 0.3) + (hintMedium * 0.3);
    }

    private String buildReasoning(String style, double p, double r,
                                   double v, LearningProfile profile) {
        return String.format(
                "Inferred %s. Scores — Practice:%.0f%% Reading:%.0f%% Visual:%.0f%%. "
                + "Based on %d attempts, %.0f%% hints, avg %ds/question.",
                style, p * 100, r * 100, v * 100,
                profile.getTotalQuestionsAttempted(),
                profile.getHintUsageRate() * 100,
                profile.getAvgTimePerQuestionMs() / 1000);
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}