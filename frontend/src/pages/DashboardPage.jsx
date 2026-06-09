import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { analyticsOverviewApi } from "../api/analytics";
import { getCurrentTopicApi, getDailyPlanApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import StreakCard from "../components/ui/StreakCard";
import Icon from "../components/ui/Icon";
import { useProfileStore } from "../store/profileStore";
import { parseApiBody, pickConceptName, pickTopicName } from "../utils/study";

export default function DashboardPage() {
  const [dailyPlan, setDailyPlan] = useState(null);
  const [overview, setOverview] = useState(null);
  const [currentTopic, setCurrentTopic] = useState(null);
  const profile = useProfileStore((s) => s.profile);
  const loadProfile = useProfileStore((s) => s.loadProfile);

  useEffect(() => {
    if (!profile) loadProfile().catch(() => {});
    getDailyPlanApi().then(setDailyPlan).catch(() => setDailyPlan({ tasks: [] }));
    analyticsOverviewApi().then(setOverview).catch(() => setOverview({}));
    getCurrentTopicApi().then(setCurrentTopic).catch(() => setCurrentTopic({}));
  }, [profile, loadProfile]);

  const streak = overview?.currentStreak ?? profile?.streakDays ?? 0;
  // Ring tracks progress toward the next 7-day milestone (Duolingo-style weekly streak).
  const ringPercent = streak ? (((streak % 7) || 7) / 7) * 100 : 0;
  const tasks = dailyPlan?.tasks?.slice(0, 4) ?? [];

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {/* Today's Plan — a connected lineup, first step highlighted */}
      <Card className="lg:col-span-2">
        <div className="mb-5 flex items-baseline justify-between">
          <h3 className="text-2xl font-extrabold tracking-tight">Today's Plan</h3>
          <span className="text-sm font-bold text-[var(--text-muted)]">{tasks.length} steps</span>
        </div>

        {!dailyPlan ? (
          <div className="h-40 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
        ) : tasks.length ? (
          <div>
            {tasks.map((task, i) => {
              const body = parseApiBody(task.apiBody);
              const conceptName = pickConceptName({ ...body, ...task });
              const topicGoal = pickTopicName({ ...body, ...task, topicName: dailyPlan.currentTopicName });
              const query = `concept=${encodeURIComponent(conceptName)}&topic=${encodeURIComponent(topicGoal)}`;
              const to =
                task.taskType === "LEARN"
                  ? `/space/learn?${query}`
                  : task.taskType === "QUIZ"
                    ? `/space/quiz?${query}`
                    : task.taskType === "PRACTICE"
                      ? `/space/practice?${query}`
                      : "/space/revision";
              const isFirst = i === 0;
              const isLast = i === tasks.length - 1;
              return (
                <div key={`${conceptName || task.description}-${i}`} className="flex gap-4">
                  {/* Lineup rail */}
                  <div className="flex flex-col items-center">
                    <div
                      className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-extrabold ${
                        isFirst
                          ? "bg-[var(--accent)] text-white shadow-[0_3px_0_0_var(--accent-hover)]"
                          : "border-2 border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)]"
                      }`}
                    >
                      {i + 1}
                    </div>
                    {!isLast && <div className="my-1 w-0.5 flex-1 bg-[var(--border)]" />}
                  </div>

                  {/* Step body */}
                  <div className={`mb-3 flex flex-1 items-center justify-between gap-3 rounded-2xl px-4 py-3 ${
                    isFirst ? "bg-[var(--accent-light)] ring-2 ring-[var(--accent)]/30" : "bg-[var(--surface-2)]"
                  }`}>
                    <div className="min-w-0">
                      {isFirst && (
                        <p className="text-[10px] font-extrabold uppercase tracking-widest text-[var(--accent-hover)]">
                          Start here
                        </p>
                      )}
                      <p className="truncate font-bold text-[var(--text)]">{conceptName || "Study task"}</p>
                      <p className="text-xs font-medium text-[var(--text-muted)]">
                        {task.taskType} • ~{task.estimatedMinutes || 15} min
                      </p>
                    </div>
                    <Link to={to} className="shrink-0">
                      <Button variant={isFirst ? "primary" : "ghost"}>Start</Button>
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="flex items-center gap-2 text-sm font-medium text-[var(--text-muted)]">
            <Icon name="celebration" size={18} className="text-[var(--accent)]" /> No tasks for today. Great work!
          </p>
        )}
      </Card>

      {/* Right column: streak (with quick stats merged in) + current topic */}
      <div className="flex flex-col gap-4">
        <StreakCard profile={profile} streak={streak} percent={ringPercent} to="/analytics" actionLabel="See analytics">
          <p className="mb-2 text-xs font-extrabold uppercase tracking-wide text-[var(--text-muted)]">Quick stats</p>
          {!overview ? (
            <div className="h-16 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
          ) : (
            <div className="grid grid-cols-2 gap-2">
              <div className="rounded-2xl bg-[var(--surface-2)] p-3">
                <div className="flex items-center gap-1.5">
                  <Icon name="schedule" size={16} className="text-[var(--accent)]" />
                  <span className="text-base font-extrabold">{overview.totalStudyTimeHours ?? 0}h</span>
                </div>
                <p className="mt-0.5 text-[10px] font-bold uppercase tracking-wide text-[var(--text-muted)]">Study hours</p>
              </div>
              <div className="rounded-2xl bg-[var(--surface-2)] p-3">
                <div className="flex items-center gap-1.5">
                  <Icon name="calendar_month" size={16} className="text-[var(--accent)]" />
                  <span className="text-base font-extrabold">{overview.activeDaysLast30 ?? "-"}</span>
                </div>
                <p className="mt-0.5 text-[10px] font-bold uppercase tracking-wide text-[var(--text-muted)]">Active days (30d)</p>
              </div>
            </div>
          )}
        </StreakCard>

        <Card>
          <h3 className="mb-2 text-lg font-extrabold tracking-tight">Current Topic</h3>
          {!currentTopic ? (
            <div className="h-16 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
          ) : (
            <>
              <p className="font-bold text-[var(--text)]">{currentTopic.topicName || "No active topic yet"}</p>
              <div className="mt-3 h-3 overflow-hidden rounded-full bg-[var(--surface-2)]">
                <div
                  className="h-full rounded-full bg-[var(--accent)] transition-all"
                  style={{ width: `${currentTopic.progressPercent || 0}%` }}
                />
              </div>
              <p className="mt-2 text-xs font-bold text-[var(--text-muted)]">{currentTopic.progressPercent || 0}% complete</p>
              <Link to="/space">
                <Button className="mt-4 w-full">Continue</Button>
              </Link>
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
