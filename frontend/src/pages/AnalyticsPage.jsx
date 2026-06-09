import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { analyticsHeatmapApi, analyticsOverviewApi, analyticsWeakConceptsApi } from "../api/analytics";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";

const WEEKS = 53; // a full year, GitHub-style
const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const pad = (n) => String(n).padStart(2, "0");
const ymd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

// Empty days = visible gray; active days = accent opacity steps.
const levelStyle = (intensity) => {
  const op = { 1: 0.35, 2: 0.55, 3: 0.78, 4: 1 }[intensity];
  return op ? { backgroundColor: "var(--accent)", opacity: op } : { backgroundColor: "var(--border)" };
};
const cellStyle = (cell) => (cell.future ? { backgroundColor: "transparent" } : levelStyle(cell.data?.intensity ?? 0));

const Tile = ({ icon, value, label, fill = 0 }) => (
  <div className="rounded-2xl bg-[var(--surface-2)] p-3">
    <div className="flex items-center gap-1.5">
      <Icon name={icon} size={18} fill={fill} className="text-[var(--accent)]" />
      <span className="text-xl font-extrabold text-[var(--text)]">{value}</span>
    </div>
    <p className="mt-0.5 text-[10px] font-bold uppercase tracking-wide text-[var(--text-muted)]">{label}</p>
  </div>
);

const EmptyState = ({ icon, text }) => (
  <div className="py-8 text-center">
    <Icon name={icon} size={40} className="text-[var(--text-muted)]" />
    <p className="mt-2 text-sm font-medium text-[var(--text-muted)]">{text}</p>
  </div>
);

