import { useEffect, useState } from "react";
import { Link, useNavigate, useOutletContext } from "react-router-dom";
import { motion } from "framer-motion";
import { completeRevisionApi, getRevisionAllApi, getRevisionDueApi } from "../api/revision";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import WizardStepHeader from "../components/ui/WizardStepHeader";
import EmptyState from "../components/ui/EmptyState";

const QUALITIES = [
  { quality: 0, icon: "sentiment_very_dissatisfied" },
  { quality: 2, icon: "sentiment_dissatisfied" },
  { quality: 3, icon: "sentiment_neutral" },
  { quality: 4, icon: "sentiment_satisfied" },
  { quality: 5, icon: "sentiment_very_satisfied" },
];

const Tile = ({ icon, value, label, fill = 0 }) => (
  <div className="rounded-2xl bg-[var(--surface-2)] p-4">
    <div className="flex items-center gap-1.5">
      <Icon name={icon} size={18} fill={fill} className="text-[var(--accent)]" />
      <span className="text-2xl font-extrabold text-[var(--text)]">{value ?? 0}</span>
    </div>
    <p className="mt-1 text-[11px] font-bold uppercase tracking-wide text-[var(--text-muted)]">{label}</p>
  </div>
);

export default function RevisionPage() {
  const navigate = useNavigate();
  const { concept, query } = useOutletContext() || {};
  const [stats, setStats] = useState(null);
  const [allCards, setAllCards] = useState([]);
  const [tab, setTab] = useState("due");
  const [completingCards, setCompletingCards] = useState([]);

  useEffect(() => {
    getRevisionDueApi().then(setStats).catch(() => setStats(null));
    getRevisionAllApi()
      .then((d) => setAllCards(Array.isArray(d) ? d : d?.cards || d?.revisionCards || []))
      .catch(() => setAllCards([]));
  }, []);

  const buildQuery = (conceptName, topicGoal) => {
    const params = new URLSearchParams();
    if (conceptName) params.set("concept", conceptName);
    if (topicGoal) params.set("topic", topicGoal);
    return params.toString();
  };

  const handleComplete = async (card, quality) => {
    if (completingCards.includes(card.conceptName)) return;
    setCompletingCards((prev) => [...prev, card.conceptName]);
    try {
      await completeRevisionApi({ conceptName: card.conceptName, quality });
      setStats((s) => ({
        ...s,
        dueCards: (s?.dueCards || []).filter((c) => c.conceptName !== card.conceptName),
        dueToday: Math.max(0, (s?.dueToday || 1) - 1),
      }));
    } finally {
      setCompletingCards((prev) => prev.filter((id) => id !== card.conceptName));
    }
  };

  return (
    <div className="space-y-5">
      <WizardStepHeader concept={concept} showSteps={false} />

      {/* Stats */}
      <Card>
        <h3 className="mb-4 text-lg font-extrabold tracking-tight">Revision</h3>
        {!stats ? (
          <div className="h-20 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Tile icon="assignment" value={stats.dueToday} label="Due today" />
            <Tile icon="workspace_premium" fill={1} value={stats.masteredCards} label="Mastered" />
            <Tile icon="schedule" value={stats.overdueCards} label="Overdue" />
            <Tile icon="style" value={stats.totalCards} label="Total cards" />
          </div>
        )}
      </Card>

      {/* Tabs + content */}
      <Card>
        <div className="mb-5 flex gap-2">
          <Button variant={tab === "due" ? "primary" : "ghost"} className="gap-1.5" onClick={() => setTab("due")}>
            <Icon name="assignment" size={18} /> Due ({stats?.dueToday || 0})
          </Button>
          <Button variant={tab === "all" ? "primary" : "ghost"} className="gap-1.5" onClick={() => setTab("all")}>
            <Icon name="style" size={18} /> All ({stats?.totalCards || 0})
          </Button>
        </div>

        {tab === "due" ? (
          stats?.dueCards?.length ? (
            <div className="space-y-3">
              {stats.dueCards.map((card, idx) => {
                const query = buildQuery(card.conceptName, card.topicGoal);
                const saving = completingCards.includes(card.conceptName);
                return (
                  <motion.div
                    key={`${card.conceptName}-${idx}`}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: Math.min(idx, 8) * 0.04 }}
                    className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-4"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="truncate text-lg font-extrabold text-[var(--text)]">{card.conceptName}</h3>
                        <p className="truncate text-sm font-medium text-[var(--text-muted)]">{card.topicGoal}</p>
                      </div>
                      <div className="shrink-0 text-right">
                        <p className="text-2xl font-extrabold text-[var(--accent)]">{(card.retentionPercent ?? 0).toFixed(0)}%</p>
                        <p className="text-[10px] font-bold uppercase tracking-wide text-[var(--text-muted)]">Retention</p>
                      </div>
                    </div>

                    <div className="mt-3 h-2 overflow-hidden rounded-full bg-[var(--surface-2)]">
                      <motion.div
                        className="h-full rounded-full bg-[var(--accent)]"
                        initial={{ width: 0 }}
                        animate={{ width: `${card.retentionPercent || 0}%` }}
                        transition={{ duration: 0.5 }}
                      />
                    </div>

                    <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs font-bold text-[var(--text-muted)]">
                      <span>Interval: {card.intervalDays || 0}d</span>
                      <span>Reps: {card.repetitions || 0}</span>
                      <span>Last: {card.daysSinceLastReview || 0}d ago</span>
                    </div>

                    {card.overdue && (
                      <div className="mt-3 flex items-center gap-1.5 rounded-xl bg-[var(--error)]/10 px-3 py-2 text-sm font-bold text-[var(--error)]">
                        <Icon name="warning" size={18} /> {card.daysOverdue} days overdue
                      </div>
                    )}

                    <div className="mt-3 grid grid-cols-3 gap-2">
                      <Link to={`/space/learn?${query}`}>
                        <Button className="w-full gap-1.5 px-0">
                          <Icon name="menu_book" size={16} /> Learn
                        </Button>
                      </Link>
                      <Link to={`/space/quiz?${query}`}>
                        <Button variant="secondary" className="w-full gap-1.5 px-0">
                          <Icon name="quiz" size={16} /> Quiz
                        </Button>
                      </Link>
                      <Link to={`/space/practice?${query}`}>
                        <Button variant="secondary" className="w-full gap-1.5 px-0">
                          <Icon name="code" size={16} /> Practice
                        </Button>
                      </Link>
                    </div>

                    <div className="mt-4 border-t-2 border-[var(--border)] pt-3">
                      <p className="mb-2 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">How well did you recall it?</p>
                      {saving ? (
                        <p className="py-1 text-center text-sm font-bold text-[var(--text-muted)]">Saving…</p>
                      ) : (
                        <div className="grid grid-cols-5 gap-2">
                          {QUALITIES.map((q) => (
                            <Button
                              key={q.quality}
                              variant="ghost"
                              className="w-full px-0"
                              onClick={() => handleComplete(card, q.quality)}
                            >
                              <Icon name={q.icon} size={24} className="text-[var(--accent)]" />
                            </Button>
                          ))}
                        </div>
                      )}
                    </div>
                  </motion.div>
                );
              })}
            </div>
          ) : (
            <EmptyState
              icon="celebration"
              iconClassName="text-[var(--accent)]"
              iconSize={44}
              title="No revisions due today!"
              message="You're up to date. Keep moving forward."
              action={
                concept ? (
                  <Link to={`/space/learn${query || ""}`}>
                    <Button className="gap-1.5 active:scale-95">
                      <Icon name="play_arrow" size={18} fill={1} /> Continue {concept.length > 24 ? `${concept.slice(0, 24)}…` : concept}
                    </Button>
                  </Link>
                ) : null
              }
            />
          )
        ) : allCards.length ? (
          <div className="space-y-2">
            {allCards.map((card, idx) => (
              <motion.div
                key={card.conceptName || idx}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: Math.min(idx, 12) * 0.02 }}
                className="flex items-center justify-between gap-3 rounded-2xl bg-[var(--surface-2)] px-4 py-3 text-sm"
              >
                <div className="min-w-0">
                  <p className="truncate font-bold text-[var(--text)]">{card.conceptName}</p>
                  <p className="text-xs font-medium text-[var(--text-muted)]">
                    {card.nextReviewAt ? `Next: ${new Date(card.nextReviewAt).toLocaleDateString()}` : "—"} · {card.repetitions || 0} reps
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <span className="font-extrabold text-[var(--accent)]">{(card.retentionPercent ?? 0).toFixed(0)}%</span>
                  <span
                    className={`rounded-full px-2.5 py-1 text-xs font-bold ${
                      card.overdue ? "bg-[var(--error)]/10 text-[var(--error)]" : "bg-[var(--surface)] text-[var(--text-muted)]"
                    }`}
                  >
                    {card.status || "—"}
                  </span>
                </div>
              </motion.div>
            ))}
          </div>
        ) : (
          <p className="py-8 text-center text-sm font-medium text-[var(--text-muted)]">No revision cards yet.</p>
        )}
      </Card>
    </div>
  );
}
