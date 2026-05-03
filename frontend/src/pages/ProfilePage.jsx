import { useEffect } from "react";
import { getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import { useProfileStore } from "../store/profileStore";
import { useState } from "react";

export default function ProfilePage() {
  const profile = useProfileStore((s) => s.profile);
  const loadProfile = useProfileStore((s) => s.loadProfile);
  const [roadmap, setRoadmap] = useState([]);
  useEffect(() => {
    if (!profile) loadProfile();
    getRoadmapApi().then((d) => setRoadmap(d?.topics || [])).catch(() => setRoadmap([]));
  }, [profile, loadProfile]);
  return (
    <div className="space-y-6">
      <div className="rounded-[2rem] bg-gradient-to-r from-sky-600 via-indigo-700 to-violet-700 p-6 text-white shadow-2xl shadow-slate-900/30">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="mb-3 inline-flex rounded-full bg-white/15 px-4 py-2 text-xs uppercase tracking-[0.25em] text-slate-100">Learning Profile</p>
            <h1 className="text-4xl font-semibold tracking-tight">{profile?.fullName || "Your Name"}</h1>
            <p className="mt-2 text-sm text-slate-200">{profile?.email || "No email available"}</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="rounded-3xl bg-white/10 p-4 text-center backdrop-blur-sm">
              <p className="text-xs uppercase text-slate-200">Goal</p>
              <p className="mt-2 text-lg font-semibold">{profile?.goal || "N/A"}</p>
            </div>
            <div className="rounded-3xl bg-white/10 p-4 text-center backdrop-blur-sm">
              <p className="text-xs uppercase text-slate-200">Style</p>
              <p className="mt-2 text-lg font-semibold">{profile?.learningStyle || "N/A"}</p>
            </div>
            <div className="rounded-3xl bg-white/10 p-4 text-center backdrop-blur-sm">
              <p className="text-xs uppercase text-slate-200">Difficulty</p>
              <p className="mt-2 text-lg font-semibold">{profile?.difficultyLevel || "N/A"}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.4fr_1fr]">
        <Card>
          <h3 className="mb-4 text-xl font-semibold">Performance Snapshot</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-3xl bg-[var(--surface-2)] p-5 shadow-sm">
              <p className="text-sm text-[var(--text-muted)]">Questions Attempted</p>
              <p className="mt-3 text-3xl font-bold">{profile?.totalQuestionsAttempted ?? 0}</p>
            </div>
            <div className="rounded-3xl bg-[var(--surface-2)] p-5 shadow-sm">
              <p className="text-sm text-[var(--text-muted)]">Accuracy</p>
              <p className="mt-3 text-3xl font-bold">{profile?.overallAccuracy ?? 0}%</p>
            </div>
            <div className="rounded-3xl bg-[var(--surface-2)] p-5 shadow-sm">
              <p className="text-sm text-[var(--text-muted)]">Avg Time</p>
              <p className="mt-3 text-3xl font-bold">{profile?.averageTimePerQuestionMs ?? 0}ms</p>
            </div>
            <div className="rounded-3xl bg-[var(--surface-2)] p-5 shadow-sm">
              <p className="text-sm text-[var(--text-muted)]">Hint Usage</p>
              <p className="mt-3 text-3xl font-bold">{profile?.hintUsageRate ?? 0}%</p>
            </div>
          </div>
        </Card>

        <Card>
          <h3 className="mb-4 text-xl font-semibold">Roadmap Progress</h3>
          <div className="space-y-4">
            {roadmap.length === 0 ? (
              <p className="text-sm text-[var(--text-muted)]">No roadmap data available yet.</p>
            ) : (
              roadmap.map((topic) => (
                <div key={topic.topicOrder} className="space-y-2">
                  <div className="flex items-center justify-between text-sm font-semibold">
                    <span>{topic.topicName}</span>
                    <span>{topic.progressPercent ?? 0}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-[var(--surface-2)]">
                    <div className="h-2 rounded-full bg-gradient-to-r from-sky-500 to-indigo-500" style={{ width: `${topic.progressPercent ?? 0}%` }} />
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}
