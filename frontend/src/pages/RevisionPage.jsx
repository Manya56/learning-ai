import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { completeRevisionApi, getRevisionAllApi, getRevisionDueApi } from "../api/revision";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

export default function RevisionPage() {
  const [stats, setStats] = useState(null);
  const [allCards, setAllCards] = useState([]);
  const [tab, setTab] = useState("due");
  useEffect(() => {
    getRevisionDueApi()
      .then((data) => {
        setStats(data);
      })
      .catch(() => setStats(null));
    
    getRevisionAllApi()
      .then((d) => {
        const data = Array.isArray(d) ? d : d?.cards || d?.revisionCards || [];
        setAllCards(data);
      })
      .catch(() => setAllCards([]));
  }, []);
  return (
    <div className="space-y-6">
      {/* Stats Overview */}
      {stats && (
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="grid gap-4 md:grid-cols-4"
        >
          <Card className="bg-gradient-to-r from-blue-500/10 to-blue-500/5">
            <div className="text-center">
              <div className="text-3xl font-bold text-blue-400">{stats.dueToday}</div>
              <div className="text-sm text-[var(--text-muted)]">Due Today</div>
            </div>
          </Card>
          <Card className="bg-gradient-to-r from-green-500/10 to-green-500/5">
            <div className="text-center">
              <div className="text-3xl font-bold text-green-400">{stats.masteredCards}</div>
              <div className="text-sm text-[var(--text-muted)]">Mastered</div>
            </div>
          </Card>
          <Card className="bg-gradient-to-r from-orange-500/10 to-orange-500/5">
            <div className="text-center">
              <div className="text-3xl font-bold text-orange-400">{stats.overdueCards}</div>
              <div className="text-sm text-[var(--text-muted)]">Overdue</div>
            </div>
          </Card>
          <Card className="bg-gradient-to-r from-purple-500/10 to-purple-500/5">
            <div className="text-center">
              <div className="text-3xl font-bold text-purple-400">{stats.totalCards}</div>
              <div className="text-sm text-[var(--text-muted)]">Total Cards</div>
            </div>
          </Card>
        </motion.div>
      )}

      {/* Tabs */}
      <Card>
        <div className="mb-6 flex gap-2">
          <Button
            variant={tab === "due" ? "primary" : "secondary"}
            onClick={() => setTab("due")}
          >
            📋 Due Cards ({stats?.dueToday || 0})
          </Button>
          <Button
            variant={tab === "all" ? "primary" : "secondary"}
            onClick={() => setTab("all")}
          >
            🎯 All Cards ({stats?.totalCards || 0})
          </Button>
        </div>

        {tab === "due" ? (
          stats?.dueCards && stats.dueCards.length > 0 ? (
            <div className="space-y-3">
              {stats.dueCards.map((card, idx) => (
                <motion.div
                  key={card.conceptName}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: idx * 0.05 }}
                  className="rounded-lg bg-gradient-to-r from-[var(--surface-2)] to-[var(--surface)] p-4 border border-[var(--border)]"
                >
                  <div className="mb-3 flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold text-lg">{card.conceptName}</h3>
                      <p className="text-sm text-[var(--text-muted)]">{card.topicGoal}</p>
                    </div>
                    <div className="text-right">
                      <div className="text-2xl font-bold">{card.retentionPercent?.toFixed(0)}%</div>
                      <div className="text-xs text-[var(--text-muted)]">Retention</div>
                    </div>
                  </div>

                  {/* Progress Bar with animation */}
                  <div className="mb-4">
                    <motion.div
                      className="h-2 rounded-full bg-[var(--surface)]"
                      initial={{ width: 0 }}
                      animate={{ width: "100%" }}
                      transition={{ duration: 0.3, delay: idx * 0.05 }}
                    >
                      <motion.div
                        className="h-2 rounded-full"
                        style={{
                          width: `${card.retentionPercent || 0}%`,
                          background:
                            (card.retentionPercent || 0) < 40
                              ? "linear-gradient(90deg, #ef4444, #dc2626)"
                              : (card.retentionPercent || 0) < 70
                                ? "linear-gradient(90deg, #f59e0b, #d97706)"
                                : "linear-gradient(90deg, #22c55e, #16a34a)",
                        }}
                        initial={{ width: 0 }}
                        animate={{ width: `${card.retentionPercent || 0}%` }}
                        transition={{ delay: idx * 0.05 + 0.2, duration: 0.5 }}
                      />
                    </motion.div>
                  </div>

                  <div className="mb-4 grid grid-cols-3 gap-2 text-xs text-[var(--text-muted)]">
                    <div>Interval: {card.intervalDays || 0} days</div>
                    <div>Reps: {card.repetitions || 0}</div>
                    <div>Last: {card.daysSinceLastReview || 0} days ago</div>
                  </div>

                  {card.overdue && (
                    <motion.div
                      className="mb-3 rounded bg-red-500/20 p-2 text-center text-sm font-semibold text-red-400"
                      animate={{ scale: [1, 1.02, 1] }}
                      transition={{ duration: 0.5, repeat: Infinity }}
                    >
                      ⚠️ {card.daysOverdue} days overdue!
                    </motion.div>
                  )}

                  <div className="grid grid-cols-5 gap-2">
                    {[
                      { quality: 0, label: "😵", color: "red" },
                      { quality: 2, label: "😕", color: "orange" },
                      { quality: 3, label: "😐", color: "yellow" },
                      { quality: 4, label: "🙂", color: "green" },
                      { quality: 5, label: "😄", color: "emerald" },
                    ].map((q) => (
                      <motion.div key={q.quality} whileHover={{ scale: 1.05 }}>
                        <Button
                          className={`w-full text-xs`}
                          variant="secondary"
                          onClick={async () => {
                            await completeRevisionApi({ conceptName: card.conceptName, quality: q.quality });
                            setStats({
                              ...stats,
                              dueCards: stats.dueCards.filter(c => c.conceptName !== card.conceptName),
                              dueToday: Math.max(0, stats.dueToday - 1),
                            });
                          }}
                        >
                          {q.label}
                        </Button>
                      </motion.div>
                    ))}
                  </div>
                </motion.div>
              ))}
            </div>
          ) : (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="rounded-lg bg-gradient-to-r from-green-500/10 to-emerald-500/10 border border-green-500/30 p-8 text-center"
            >
              <div className="text-5xl mb-3">🎉</div>
              <p className="text-xl font-semibold text-green-400">No revisions due today!</p>
              <p className="text-sm text-[var(--text-muted)]">You are up to date. Come back tomorrow.</p>
            </motion.div>
          )
        ) : (
          <div className="space-y-2">
            {allCards.length > 0 ? (
              allCards.map((card, idx) => (
                <motion.div
                  key={card.conceptName}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: idx * 0.03 }}
                  className="grid grid-cols-5 gap-3 rounded-lg bg-[var(--surface-2)] p-3 text-sm hover:bg-[var(--surface)] transition-colors"
                >
                  <div className="font-medium truncate">{card.conceptName}</div>
                  <div className="text-right text-[var(--text-muted)]">{card.retentionPercent?.toFixed(0) || 0}%</div>
                  <div className="text-right text-[var(--text-muted)]">{card.nextReviewAt ? new Date(card.nextReviewAt).toLocaleDateString() : "-"}</div>
                  <div className={`text-right font-semibold ${card.overdue ? "text-red-400" : card.status === "ACTIVE" ? "text-green-400" : "text-yellow-400"}`}>
                    {card.status || "-"}
                  </div>
                  <div className="text-right text-[var(--text-muted)]">{card.repetitions || 0} reps</div>
                </motion.div>
              ))
            ) : (
              <div className="text-center text-[var(--text-muted)] py-8">No revision cards yet</div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}
