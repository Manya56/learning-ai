import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { getWeeklyLeaderboardApi, getAllTimeLeaderboardApi, getMyXpApi } from "../api/leaderboard";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";

const rankMedal = (rank) =>
  rank === 1 ? "emoji_events" : rank === 2 ? "workspace_premium" : rank === 3 ? "military_tech" : null;

const normalize = (data) => {
  const entries = (data?.entries || []).map((u) => ({
    rank: u.rank,
    username: u.displayName || "Anonymous",
    xp: u.xp || 0,
    userId: u.userId,
    isYou: u.isCurrentUser || false,
  }));
  const myEntry = data?.myEntry
    ? { rank: data.myEntry.rank, username: data.myEntry.displayName || "You", xp: data.myEntry.xp || 0, isYou: true }
    : entries.find((e) => e.isYou) || null;
  return { entries, myEntry };
};

const Tile = ({ icon, value, label, fill = 0, accent = false }) => (
  <div className={`rounded-2xl p-4 ${accent ? "bg-[var(--accent)]" : "bg-[var(--surface-2)]"}`}>
    <div className="flex items-center gap-1.5">
      <Icon name={icon} size={18} fill={fill} className={accent ? "text-white" : "text-[var(--accent)]"} />
      <span className={`text-2xl font-extrabold ${accent ? "text-white" : "text-[var(--text)]"}`}>{value}</span>
    </div>
    <p className={`mt-1 text-[11px] font-bold uppercase tracking-wide ${accent ? "text-white/70" : "text-[var(--text-muted)]"}`}>{label}</p>
  </div>
);

const Row = ({ entry, index }) => {
  const medal = rankMedal(entry.rank);
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: Math.min(index, 12) * 0.03 }}
      className="flex items-center justify-between gap-3 rounded-2xl bg-[var(--surface-2)] px-4 py-3"
    >
      <div className="flex min-w-0 items-center gap-3">
        <span className="flex w-8 shrink-0 items-center justify-center text-sm font-extrabold text-[var(--text-muted)]">
          {medal ? <Icon name={medal} size={22} fill={1} className="text-[var(--accent)]" /> : `#${entry.rank}`}
        </span>
        <span className="truncate font-bold text-[var(--text)]">{entry.username}</span>
      </div>
      <span className="shrink-0 text-sm font-bold text-[var(--text-muted)]">{entry.xp} XP</span>
    </motion.div>
  );
};

const YouRow = ({ entry }) => {
  const medal = rankMedal(entry.rank);
  return (
    <motion.div
      animate={{ scale: [1, 1.015, 1] }}
      transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut" }}
      className="flex items-center justify-between gap-3 rounded-2xl bg-[var(--accent)] px-4 py-3 text-white shadow-[0_4px_0_0_var(--accent-hover)]"
    >
      <div className="flex min-w-0 items-center gap-3">
        <span className="flex w-8 shrink-0 items-center justify-center text-sm font-extrabold text-white/90">
          {medal ? <Icon name={medal} size={22} fill={1} className="text-white" /> : `#${entry.rank}`}
        </span>
        <span className="truncate font-bold">{entry.username} (You)</span>
      </div>
      <span className="shrink-0 text-sm font-bold text-white/90">{entry.xp} XP</span>
    </motion.div>
  );
};

export default function LeaderboardPage() {
  const [weekly, setWeekly] = useState(null);
  const [allTime, setAllTime] = useState(null);
  const [myXp, setMyXp] = useState(null);
  const [activeTab, setActiveTab] = useState("weekly");

  useEffect(() => {
    getWeeklyLeaderboardApi().then((d) => setWeekly(normalize(d))).catch(() => setWeekly({ entries: [], myEntry: null }));
    getAllTimeLeaderboardApi().then((d) => setAllTime(normalize(d))).catch(() => setAllTime({ entries: [], myEntry: null }));
    getMyXpApi().then((d) => setMyXp(d || {})).catch(() => setMyXp({}));
  }, []);

  const data = activeTab === "weekly" ? weekly : allTime;
  const others = (data?.entries || []).filter((e) => !e.isYou);
  const weeklyRank = myXp?.weeklyRank;
  const rankText = weeklyRank && weeklyRank > 0 ? `#${weeklyRank}` : "—";

  return (
    <div className="space-y-5">
      {/* Your XP — one accent tile */}
      <Card>
        <h3 className="mb-4 text-lg font-extrabold tracking-tight">Your XP</h3>
        {!myXp ? (
          <div className="h-20 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Tile icon="bolt" value={myXp.weeklyXp || 0} label="Weekly XP" accent />
            <Tile icon="star" fill={1} value={myXp.allTimeXp || 0} label="All-time XP" />
            <Tile icon="military_tech" value={rankText} label="Weekly rank" />
            <Tile icon="calendar_month" value={`W${myXp.currentWeek || "?"}`} label="Current week" />
          </div>
        )}
      </Card>

      {/* Leaderboard */}
      <Card>
        <div className="mb-4 flex gap-2">
          <Button variant={activeTab === "weekly" ? "primary" : "ghost"} className="gap-1.5" onClick={() => setActiveTab("weekly")}>
            <Icon name="emoji_events" size={18} fill={1} /> Weekly
          </Button>
          <Button variant={activeTab === "all-time" ? "primary" : "ghost"} className="gap-1.5" onClick={() => setActiveTab("all-time")}>
            <Icon name="star" size={18} fill={1} /> All-time
          </Button>
        </div>

        {!data ? (
          <div className="space-y-2">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="h-14 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
            ))}
          </div>
        ) : others.length || data.myEntry ? (
          <div className="space-y-2">
            {others.map((entry, index) => (
              <Row key={entry.userId || entry.rank} entry={entry} index={index} />
            ))}
            {data.myEntry && <YouRow entry={data.myEntry} />}
          </div>
        ) : (
          <div className="py-10 text-center">
            <Icon name="emoji_events" size={40} className="text-[var(--text-muted)]" />
            <p className="mt-2 text-sm font-medium text-[var(--text-muted)]">No rankings yet. Earn XP to get on the board!</p>
          </div>
        )}
      </Card>
    </div>
  );
}