export default function AnalyticsPage() {
  const [overview, setOverview] = useState(null);
  const [heatmap, setHeatmap] = useState([]);
  const [weak, setWeak] = useState([]);
  const [strong, setStrong] = useState([]);
  const [selected, setSelected] = useState(null);
  const [insightsOpen, setInsightsOpen] = useState(false);

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
  }, []);

  const accuracy = (overview?.overallAccuracy ?? 0).toFixed(0);

  // One contiguous year: 53 week-columns × 7 day-rows, ending this week.
  const byDate = new Map((heatmap || []).map((d) => [d.date, d]));
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(today);
  start.setDate(today.getDate() + (6 - today.getDay()) - (WEEKS * 7 - 1)); // a Sunday ~1 year back
  const columns = Array.from({ length: WEEKS }, (_, w) =>
    Array.from({ length: 7 }, (_, r) => {
      const date = new Date(start);
      date.setDate(start.getDate() + w * 7 + r);
      return { date, data: byDate.get(ymd(date)), future: date > today };
    })
  );
  // Month label sits above the week-column where a new month starts.
  const monthLabels = columns.map((col, i) => {
    const m = col[0].date.getMonth();
    return i === 0 || m !== columns[i - 1][0].date.getMonth() ? MONTHS[m] : "";
  });
  const activeDays = columns.flat().filter((c) => (c.data?.intensity ?? 0) > 0).length;

  return (
    <div className="space-y-5">
      {/* Header + key stats */}
      <Card>
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Here's how you're doing!</h1>
          <p className="mt-1 text-sm font-medium text-[var(--text-muted)]">A snapshot of your learning so far</p>
          <div className="mt-3 flex flex-wrap items-center gap-x-5 gap-y-1 text-sm font-bold text-[var(--text)]">
            <span className="flex items-center gap-1.5">
              <Icon name="flag" size={16} className="text-[var(--accent)]" /> {overview?.goal || "—"}
            </span>
            <span className="flex items-center gap-1.5">
              <Icon name="bolt" size={16} className="text-[var(--accent)]" /> {overview?.currentDifficulty || "—"} level
            </span>
          </div>
        </div>
        <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Tile icon="local_fire_department" fill={1} value={overview?.currentStreak ?? 0} label="Day streak" />
          <Tile icon="target" value={`${accuracy}%`} label="Accuracy" />
          <Tile icon="schedule" value={`${overview?.totalStudyTimeHours ?? 0}h`} label="Study hours" />
          <Tile icon="calendar_month" value={overview?.activeDaysLast30 ?? 0} label="Active days" />
        </div>
      </Card>

      {/* Activity heatmap — one contiguous year, GitHub-style */}
      <Card>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-lg font-extrabold tracking-tight">Activity Heatmap</h3>
          <span className="text-xs font-medium text-[var(--text-muted)]">
            {activeDays} active day{activeDays === 1 ? "" : "s"} in the last year
          </span>
        </div>

        <div className="overflow-x-auto pb-1">
          <div className="min-w-[700px]">
            {/* month labels */}
            <div className="mb-1 flex gap-[3px]">
              <div className="w-6 shrink-0" />
              <div className="flex flex-1 gap-[3px]">
                {monthLabels.map((m, i) => (
                  <span key={i} className="min-w-0 flex-1 whitespace-nowrap text-[10px] font-bold text-[var(--text-muted)]">{m}</span>
                ))}
              </div>
            </div>
            {/* grid */}
            <div className="flex gap-[3px]">
              <div className="flex w-6 shrink-0 flex-col gap-[3px] text-[9px] font-bold leading-none text-[var(--text-muted)]">
                {["", "M", "", "W", "", "F", ""].map((d, i) => (
                  <span key={i} className="flex flex-1 items-center">{d}</span>
                ))}
              </div>
              <div className="flex flex-1 gap-[3px]">
                {columns.map((col, ci) => (
                  <div key={ci} className="flex flex-1 flex-col gap-[3px]">
                    {col.map((cell, ri) => (
                      <button
                        key={ri}
                        type="button"
                        disabled={cell.future}
                        onClick={() => setSelected(cell)}
                        title={cell.future ? "" : `${ymd(cell.date)} · ${cell.data?.questions || 0} questions`}
                        className={`aspect-square w-full rounded-[3px] transition-transform hover:scale-125 ${
                          selected && cell.date.getTime() === selected.date.getTime()
                            ? "ring-2 ring-[var(--text)] ring-offset-1 ring-offset-[var(--surface)]"
                            : ""
                        }`}
                        style={cellStyle(cell)}
                      />
                    ))}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* legend */}
        <div className="mt-3 flex items-center justify-end gap-1.5 text-[10px] font-bold uppercase tracking-wide text-[var(--text-muted)]">
          Less
          {[0, 1, 2, 3, 4].map((n) => (
            <span key={n} className="h-3 w-3 rounded-[2px]" style={levelStyle(n)} />
          ))}
          More
        </div>

        {/* selected day detail */}
        {selected && (
          <div className="mt-4 rounded-2xl bg-[var(--surface-2)] p-4">
            <p className="text-sm font-extrabold text-[var(--text)]">
              {selected.date.toLocaleDateString(undefined, { weekday: "long", year: "numeric", month: "long", day: "numeric" })}
            </p>
            {selected.data ? (
              <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm font-bold text-[var(--text)]">
                <span className="flex items-center gap-1.5">
                  <Icon name="quiz" size={16} className="text-[var(--accent)]" /> {selected.data.questions || 0} questions
                </span>
                <span className="flex items-center gap-1.5">
                  <Icon name="target" size={16} className="text-[var(--accent)]" /> {(selected.data.accuracy || 0).toFixed(0)}% accuracy
                </span>
                <span className="flex items-center gap-1.5">
                  <Icon name="schedule" size={16} className="text-[var(--accent)]" /> {((selected.data.timeMs || 0) / 1000).toFixed(1)}s
                </span>
              </div>
            ) : (
              <p className="mt-1 text-sm font-medium text-[var(--text-muted)]">No activity on this day.</p>
            )}
          </div>
        )}
      </Card>

      {/* Insights — summary always shown, details collapse on click */}
      <Card>
        <button
          type="button"
          onClick={() => setInsightsOpen((o) => !o)}
          className="flex w-full items-center justify-between gap-2 text-left"
        >
          <div className="flex items-center gap-2">
            <Icon name="trending_up" size={20} className="text-[var(--accent)]" />
            <h3 className="text-lg font-extrabold tracking-tight">Insights</h3>
          </div>
          <Icon
            name="expand_more"
            size={22}
            className={`shrink-0 text-[var(--text-muted)] transition-transform duration-200 ${insightsOpen ? "rotate-180" : ""}`}
          />
        </button>

        <p className="mt-3 text-sm leading-relaxed text-[var(--text)]">
          You're on <span className="font-extrabold text-[var(--accent-hover)]">{overview?.currentDifficulty || "—"}</span> difficulty with{" "}
          <span className="font-extrabold text-[var(--accent-hover)]">{accuracy}%</span> overall accuracy. This week you completed{" "}
          <span className="font-extrabold text-[var(--accent-hover)]">{overview?.questionsThisWeek ?? 0}</span> questions and have{" "}
          <span className="font-extrabold text-[var(--accent-hover)]">{overview?.dueRevisions ?? 0}</span> revision cards ready for review.
        </p>

        {!insightsOpen && (
          <button
            type="button"
            onClick={() => setInsightsOpen(true)}
            className="mt-3 flex items-center gap-1 text-xs font-extrabold uppercase tracking-wide text-[var(--accent-hover)]"
          >
            Show details <Icon name="expand_more" size={16} />
          </button>
        )}

        <AnimatePresence initial={false}>
          {insightsOpen && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.25, ease: "easeOut" }}
              className="overflow-hidden"
            >
              <div className="mt-5 grid gap-6 border-t-2 border-[var(--border)] pt-5 lg:grid-cols-2">
                {/* Areas for improvement */}
                <div>
                  <h4 className="mb-3 text-sm font-extrabold uppercase tracking-wide text-[var(--text-muted)]">Areas for improvement</h4>
                  {weak.length ? (
                    <div className="space-y-3">
                      {weak.slice(0, 6).map((item, idx) => {
                        const name = item.concept || item.conceptName;
                        return (
                          <div key={name || idx} className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-4">
                            <div className="flex items-center justify-between gap-3">
                              <div className="flex min-w-0 items-center gap-2">
                                <div className="shrink-0 rounded-xl bg-[var(--accent-light)] p-2">
                                  <Icon name="menu_book" size={18} className="text-[var(--accent)]" />
                                </div>
                                <span className="truncate font-bold text-[var(--text)]">{name}</span>
                              </div>
                              <span className="shrink-0 text-sm font-extrabold text-[var(--accent-hover)]">{item.score ?? 0}%</span>
                            </div>
                            <div className="mt-3 h-2 overflow-hidden rounded-full bg-[var(--surface-2)]">
                              <div className="h-full rounded-full bg-[var(--accent)]" style={{ width: `${item.score ?? 0}%` }} />
                            </div>
                            <Link to={`/space/practice?concept=${encodeURIComponent(name || "")}&topic=${encodeURIComponent(overview?.goal || "")}`}>
                              <Button variant="ghost" className="mt-3 w-full gap-1.5">
                                <Icon name="code" size={16} /> Practice this concept
                              </Button>
                            </Link>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <EmptyState icon="workspace_premium" text="No weak concepts yet. Keep learning!" />
                  )}
                </div>

                {/* Strong concepts */}
                <div>
                  <h4 className="mb-3 text-sm font-extrabold uppercase tracking-wide text-[var(--text-muted)]">Strong concepts</h4>
                  {strong.length ? (
                    <div className="flex flex-wrap gap-2">
                      {strong.map((item, idx) => (
                        <span
                          key={item.concept || idx}
                          className="flex items-center gap-1.5 rounded-full bg-[var(--accent-light)] px-3 py-2 text-sm font-bold text-[var(--accent-hover)]"
                        >
                          <Icon name="check_circle" size={16} fill={1} /> {item.concept} · {item.score}%
                        </span>
                      ))}
                    </div>
                  ) : (
                    <EmptyState icon="target" text="Keep practicing to discover your strong concepts!" />
                  )}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </Card>
    </div>
  );
}
