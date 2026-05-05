import { useEffect, useState } from "react";
import { analyticsHeatmapApi, analyticsOverviewApi, analyticsWeakConceptsApi, analyticsWeeklyApi } from "../api/analytics";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { Link } from "react-router-dom";
import { TrendingUp, Target, Clock, Calendar, Flame, Award, BarChart3 } from "lucide-react";

export default function AnalyticsPage() {
  const [overview, setOverview] = useState(null);
  const [heatmap, setHeatmap] = useState([]);
  const [weak, setWeak] = useState([]);
  const [strong, setStrong] = useState([]);
  const [weekly, setWeekly] = useState([]);

  useEffect(() => {
    analyticsOverviewApi().then(setOverview).catch(() => setOverview({}));
    analyticsHeatmapApi().then((d) => setHeatmap(d?.days || d || [])).catch(() => setHeatmap([]));
    analyticsWeakConceptsApi()
      .then((d) => {
        setWeak(d?.weakConcepts || []);
        setStrong(d?.strongConcepts || []);
      })
      .catch(() => {
        setWeak([]);
        setStrong([]);
      });
    analyticsWeeklyApi().then((d) => setWeekly(d?.weeks || d || [])).catch(() => setWeekly([]));
  }, []);

  return (
    <div className="space-y-6">
      {/* Hero Header */}
      <div className="rounded-[2rem] bg-gradient-to-r from-emerald-600 via-teal-700 to-cyan-700 p-6 text-white shadow-2xl shadow-slate-900/30">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="mb-3 inline-flex rounded-full bg-white/15 px-4 py-2 text-xs uppercase tracking-[0.25em] text-slate-100">Learning Analytics</p>
            <h1 className="text-4xl font-semibold tracking-tight">Your Progress Dashboard</h1>
            <p className="mt-2 text-sm text-slate-200">Track your learning journey and performance insights</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-3xl bg-white/10 p-4 text-center backdrop-blur-sm">
              <div className="flex items-center justify-center mb-2">
                <Flame className="h-5 w-5 text-orange-300 mr-2" />
                <p className="text-xs uppercase text-slate-200">Current Streak</p>
              </div>
              <p className="text-2xl font-bold">{overview?.currentStreak ?? 0}</p>
            </div>
            <div className="rounded-3xl bg-white/10 p-4 text-center backdrop-blur-sm">
              <div className="flex items-center justify-center mb-2">
                <Target className="h-5 w-5 text-green-300 mr-2" />
                <p className="text-xs uppercase text-slate-200">Accuracy</p>
              </div>
              <p className="text-2xl font-bold">{(overview?.overallAccuracy ?? 0).toFixed(2)}%</p>
            </div>
          </div>
        </div>
      </div>

      {/* Key Metrics Grid*/}
      <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-4">
        <Card className="rounded-3xl border-0 bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-950/50 dark:to-indigo-950/50 p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-blue-100 dark:bg-blue-900/50 p-3">
              <Clock className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-sm text-slate-600 dark:text-slate-400">Study Hours</p>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{overview?.totalStudyTimeHours ?? 0}h</p>
            </div>
          </div>
        </Card>

        <Card className="rounded-3xl border-0 bg-gradient-to-br from-green-50 to-emerald-50 dark:from-green-950/50 dark:to-emerald-950/50 p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-green-100 dark:bg-green-900/50 p-3">
              <Calendar className="h-6 w-6 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-sm text-slate-600 dark:text-slate-400">Active Days</p>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{overview?.activeDaysLast30 ?? 0}</p>
            </div>
          </div>
        </Card>

        <Card className="rounded-3xl border-0 bg-gradient-to-br from-purple-50 to-violet-50 dark:from-purple-950/50 dark:to-violet-950/50 p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-purple-100 dark:bg-purple-900/50 p-3">
              <BarChart3 className="h-6 w-6 text-purple-600 dark:text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-slate-600 dark:text-slate-400">This Week</p>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{overview?.questionsThisWeek ?? 0}</p>
            </div>
          </div>
        </Card>

        <Card className="rounded-3xl border-0 bg-gradient-to-br from-orange-50 to-red-50 dark:from-orange-950/50 dark:to-red-950/50 p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-orange-100 dark:bg-orange-900/50 p-3">
              <Award className="h-6 w-6 text-orange-600 dark:text-orange-400" />
            </div>
            <div>
              <p className="text-sm text-slate-600 dark:text-slate-400">Due Revisions</p>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{overview?.dueRevisions ?? 0}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Progress Summary */}
      <Card className="rounded-3xl border-0 bg-gradient-to-r from-slate-50 to-gray-50 dark:from-slate-900/50 dark:to-gray-900/50 p-6">
        <div className="flex items-center gap-3 mb-4">
          <TrendingUp className="h-6 w-6 text-slate-600 dark:text-slate-400" />
          <h3 className="text-xl font-semibold">Progress Summary</h3>
        </div>
        <p className="text-slate-700 dark:text-slate-300 leading-relaxed">
          You're currently on <span className="font-semibold text-slate-900 dark:text-white">{overview?.currentDifficulty || "Unknown"}</span> difficulty
          with <span className="font-semibold text-slate-900 dark:text-white">{(overview?.overallAccuracy ?? 0).toFixed(2)}%</span> overall accuracy.
          This week you've completed <span className="font-semibold text-slate-900 dark:text-white">{overview?.questionsThisWeek ?? 0}</span> questions
          and have <span className="font-semibold text-slate-900 dark:text-white">{overview?.dueRevisions ?? 0}</span> revision cards ready for review.
        </p>
      </Card>

      {/* Activity Heatmap */}
      <Card className="rounded-3xl p-6">
        <h3 className="mb-4 text-xl font-semibold flex items-center gap-2">
          <div className="h-3 w-3 rounded-full bg-slate-400"></div>
          Activity Heatmap (Last 90 Days)
        </h3>
        <div className="grid grid-cols-13 gap-1">
          {heatmap.length ? (
            heatmap.map((day, i) => (
              <div
                key={`${day.date || i}`}
                className="h-4 w-4 rounded-md transition-all hover:scale-125"
                title={`${day.date}: ${day.questions || 0} questions, ${(day.accuracy || 0).toFixed(2)}% accuracy`}
                style={{
                  background:
                    day.intensity === 4
                      ? "#10b981"
                      : day.intensity === 3
                        ? "#059669"
                        : day.intensity === 2
                          ? "#047857"
                          : day.intensity === 1
                            ? "#065f46"
                            : "#e5e7eb",
                }}
              />
            ))
          ) : (
            <p className="text-sm text-slate-500 col-span-full py-8 text-center">No activity data yet. Start learning to see your progress!</p>
          )}
        </div>
      </Card>

      {/* Performance Analysis */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Weak Concepts */}
        <Card className="rounded-3xl p-6">
          <h3 className="mb-4 text-xl font-semibold text-red-600 dark:text-red-400">Areas for Improvement</h3>
          <div className="space-y-4">
            {weak.length ? (
              weak.slice(0, 6).map((item, idx) => (
                <div key={item.concept || item.conceptName || idx} className="rounded-2xl bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 p-4">
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-medium text-slate-900 dark:text-white">{item.concept || item.conceptName}</span>
                    <span className="text-sm font-bold text-red-600 dark:text-red-400">{item.score ?? 0}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-red-100 dark:bg-red-900/50 mb-3">
                    <div className="h-2 rounded-full bg-red-500" style={{ width: `${item.score ?? 0}%` }} />
                  </div>
                  <Link to={`/practice?concept=${encodeURIComponent(item.concept || item.conceptName)}&topic=${encodeURIComponent(overview?.goal || "")}`}>
                    <Button variant="outline" size="sm" className="w-full border-red-300 text-red-700 hover:bg-red-50 dark:border-red-700 dark:text-red-300 dark:hover:bg-red-950/50">
                      Practice This Concept
                    </Button>
                  </Link>
                </div>
              ))
            ) : (
              <div className="text-center py-8">
                <Award className="h-12 w-12 text-slate-400 mx-auto mb-3" />
                <p className="text-slate-500">No weak concepts identified yet. Keep learning!</p>
              </div>
            )}
          </div>
        </Card>

        {/* Weekly Performance */}
        <Card className="rounded-3xl p-6">
          <h3 className="mb-4 text-xl font-semibold text-blue-600 dark:text-blue-400">Weekly Performance</h3>
          <div className="space-y-4">
            {weekly.length ? (
              weekly.slice(0, 4).map((week, idx) => (
                <div key={idx} className="rounded-2xl bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 p-4">
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-medium text-slate-900 dark:text-white">{week.week || week.label || `Week ${idx + 1}`}</span>
                    <div className="text-right">
                      <div className="text-sm text-slate-600 dark:text-slate-400">{week.questions || 0} questions</div>
                      <div className="text-sm font-bold text-blue-600 dark:text-blue-400">{(week.accuracy || 0).toFixed(2)}% accuracy</div>
                    </div>
                  </div>
                  <div className="h-2 rounded-full bg-blue-100 dark:bg-blue-900/50 mb-2">
                    <div className="h-2 rounded-full bg-blue-500" style={{ width: `${week.accuracy || 0}%` }} />
                  </div>
                  <div className="text-xs text-slate-500 dark:text-slate-400">
                    {week.activeDays || 0} active days
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8">
                <BarChart3 className="h-12 w-12 text-slate-400 mx-auto mb-3" />
                <p className="text-slate-500">No weekly data yet. Complete some quizzes to see trends!</p>
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* Strong Concepts */}
      <Card className="rounded-3xl p-6">
        <h3 className="mb-4 text-xl font-semibold text-green-600 dark:text-green-400 flex items-center gap-2">
          <Award className="h-5 w-5" />
          Strong Concepts
        </h3>
        <div className="flex flex-wrap gap-3">
          {strong.length ? (
            strong.map((item, idx) => (
              <div key={item.concept || idx} className="rounded-full bg-green-100 dark:bg-green-950/50 border border-green-200 dark:border-green-800 px-4 py-2">
                <span className="text-sm font-medium text-green-800 dark:text-green-300">
                  {item.concept} • {item.score}%
                </span>
              </div>
            ))
          ) : (
            <div className="w-full text-center py-8">
              <Target className="h-12 w-12 text-slate-400 mx-auto mb-3" />
              <p className="text-slate-500">Keep practicing to discover your strong concepts!</p>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
