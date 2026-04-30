import { useEffect, useState } from "react";
import { analyticsHeatmapApi, analyticsOverviewApi, analyticsWeakConceptsApi, analyticsWeeklyApi } from "../api/analytics";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { Link } from "react-router-dom";

export default function AnalyticsPage() {
  const [overview, setOverview] = useState(null);
  const [heatmap, setHeatmap] = useState([]);
  const [weak, setWeak] = useState([]);
  const [strong, setStrong] = useState([]);
  const [weekly, setWeekly] = useState([]);

  useEffect(() => {
    analyticsOverviewApi().then(setOverview).catch(() => setOverview({}));
    analyticsHeatmapApi().then((d) => setHeatmap(d?.days || d || [])).catch(() => setHeatmap([]));
    analyticsWeakConceptsApi()
      .then((d) => {
        setWeak(d?.weakConcepts || []);
        setStrong(d?.strongConcepts || []);
      })
      .catch(() => {
        setWeak([]);
        setStrong([]);
      });
    analyticsWeeklyApi().then((d) => setWeekly(d?.weeks || d || [])).catch(() => setWeekly([]));
  }, []);
  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-4">
        {[
          ["currentStreak", "Streak"],
          ["overallAccuracy", "Accuracy"],
          ["totalStudyTimeHours", "Study Hours"],
          ["activeDaysLast30", "Active Days"],
        ].map(([k, label]) => (
          <Card key={k}>
            <p className="text-xs uppercase text-[var(--text-muted)]">{label}</p>
            <p className="text-xl font-semibold">{overview?.[k] ?? "-"}</p>
          </Card>
        ))}
      </div>
      <Card>
        <p className="mb-2 text-sm font-medium">Progress Summary</p>
        <p className="text-sm text-[var(--text-muted)]">
          You are on {overview?.currentDifficulty || "-"} difficulty with {overview?.overallAccuracy ?? "-"}% overall accuracy,
          {` ${overview?.questionsThisWeek ?? 0}`} questions this week, and {overview?.dueRevisions ?? 0} revision cards due.
        </p>
      </Card>
      <Card>
        <p className="mb-2 text-sm font-medium">Activity Heatmap (last 90 days)</p>
        <div className="grid grid-cols-13 gap-1">
          {heatmap.length ? (
            heatmap.map((day, i) => (
              <div
                key={`${day.date || i}`}
                className="h-3 w-3 rounded-sm"
                title={`${day.date}: ${day.questions || 0} questions, ${day.accuracy || 0}%`}
                style={{
                  background:
                    day.intensity === 4
                      ? "#818cf8"
                      : day.intensity === 3
                        ? "#6366f1"
                        : day.intensity === 2
                          ? "#4338ca"
                          : day.intensity === 1
                            ? "#312e81"
                            : "var(--surface-2)",
                }}
              />
            ))
          ) : (
            <p className="text-sm text-[var(--text-muted)]">No heatmap activity yet.</p>
          )}
        </div>
      </Card>
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <p className="mb-3 text-sm font-medium">Needs Work</p>
          <div className="space-y-2">
            {weak.length ? (
              weak.slice(0, 6).map((item) => (
                <div key={item.concept || item.conceptName} className="rounded-lg bg-[var(--surface-2)] p-2">
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span>{item.concept || item.conceptName}</span>
                    <span className="text-[var(--text-muted)]">{item.score ?? 0}%</span>
                  </div>
                  <div className="h-2 rounded bg-[var(--surface)]">
                    <div className="h-2 rounded bg-[var(--warning)]" style={{ width: `${item.score ?? 0}%` }} />
                  </div>
                  <Link className="mt-1 inline-block" to={`/practice?concept=${encodeURIComponent(item.concept || item.conceptName)}&topic=${encodeURIComponent(overview?.goal || "")}`}>
                    <Button variant="ghost" className="mt-1 text-xs">Practice This</Button>
                  </Link>
                </div>
              ))
            ) : (
              <p className="text-sm text-[var(--text-muted)]">No weak concepts identified yet.</p>
            )}
          </div>
        </Card>
        <Card>
          <p className="mb-3 text-sm font-medium">Weekly Quiz Performance</p>
          <div className="space-y-2">
            {weekly.length ? (
              weekly.slice(0, 4).map((week, idx) => (
                <div key={idx}>
                  <div className="mb-1 flex justify-between text-xs">
                    <span>{week.week || week.label || `Week ${idx + 1}`}</span>
                    <span>{week.questions || 0} questions • {week.accuracy || 0}% • {week.activeDays || 0} active days</span>
                  </div>
                  <div className="h-2 rounded bg-[var(--surface-2)]">
                    <div className="h-2 rounded bg-[var(--accent)]" style={{ width: `${week.accuracy || 0}%` }} />
                  </div>
                </div>
              ))
            ) : (
              <p className="text-sm text-[var(--text-muted)]">No weekly data yet.</p>
            )}
          </div>
        </Card>
      </div>
      <Card>
        <p className="mb-3 text-sm font-medium">Strong Concepts</p>
        <div className="flex flex-wrap gap-2">
          {strong.length ? strong.map((item) => (
            <span key={item.concept} className="rounded-full bg-[var(--surface-2)] px-3 py-1 text-xs">
              {item.concept} • {item.score}%
            </span>
          )) : <p className="text-sm text-[var(--text-muted)]">No strong concepts yet.</p>}
        </div>
      </Card>
    </div>
  );
}
