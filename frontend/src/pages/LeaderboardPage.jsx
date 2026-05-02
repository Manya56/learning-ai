import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { getWeeklyLeaderboardApi, getAllTimeLeaderboardApi, getMyXpApi } from "../api/leaderboard";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

const getRankIcon = (rank) => {
  if (rank === 1) return "🥇";
  if (rank === 2) return "🥈";
  if (rank === 3) return "🥉";
  return `#${rank}`;
};

const getXpBadge = (xp) => {
  if (xp >= 10000) return "🏆";
  if (xp >= 5000) return "⭐";
  if (xp >= 1000) return "🔥";
  return "🌱";
};

export default function LeaderboardPage() {
  const [weekly, setWeekly] = useState(null);
  const [allTime, setAllTime] = useState(null);
  const [myXp, setMyXp] = useState(null);
  const [activeTab, setActiveTab] = useState("weekly");

  useEffect(() => {
    getWeeklyLeaderboardApi()
      .then((data) => {
        const entries = (data?.entries || []).map((u) => ({
          rank: u.rank,
          username: u.displayName || "Anonymous",
          xp: u.xp || 0,
          userId: u.userId,
          isYou: u.isCurrentUser || false,
        })).filter(u => !u.isYou);
        const normalized = {
          leaderboard: entries,
          yourRank: data?.myEntry ? {
            rank: data.myEntry.rank,
            username: data.myEntry.displayName,
            xp: data.myEntry.xp || 0,
          } : null,
        };
        setWeekly(normalized);
      })
      .catch(() => setWeekly({ leaderboard: [], yourRank: null }));
    
    getAllTimeLeaderboardApi()
      .then((data) => {
        const entries = (data?.entries || []).map((u) => ({
          rank: u.rank,
          username: u.displayName || "Anonymous",
          xp: u.xp || 0,
          userId: u.userId,
          isYou: u.isCurrentUser || false,
        })).filter(u => !u.isYou);
        const normalized = {
          leaderboard: entries,
          yourRank: data?.myEntry ? {
            rank: data.myEntry.rank,
            username: data.myEntry.displayName,
            xp: data.myEntry.xp || 0,
          } : null,
        };
        setAllTime(normalized);
      })
      .catch(() => setAllTime({ leaderboard: [], yourRank: null }));
    
    getMyXpApi()
      .then((data) => setMyXp(data || {}))
      .catch(() => setMyXp({}));
  }, []);

  const currentData = activeTab === "weekly" ? weekly : allTime;

  return (
    <div className="space-y-6">
      {/* XP Summary */}
      <Card>
        <h2 className="mb-4 text-2xl font-bold">Your XP Stats</h2>
        {!myXp ? (
          <div className="h-20 animate-pulse rounded bg-[var(--surface-2)]" />
        ) : (
          <div className="grid gap-4 md:grid-cols-4">
            <div className="rounded-lg bg-gradient-to-r from-purple-500/20 to-blue-500/20 p-4 text-center">
              <div className="text-2xl">{getXpBadge(myXp.weeklyXp || 0)}</div>
              <div className="text-lg font-semibold">{myXp.weeklyXp || 0}</div>
              <div className="text-sm text-[var(--text-muted)]">Weekly XP</div>
            </div>
            <div className="rounded-lg bg-gradient-to-r from-green-500/20 to-teal-500/20 p-4 text-center">
              <div className="text-2xl">{getXpBadge(myXp.allTimeXp || 0)}</div>
              <div className="text-lg font-semibold">{myXp.allTimeXp || 0}</div>
              <div className="text-sm text-[var(--text-muted)]">All-Time XP</div>
            </div>
            <div className="rounded-lg bg-gradient-to-r from-orange-500/20 to-red-500/20 p-4 text-center">
              <div className="text-2xl">🏅</div>
              <div className="text-lg font-semibold">#{myXp.weeklyRank || "?"}</div>
              <div className="text-sm text-[var(--text-muted)]">Weekly Rank</div>
            </div>
            <div className="rounded-lg bg-gradient-to-r from-pink-500/20 to-purple-500/20 p-4 text-center">
              <div className="text-2xl">📅</div>
              <div className="text-lg font-semibold">Week {myXp.currentWeek || "?"}</div>
              <div className="text-sm text-[var(--text-muted)]">Current Week</div>
            </div>
          </div>
        )}
      </Card>

      {/* Leaderboard Tabs */}
      <Card>
        <div className="mb-4 flex space-x-2">
          <Button
            variant={activeTab === "weekly" ? "primary" : "ghost"}
            onClick={() => setActiveTab("weekly")}
          >
            🏆 Weekly
          </Button>
          <Button
            variant={activeTab === "all-time" ? "primary" : "ghost"}
            onClick={() => setActiveTab("all-time")}
          >
            🌟 All-Time
          </Button>
        </div>

        {!currentData ? (
          <div className="space-y-2">
            {[...Array(10)].map((_, i) => (
              <div key={i} className="h-12 animate-pulse rounded bg-[var(--surface-2)]" />
            ))}
          </div>
        ) : (
          <div className="space-y-2">
            {currentData.leaderboard?.map((entry, index) => (
              <motion.div
                key={entry.userId}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className={`flex items-center justify-between rounded-lg p-3 transition-all duration-300 ${
                  entry.isYou
                    ? "bg-gradient-to-r from-yellow-500/20 to-orange-500/20 border border-yellow-500/30"
                    : "bg-[var(--surface-2)] hover:bg-[var(--surface)]"
                }`}
              >
                <div className="flex items-center space-x-3">
                  <motion.div
                    className="text-lg font-bold"
                    animate={{ scale: entry.isYou ? [1, 1.2, 1] : 1 }}
                    transition={{ duration: 0.5 }}
                  >
                    {getRankIcon(entry.rank)}
                  </motion.div>
                  <div>
                    <div className="font-semibold">{entry.username}</div>
                    <div className="text-sm text-[var(--text-muted)]">{entry.xp} XP</div>
                  </div>
                </div>
                {entry.isYou && <motion.div className="text-yellow-400" animate={{ rotate: [0, 10, -10, 0] }} transition={{ duration: 1, repeat: Infinity }}>⭐ You</motion.div>}
              </motion.div>
            ))}
            {currentData.yourRank && (
              <motion.div
                className="mt-4 rounded-lg bg-gradient-to-r from-yellow-500/20 to-orange-500/20 border border-yellow-500/30 p-3"
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.5 }}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <motion.div
                      className="text-lg font-bold"
                      animate={{ scale: [1, 1.2, 1] }}
                      transition={{ duration: 0.5, repeat: Infinity }}
                    >
                      {getRankIcon(currentData.yourRank.rank)}
                    </motion.div>
                    <div>
                      <div className="font-semibold">{currentData.yourRank.username}</div>
                      <div className="text-sm text-[var(--text-muted)]">{currentData.yourRank.xp} XP</div>
                    </div>
                  </div>
                  <motion.div className="text-yellow-400" animate={{ rotate: [0, 10, -10, 0] }} transition={{ duration: 1, repeat: Infinity }}>⭐ You</motion.div>
                </div>
              </motion.div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}