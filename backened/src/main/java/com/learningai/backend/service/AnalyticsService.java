package com.learningai.backend.service;

import com.learningai.backend.entity.DailyStats;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.ProfileSnapshot;
import com.learningai.backend.repository.DailyStatsRepository;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.ProfileSnapshotRepository;
import com.learningai.backend.repository.RevisionCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DailyStatsRepository      dailyStatsRepository;
    private final LearningProfileRepository profileRepository;
    private final ProfileSnapshotRepository snapshotRepository;
    private final RevisionCardRepository    revisionCardRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─── Overview ─────────────────────────────────────────────────────────

    public Map<String, Object> getOverview(UUID userId) {
        LearningProfile profile = profileRepository
                .findByUserId(userId).orElse(null);

        LocalDate today  = LocalDate.now();
        LocalDate week   = today.minusDays(7);
        LocalDate month  = today.minusDays(30);

        List<DailyStats> last30 = dailyStatsRepository
                .findRecentStats(userId, month);

        // Total questions this week
        List<DailyStats> last7 = last30.stream()
                .filter(d -> !d.getStatDate().isBefore(week))
                .collect(Collectors.toList());

        int questionsThisWeek = last7.stream()
                .mapToInt(DailyStats::getQuestionsAttempted).sum();

        double avgAccuracyThisWeek = last7.stream()
                .filter(d -> d.getQuestionsAttempted() > 0)
                .mapToDouble(DailyStats::getAccuracy)
                .average().orElse(0.0);

        long totalTimeMs = last30.stream()
                .mapToLong(DailyStats::getTimeSpentMs).sum();

        // Active days in last 30
        long activeDays = last30.stream()
                .filter(d -> d.getQuestionsAttempted() > 0)
                .count();

        // Due revisions today
        long dueRevisions = revisionCardRepository
                .countDueCards(userId, today);

        return Map.of(
            "currentStreak",        profile != null
                    ? profile.getCurrentDayStreak() : 0,
            "currentDifficulty",    profile != null
                    ? profile.getCurrentDifficulty() : "EASY",
            "learningStyle",        profile != null
                    ? profile.getLearningStyle() : "PRACTICE",
            "overallAccuracy",      profile != null
                    ? Math.round(profile.getOverallAccuracy() * 1000.0)
                            / 10.0 : 0.0,
            "questionsThisWeek",    questionsThisWeek,
            "avgAccuracyThisWeek",  Math.round(
                    avgAccuracyThisWeek * 1000.0) / 10.0,
            "totalStudyTimeHours",  Math.round(
                    totalTimeMs / 3600000.0 * 10.0) / 10.0,
            "activeDaysLast30",     activeDays,
            "dueRevisions",         dueRevisions,
            "goal",                 profile != null
                    ? profile.getGoal() : "Not set"
        );
    }

    // ─── Activity heatmap (last 90 days) ─────────────────────────────────

    public List<Map<String, Object>> getHeatmap(UUID userId) {
        LocalDate from = LocalDate.now().minusDays(90);

        List<DailyStats> stats = dailyStatsRepository
                .findRecentStats(userId, from);

        return stats.stream()
                .map(d -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date",      d.getStatDate().format(DATE_FMT));
                    entry.put("questions", d.getQuestionsAttempted());
                    entry.put("accuracy",  Math.round(
                            d.getAccuracy() * 1000.0) / 10.0);
                    entry.put("timeMs",    d.getTimeSpentMs());
                    // Intensity 0-4 for heatmap coloring
                    entry.put("intensity", calculateIntensity(
                            d.getQuestionsAttempted()));
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ─── Difficulty history ───────────────────────────────────────────────

    public List<Map<String, Object>> getDifficultyHistory(UUID userId) {
        List<ProfileSnapshot> snapshots = snapshotRepository
                .findByUserIdOrderByCreatedAtAsc(userId);

        return snapshots.stream()
                .filter(s -> s.getSnapshotReason()
                        .contains("DIFFICULTY"))
                .map(s -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date",       s.getCreatedAt().toString());
                    entry.put("difficulty", s.getDifficulty());
                    entry.put("accuracy",   Math.round(
                            s.getAccuracy() * 1000.0) / 10.0);
                    entry.put("reason",     s.getSnapshotReason());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ─── Learning style evolution ─────────────────────────────────────────

    public List<Map<String, Object>> getStyleEvolution(UUID userId) {
        List<ProfileSnapshot> snapshots = snapshotRepository
                .findByUserIdOrderByCreatedAtAsc(userId);

        return snapshots.stream()
                .filter(s -> s.getSnapshotReason().contains("STYLE"))
                .map(s -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date",  s.getCreatedAt().toString());
                    entry.put("style", s.getLearningStyle());
                    entry.put("totalAttempted", s.getTotalAttempted());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ─── Weak concepts with trend ─────────────────────────────────────────

    public Map<String, Object> getWeakConceptAnalysis(UUID userId) {
        LearningProfile profile = profileRepository
                .findByUserId(userId).orElse(null);

        if (profile == null) return Map.of("weakConcepts", List.of());

        // Current weak concepts sorted by score
        List<Map<String, Object>> weakList = profile.getWeakConcepts()
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("concept",    e.getKey());
                    item.put("score",      Math.round(
                            e.getValue() * 100.0) / 1.0);
                    item.put("status",     e.getValue() < 0.3
                            ? "CRITICAL" : e.getValue() < 0.5
                            ? "WEAK" : "IMPROVING");
                    return item;
                })
                .collect(Collectors.toList());

        // Strong concepts
        List<Map<String, Object>> strongList = profile.getStrongConcepts()
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("concept", e.getKey());
                    item.put("score",   Math.round(
                            e.getValue() * 100.0) / 1.0);
                    return item;
                })
                .collect(Collectors.toList());

        return Map.of(
            "weakConcepts",       weakList,
            "strongConcepts",     strongList,
            "totalWeak",          weakList.size(),
            "totalStrong",        strongList.size(),
            "overallAccuracy",    Math.round(
                    profile.getOverallAccuracy() * 1000.0) / 10.0
        );
    }

    // ─── Weekly performance ───────────────────────────────────────────────

    public List<Map<String, Object>> getWeeklyPerformance(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(28); // last 4 weeks

        List<DailyStats> stats = dailyStatsRepository
                .findRecentStats(userId, from);

        // Group by week
        Map<Integer, List<DailyStats>> byWeek = new LinkedHashMap<>();
        for (DailyStats d : stats) {
            long daysAgo = today.toEpochDay() - d.getStatDate().toEpochDay();
            int weekNum  = (int) (daysAgo / 7);
            byWeek.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(d);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int w = 3; w >= 0; w--) {
            List<DailyStats> weekStats = byWeek.getOrDefault(
                    w, List.of());

            int questions = weekStats.stream()
                    .mapToInt(DailyStats::getQuestionsAttempted).sum();
            double accuracy = weekStats.stream()
                    .filter(d -> d.getQuestionsAttempted() > 0)
                    .mapToDouble(DailyStats::getAccuracy)
                    .average().orElse(0.0);

            Map<String, Object> entry = new HashMap<>();
            entry.put("week",      w == 0 ? "This week"
                    : w == 1 ? "Last week" : w + " weeks ago");
            entry.put("questions", questions);
            entry.put("accuracy",  Math.round(accuracy * 1000.0) / 10.0);
            entry.put("activeDays", weekStats.stream()
                    .filter(d -> d.getQuestionsAttempted() > 0)
                    .count());
            result.add(entry);
        }

        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private int calculateIntensity(int questions) {
        if (questions == 0)  return 0;
        if (questions < 5)   return 1;
        if (questions < 10)  return 2;
        if (questions < 20)  return 3;
        return 4;
    }
}