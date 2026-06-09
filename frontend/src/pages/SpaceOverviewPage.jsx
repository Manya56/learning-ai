import { useState } from "react";
import { Link, useOutletContext } from "react-router-dom";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import { pickConceptName } from "../utils/study";

export default function SpaceOverviewPage() {
  const ctx = useOutletContext() || {};
  const { topic, topicName, concept, query } = ctx;
  const concepts = (topic?.concepts || []).map(pickConceptName).filter(Boolean);
  const completed = new Set(topic?.completedConcepts || []);
  const [showAll, setShowAll] = useState(false);

  const total = concepts.length;
  const doneCount = completed.size;
  const activeIndex = concepts.findIndex((c) => c === concept);
  const allDone = total > 0 && doneCount >= total;
  const pct = total ? Math.round((doneCount / total) * 100) : 0;
  const lockedChapter = (topic?.status || "").toUpperCase() === "LOCKED";

  if (lockedChapter) {
    return (
      <div className="flex min-h-[62vh] flex-col items-center justify-center gap-4 py-6 text-center">
        <div className="flex h-24 w-24 items-center justify-center rounded-[2rem] bg-[var(--surface-2)] text-[var(--text-muted)]">
          <Icon name="lock" size={44} />
        </div>
        <div>
          <p className="text-[11px] font-extrabold uppercase tracking-widest text-[var(--text-muted)]">Locked</p>
          <h2 className="mt-1 text-2xl font-extrabold tracking-tight">{topicName}</h2>
        </div>
        <p className="max-w-xs text-sm font-medium text-[var(--text-muted)]">
          Finish the previous chapter to unlock this one. Use the ◄ arrow at the top to go back.
        </p>
      </div>
    );
  }

  if (!total) {
    return <p className="py-16 text-center text-sm font-medium text-[var(--text-muted)]">No concepts in this topic yet.</p>;
  }

  return (
    <div className="flex min-h-[62vh] flex-col items-center justify-center gap-5 py-6 text-center">
      {/* One big focus node */}
      <div className="flex h-28 w-28 items-center justify-center rounded-[2.25rem] bg-[var(--accent)] text-4xl font-extrabold text-white shadow-[0_7px_0_0_var(--accent-hover)]">
        {allDone ? (
          <Icon name="emoji_events" size={52} fill={1} />
        ) : activeIndex >= 0 ? (
          activeIndex + 1
        ) : (
          <Icon name="play_arrow" size={52} fill={1} />
        )}
      </div>

      <div>
        <p className="text-[11px] font-extrabold uppercase tracking-widest text-[var(--accent-hover)]">
          {allDone ? "Topic complete" : activeIndex >= 0 ? `Concept ${activeIndex + 1} of ${total}` : "Get started"}
        </p>
        <h2 className="mt-1 text-2xl font-extrabold tracking-tight">{allDone ? topicName : concept || topicName}</h2>
      </div>

      {/* ONE big action */}
      {allDone ? (
        <p className="max-w-xs text-sm font-medium text-[var(--text-muted)]">Use the ► arrow at the top to start the next chapter.</p>
      ) : (
        <Link to={`/space/learn${query}`} className="w-full max-w-xs">
          <Button className="h-14 w-full gap-2 text-base active:scale-95">
            <Icon name="play_arrow" size={22} fill={1} /> {doneCount > 0 ? "Continue" : "Start"}
          </Button>
        </Link>
      )}

      {/* Progress */}
      <div className="w-full max-w-xs">
        <div className="h-2.5 overflow-hidden rounded-full bg-[var(--surface-2)]">
          <div className="h-full rounded-full bg-[var(--accent)] transition-all" style={{ width: `${pct}%` }} />
        </div>
        <p className="mt-2 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">{doneCount} / {total} concepts</p>
      </div>

      {/* Collapsible concept list */}
      <button
        type="button"
        onClick={() => setShowAll((v) => !v)}
        className="inline-flex items-center gap-1 text-xs font-extrabold text-[var(--accent-hover)] active:scale-95"
      >
        <Icon name={showAll ? "expand_less" : "expand_more"} size={16} /> {showAll ? "Hide concepts" : "View all concepts"}
      </button>

      {showAll && (
        <div className="w-full max-w-md space-y-2 text-left">
          {concepts.map((name, i) => {
            const state = completed.has(name) ? "done" : name === concept ? "current" : "todo";
            return (
              <div
                key={name || i}
                className={`flex items-center gap-3 rounded-2xl border-2 px-4 py-3 ${
                  state === "current" ? "border-[var(--accent)] bg-[var(--accent-light)]" : "border-[var(--border)] bg-[var(--surface)]"
                }`}
              >
                <div
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-xl text-xs font-extrabold ${
                    state === "todo" ? "bg-[var(--surface-2)] text-[var(--text-muted)]" : "bg-[var(--accent)] text-white"
                  }`}
                >
                  {state === "done" ? <Icon name="check" size={16} /> : i + 1}
                </div>
                <span className={`flex-1 truncate text-sm font-bold ${state === "todo" ? "text-[var(--text-muted)]" : "text-[var(--text)]"}`}>{name}</span>
                {state === "done" && (
                  <Link to={`/space/learn?concept=${encodeURIComponent(name)}&topic=${encodeURIComponent(topicName || "")}`} className="shrink-0 text-xs font-extrabold text-[var(--accent-hover)]">
                    Review
                  </Link>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
