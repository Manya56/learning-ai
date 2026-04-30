import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { analyticsOverviewApi } from "../api/analytics";
import { getCurrentTopicApi, getDailyPlanApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { parseApiBody, pickConceptName, pickTopicName } from "../utils/study";

export default function DashboardPage() {
  const [dailyPlan, setDailyPlan] = useState(null);
  const [overview, setOverview] = useState(null);
  const [currentTopic, setCurrentTopic] = useState(null);

  useEffect(() => {
    getDailyPlanApi().then(setDailyPlan).catch(() => setDailyPlan({ tasks: [] }));
    analyticsOverviewApi().then(setOverview).catch(() => setOverview({}));
    getCurrentTopicApi().then(setCurrentTopic).catch(() => setCurrentTopic({}));
  }, []);

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card>
        <h3 className="mb-2 font-semibold">Today's Plan</h3>
        {!dailyPlan ? (
          <div className="h-20 animate-pulse rounded bg-[var(--surface-2)]" />
        ) : dailyPlan.tasks?.length ? (
          <div className="space-y-2">
            {dailyPlan.tasks.slice(0, 4).map((task, i) => {
              const body = parseApiBody(task.apiBody);
              const conceptName = pickConceptName({ ...body, ...task });
              const topicGoal = pickTopicName({ ...body, ...task, topicName: dailyPlan.currentTopicName });
              const query = `concept=${encodeURIComponent(conceptName)}&topic=${encodeURIComponent(topicGoal)}`;
              const to =
                task.taskType === "LEARN"
                  ? `/learn?${query}`
                  : task.taskType === "QUIZ"
                    ? `/quiz?${query}`
                    : task.taskType === "PRACTICE"
                      ? `/practice?${query}`
                      : "/revision";
              return (
                <div key={`${conceptName || task.description}-${i}`} className="flex items-center justify-between rounded-lg bg-[var(--surface-2)] p-2">
                  <div>
                    <p className="text-sm">{conceptName || "Study task"}</p>
                    <p className="text-xs text-[var(--text-muted)]">{task.taskType} • ~{task.estimatedMinutes || 15} min</p>
                  </div>
                  <Link to={to}>
                    <Button variant="ghost" className="text-xs">Start</Button>
                  </Link>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-sm text-[var(--text-muted)]">No tasks for today. Great work!</p>
        )}
      </Card>
      <Card>
        <h3 className="mb-2 font-semibold">Current Topic Progress</h3>
        {!currentTopic ? (
          <div className="h-20 animate-pulse rounded bg-[var(--surface-2)]" />
        ) : (
          <>
            <p className="text-sm">{currentTopic.topicName || "No active topic yet"}</p>
            <p className="text-xs text-[var(--text-muted)]">{currentTopic.progressPercent || 0}% complete</p>
            <Link to="/roadmap/current">
              <Button className="mt-3">Continue</Button>
            </Link>
          </>
        )}
      </Card>
      <Card>
        <h3 className="mb-2 font-semibold">Quick Stats</h3>
        {!overview ? (
          <div className="h-20 animate-pulse rounded bg-[var(--surface-2)]" />
        ) : (
          <div className="grid grid-cols-2 gap-2 text-sm">
            <div className="rounded-lg bg-[var(--surface-2)] p-2">🔥 {overview.currentStreak ?? "-"} days</div>
            <div className="rounded-lg bg-[var(--surface-2)] p-2">🎯 {overview.overallAccuracy ?? "-"}%</div>
            <div className="rounded-lg bg-[var(--surface-2)] p-2">❓ {overview.questionsThisWeek ?? "-"}</div>
            <div className="rounded-lg bg-[var(--surface-2)] p-2">🔁 {overview.dueRevisions ?? "-"}</div>
          </div>
        )}
      </Card>
    </div>
  );
}
