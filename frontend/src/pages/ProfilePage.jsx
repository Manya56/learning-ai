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
    <div className="space-y-4">
      <Card>
        <h3 className="mb-2 text-xl font-semibold">{profile?.fullName || "Profile"}</h3>
        <p className="text-sm text-[var(--text-muted)]">{profile?.email}</p>
        <div className="mt-3 flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-[var(--surface-2)] px-2 py-1">Goal: {profile?.goal || "-"}</span>
          <span className="rounded-full bg-[var(--accent-light)] px-2 py-1">Difficulty: {profile?.difficultyLevel || "-"}</span>
          <span className="rounded-full bg-[var(--surface-2)] px-2 py-1">Style: {profile?.learningStyle || "-"}</span>
        </div>
      </Card>
      <Card>
        <p className="mb-2 font-semibold">Learning DNA</p>
        <div className="grid gap-2 text-sm md:grid-cols-2">
          <div className="rounded-lg bg-[var(--surface-2)] p-2">Questions attempted: {profile?.totalQuestionsAttempted ?? 0}</div>
          <div className="rounded-lg bg-[var(--surface-2)] p-2">Accuracy: {profile?.overallAccuracy ?? 0}%</div>
          <div className="rounded-lg bg-[var(--surface-2)] p-2">Avg time: {profile?.averageTimePerQuestionMs ?? 0}ms</div>
          <div className="rounded-lg bg-[var(--surface-2)] p-2">Hint usage: {profile?.hintUsageRate ?? 0}%</div>
        </div>
      </Card>
      <Card>
        <p className="mb-2 font-semibold">Roadmap Progress</p>
        <div className="space-y-1">
          {roadmap.map((topic) => (
            <div key={topic.topicOrder} className="rounded-lg bg-[var(--surface-2)] p-2 text-sm">
              {topic.topicOrder + 1}. {topic.topicName} • {topic.progressPercent ?? 0}%
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
