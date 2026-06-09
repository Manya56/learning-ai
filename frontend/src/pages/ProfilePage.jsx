import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getRoadmapApi } from "../api/roadmap";
import { getWeeklyLeaderboardApi } from "../api/leaderboard";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import { useProfileStore } from "../store/profileStore";

const getInitials = (name = "") =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() || "")
    .join("") || "?";

export default function ProfilePage() {
  const profile = useProfileStore((s) => s.profile);
  const loadProfile = useProfileStore((s) => s.loadProfile);
  const [roadmap, setRoadmap] = useState(null);
  const [board, setBoard] = useState(null);

  useEffect(() => {
    if (!profile) loadProfile().catch(() => {});
    getRoadmapApi().then(setRoadmap).catch(() => setRoadmap({ topics: [] }));
    getWeeklyLeaderboardApi()
      .then((d) => {
        const entries = (d?.entries || []).map((u) => ({
          rank: u.rank,
          username: u.displayName || "Anonymous",
          xp: u.xp || 0,
          userId: u.userId,
          isYou: u.isCurrentUser || false,
        }));
        setBoard({ entries });
      })
      .catch(() => setBoard({ entries: [] }));
  }, [profile, loadProfile]);

  const avatar = profile?.avatarUrl || profile?.photoUrl || profile?.picture || profile?.image;

  const entries = board?.entries || [];
  const myIndex = entries.findIndex((e) => e.isYou);
  const lbWindow =
    entries.length === 0 ? [] : myIndex >= 0 ? entries.slice(Math.max(0, myIndex - 2), myIndex + 3) : entries.slice(0, 5);

  const topics = roadmap?.topics || [];
  const totalTopics = roadmap?.totalTopics ?? topics.length;
  const completedTopics = roadmap?.completedTopics ?? topics.filter((t) => t.status === "COMPLETED").length;
  const overallProgress = Math.round(roadmap?.overallProgressPercent ?? 0);
  const currentIndex = Math.max(0, topics.findIndex((t) => t.status === "IN_PROGRESS"));
  const rmWindow = topics.slice(Math.max(0, currentIndex - 1), currentIndex + 3);

  return (
    <div className="grid min-h-[calc(100dvh-7rem)] grid-cols-1 gap-5 sm:grid-cols-2 sm:grid-rows-2">
      {/* Identity */}
      <Card className="flex flex-col items-center justify-center text-center">
        <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-[var(--accent-light)] text-3xl font-extrabold text-[var(--accent-hover)]">
          {avatar ? <img src={avatar} alt="" className="h-full w-full object-cover" /> : getInitials(profile?.fullName)}
        </div>
        <h1 className="mt-4 text-2xl font-extrabold tracking-tight">{profile?.fullName || "Your Name"}</h1>
        <p className="text-sm font-medium text-[var(--text-muted)]">{profile?.email || "No email available"}</p>
        <div className="mt-3 flex flex-wrap items-center justify-center gap-x-4 gap-y-1 text-sm font-bold text-[var(--text)]">
          <span className="flex items-center gap-1.5">
            <Icon name="flag" size={16} className="text-[var(--accent)]" /> {profile?.goal || "—"}
          </span>
          <span className="flex items-center gap-1.5">
            <Icon name="psychology" size={16} className="text-[var(--accent)]" /> {profile?.learningStyle || "—"} learner
          </span>
          <span className="flex items-center gap-1.5">
            <Icon name="bolt" size={16} className="text-[var(--accent)]" /> {profile?.currentDifficulty || profile?.difficultyLevel || "—"} level
          </span>
        </div>
      </Card>

      {/* Leaderboard */}
      <Card className="flex flex-col">
        <h3 className="text-lg font-extrabold tracking-tight">Leaderboard</h3>
        <div className="mt-4 flex-1">
          {!board ? (
            <div className="h-32 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
          ) : lbWindow.length ? (
            <div className="space-y-2">
              {lbWindow.map((e) => (
                <div
                  key={e.userId || e.rank}
                  className={`flex items-center justify-between gap-2 rounded-2xl px-3 py-2 ${
                    e.isYou ? "bg-[var(--accent-light)]" : "bg-[var(--surface-2)]"
                  }`}
                >
                  <div className="flex min-w-0 items-center gap-2">
                    <span className={`w-6 shrink-0 text-center text-sm font-extrabold ${e.isYou ? "text-[var(--accent-hover)]" : "text-[var(--text-muted)]"}`}>
                      {e.rank}
                    </span>
                    <span className={`truncate font-bold ${e.isYou ? "text-[var(--accent-hover)]" : "text-[var(--text)]"}`}>
                      {e.isYou ? "You" : e.username}
                    </span>
                  </div>
                  <span className={`shrink-0 text-sm font-bold ${e.isYou ? "text-[var(--accent-hover)]" : "text-[var(--text-muted)]"}`}>
                    {e.xp} XP
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm font-medium text-[var(--text-muted)]">No ranking yet. Earn XP to climb the board!</p>
          )}
        </div>
        <Link to="/leaderboard">
          <Button className="mt-4 w-full gap-2">
            <Icon name="leaderboard" size={18} /> See leaderboard
          </Button>
        </Link>
      </Card>

      {/* Roadmap */}
      <Card className="flex flex-col">
        <h3 className="text-lg font-extrabold tracking-tight">Roadmap</h3>
        <div className="mt-4 flex-1">
          <div className="mb-1.5 flex items-end justify-between">
            <span className="text-xs font-bold text-[var(--text-muted)]">{completedTopics}/{totalTopics} topics</span>
            <span className="text-lg font-extrabold text-[var(--accent)]">{overallProgress}%</span>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-[var(--surface-2)]">
            <div className="h-full rounded-full bg-[var(--accent)] transition-all" style={{ width: `${overallProgress}%` }} />
          </div>
          <div className="mt-3 space-y-1">
            {rmWindow.length ? (
              rmWindow.map((t, idx) => (
                <div
                  key={t.topicOrder ?? idx}
                  className={`flex items-center gap-2 rounded-xl px-2 py-1.5 text-sm ${t.status === "IN_PROGRESS" ? "bg-[var(--accent-light)]" : ""}`}
                >
                  <span
                    className={`h-2.5 w-2.5 shrink-0 rounded-full ${
                      t.status === "COMPLETED" || t.status === "IN_PROGRESS" ? "bg-[var(--accent)]" : "bg-[var(--border)]"
                    }`}
                  />
                  <span className={`truncate font-bold ${t.status === "LOCKED" ? "text-[var(--text-muted)]" : "text-[var(--text)]"}`}>
                    {t.topicName}
                  </span>
                  <span className="ml-auto shrink-0 text-xs font-bold text-[var(--text-muted)]">{t.progressPercent ?? 0}%</span>
                </div>
              ))
            ) : (
              <p className="text-sm font-medium text-[var(--text-muted)]">No roadmap yet.</p>
            )}
          </div>
        </div>
        <div className="mt-4 flex gap-2">
          <Link to="/space" className="flex-1">
            <Button className="w-full gap-1.5">
              <Icon name="play_arrow" size={18} fill={1} /> Continue
            </Button>
          </Link>
          <Link to="/roadmap" className="flex-1">
            <Button variant="secondary" className="w-full gap-1.5">
              <Icon name="map" size={18} /> Roadmap
            </Button>
          </Link>
        </div>
      </Card>

      {/* Analytics */}
      <Card className="flex flex-col">
        <h3 className="text-lg font-extrabold tracking-tight">Analytics</h3>
        <div className="mt-4 flex-1 space-y-2.5 text-sm font-bold">
          <div className="flex items-center justify-between">
            <span className="text-[var(--text-muted)]">Accuracy</span>
            <span className="text-[var(--text)]">{(profile?.overallAccuracy ?? 0).toFixed(0)}%</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--text-muted)]">Questions answered</span>
            <span className="text-[var(--text)]">{profile?.totalQuestionsAttempted ?? 0}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--text-muted)]">Avg time / question</span>
            <span className="text-[var(--text)]">{((profile?.avgTimePerQuestionMs ?? 0) / 1000).toFixed(1)}s</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-[var(--text-muted)]">Hint usage</span>
            <span className="text-[var(--text)]">{profile?.hintUsageRate ?? 0}%</span>
          </div>
        </div>
        <Link to="/analytics">
          <Button className="mt-4 w-full gap-2">
            <Icon name="analytics" size={18} /> See analytics
          </Button>
        </Link>
      </Card>
    </div>
  );
}
