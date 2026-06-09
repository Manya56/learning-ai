import { Suspense, useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { getRoadmapApi } from "../../api/roadmap";
import Icon from "../ui/Icon";
import ConfirmModal from "../ui/ConfirmModal";
import Spinner from "../ui/Spinner";
import { useUiStore } from "../../store/uiStore";
import { useProfileStore } from "../../store/profileStore";
import { getConceptPoolFromTopic, getPreferredConcept } from "../../utils/study";

const TABS = [
  { to: "learn", label: "Learn", icon: "menu_book", needsConcept: true },
  { to: "quiz", label: "Quiz", icon: "quiz", needsConcept: true },
  { to: "practice", label: "Practice", icon: "code", needsConcept: true },
  { to: "mentor", label: "Mentor", icon: "psychology", needsConcept: false },
  { to: "revision", label: "Revision", icon: "replay", needsConcept: false },
];

// Mimo / Duolingo-style learning space: no navbar. Top has chapter arrows + "Chapter X of Y",
// the centered topic title + progress, the concept list in the middle, activities on a bottom bar.
export default function SpaceShell() {
  const navigate = useNavigate();
  const location = useLocation();
  const isOverview = location.pathname === "/space" || location.pathname === "/space/";
  const [topics, setTopics] = useState([]);
  const [chapter, setChapter] = useState(0);
  const [showExit, setShowExit] = useState(false);
  const activeQuizSessionId = useUiStore((s) => s.activeQuizSessionId);
  const revisionDueCount = useProfileStore((s) => s.revisionDueCount);
  const loadRevisionCount = useProfileStore((s) => s.loadRevisionCount);

  const loadRoadmap = () =>
    getRoadmapApi()
      .then((d) => {
        const list = d?.topics || [];
        setTopics(list);
        const inProgress = list.findIndex((t) => t.status === "IN_PROGRESS");
        const unlocked = list.findIndex((t) => t.status === "UNLOCKED");
        setChapter(inProgress >= 0 ? inProgress : unlocked >= 0 ? unlocked : 0);
      })
      .catch(() => setTopics([]));

  useEffect(() => {
    loadRoadmap();
    loadRevisionCount().catch(() => {});
  }, []);

  const handleClose = () => {
    if (activeQuizSessionId) setShowExit(true);
    else navigate("/dashboard");
  };

  const total = topics.length || 1;
  const topic = topics[chapter] || null;
  const topicName = topic?.topicName || "Your topic";
  const progress = Math.round(topic?.progressPercent ?? 0);
  const locked = (topic?.status || "LOCKED") === "LOCKED";
  const pool = getConceptPoolFromTopic(topic || {});
  const concept = locked ? "" : topic?.nextConcept || getPreferredConcept(pool) || "";
  const query = `?${concept ? `concept=${encodeURIComponent(concept)}&` : ""}topic=${encodeURIComponent(topicName)}`;
  const tabTo = (t) => (t.needsConcept ? `${t.to}${query}` : t.to);

  const goChapter = (delta) => {
    setChapter((c) => Math.min(topics.length - 1, Math.max(0, c + delta)));
    navigate("/space");
  };
  const atStart = chapter <= 0;
  const atEnd = chapter >= topics.length - 1;

  const arrowClass = (disabled) =>
    `flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--surface-2)] text-[var(--text)] transition active:scale-95 ${
      disabled ? "pointer-events-none opacity-30" : "hover:bg-[var(--border)]"
    }`;

  const tabClass = ({ isActive }) =>
    `flex flex-1 flex-col items-center gap-0.5 rounded-xl py-2 text-[10px] font-extrabold transition active:scale-95 ${
      isActive ? "text-[var(--accent)]" : "text-[var(--text-muted)]"
    }`;

  return (
    <div className="relative flex h-screen flex-col bg-[var(--surface)] text-[var(--text)]">
      {isOverview ? (
        /* Overview: no app bar — just a floating close + a subtle chapter stepper */
        <div className="relative px-4 pt-4">
          <button
            type="button"
            onClick={handleClose}
            aria-label="Close"
            className="absolute right-4 top-3 flex h-9 w-9 items-center justify-center rounded-full bg-[var(--surface-2)] text-[var(--text)] transition hover:bg-[var(--border)] active:scale-95"
          >
            <Icon name="close" size={20} />
          </button>
          <div className="flex items-center justify-center gap-2">
            <button type="button" onClick={() => goChapter(-1)} disabled={atStart} aria-label="Previous chapter" className={arrowClass(atStart)}>
              <Icon name="chevron_left" size={18} />
            </button>
            <span className="text-[11px] font-extrabold uppercase tracking-wide text-[var(--text-muted)] tabular-nums">
              Chapter {Math.min(chapter + 1, total)}/{total}
            </span>
            <button type="button" onClick={() => goChapter(1)} disabled={atEnd} aria-label="Next chapter" className={arrowClass(atEnd)}>
              <Icon name="chevron_right" size={18} />
            </button>
          </div>
        </div>
      ) : (
        /* Activity: full top header with title + progress */
        <header className="border-b-2 border-[var(--border)] px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={handleClose}
              aria-label="Close"
              className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--surface-2)] text-[var(--text)] transition hover:bg-[var(--border)] active:scale-95"
            >
              <Icon name="close" size={20} />
            </button>
            <span className="text-[11px] font-extrabold uppercase tracking-wide text-[var(--text-muted)] tabular-nums">
              Chapter {Math.min(chapter + 1, total)}/{total}
            </span>
            <div className="h-9 w-9" aria-hidden />
          </div>

          <button type="button" onClick={() => navigate("/space")} className="mt-3 flex w-full items-center justify-center gap-1 active:scale-95">
            <span className="min-w-0 truncate text-lg font-extrabold tracking-tight">{topicName}</span>
            <Icon name="expand_more" size={20} className="shrink-0 text-[var(--text-muted)]" />
          </button>

          <div className="mx-auto mt-2 flex max-w-sm items-center gap-2.5">
            <div className="h-2 flex-1 overflow-hidden rounded-full bg-[var(--surface-2)]">
              <div className="h-full rounded-full bg-[var(--accent)] transition-all" style={{ width: `${progress}%` }} />
            </div>
            <span className="text-xs font-extrabold tabular-nums text-[var(--accent-hover)]">{progress}%</span>
          </div>
        </header>
      )}

      {/* Content */}
      <main className="flex-1 overflow-y-auto px-4 py-5">
        <div className="mx-auto max-w-2xl">
          <Suspense fallback={<div className="flex min-h-[50vh] items-center justify-center"><Spinner size={32} /></div>}>
            <Outlet context={{ topic, concept, topicName, query, refreshRoadmap: loadRoadmap }} />
          </Suspense>
        </div>
      </main>

      {/* Bottom activity bar — hidden on the overview, shown once inside an activity */}
      {!isOverview && (
      <nav
        className="flex gap-1 border-t-2 border-[var(--border)] bg-[var(--surface)] px-2 pt-1.5"
        style={{ paddingBottom: "max(0.5rem, env(safe-area-inset-bottom))" }}
      >
        {TABS.map((t) => (
          <NavLink key={t.to} to={tabTo(t)} className={tabClass}>
            {({ isActive }) => (
              <>
                <span className="relative">
                  <Icon name={t.icon} size={24} fill={isActive ? 1 : 0} />
                  {t.to === "revision" && revisionDueCount > 0 && (
                    <span className="absolute -right-2 -top-1.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-[var(--error)] px-1 text-[9px] font-extrabold leading-none text-white">
                      {revisionDueCount > 9 ? "9+" : revisionDueCount}
                    </span>
                  )}
                </span>
                {t.label}
              </>
            )}
          </NavLink>
        ))}
      </nav>
      )}

      <ConfirmModal
        open={showExit}
        icon="logout"
        title="Leave the space?"
        message="Your quiz isn't finished — you can resume it later."
        confirmLabel="Leave space"
        cancelLabel="Stay"
        onConfirm={() => navigate("/dashboard")}
        onCancel={() => setShowExit(false)}
      />
    </div>
  );
}
