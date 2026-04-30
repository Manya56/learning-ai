import { useEffect } from "react";
import { useProfileStore } from "../../store/profileStore";

export default function TopBar({ title }) {
  const profile = useProfileStore((s) => s.profile);
  const revisionDueCount = useProfileStore((s) => s.revisionDueCount);
  const loadRevisionCount = useProfileStore((s) => s.loadRevisionCount);

  useEffect(() => {
    loadRevisionCount().catch(() => {});
  }, [loadRevisionCount]);

  return (
    <header className="mb-4 flex items-center justify-between rounded-xl border border-[var(--border)] bg-[var(--surface)] p-4">
      <h2 className="text-lg font-semibold">{title}</h2>
      <div className="flex gap-2 text-xs">
        <span className="rounded-full bg-[var(--surface-2)] px-2 py-1">{profile?.goal || "No goal"}</span>
        <span className="rounded-full bg-[var(--accent-light)] px-2 py-1">
          {profile?.difficultyLevel || "MEDIUM"}
        </span>
        <span className="rounded-full bg-[var(--surface-2)] px-2 py-1">🔥 {profile?.streakDays ?? 0} days</span>
        <span className="rounded-full bg-red-500/20 px-2 py-1 text-red-300">Revision: {revisionDueCount}</span>
      </div>
    </header>
  );
}
