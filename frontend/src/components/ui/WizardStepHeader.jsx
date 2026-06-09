import Icon from "./Icon";

const STEPS = ["learn", "quiz", "practice"];

// Persistent focus header for Space activity tabs: concept name (left),
// a Learn→Quiz→Practice segment indicator (center), and a leave control (right).
export default function WizardStepHeader({ concept, active, showSteps = true, onLeave }) {
  return (
    <div className="mb-5 flex items-center gap-3">
      <div className="min-w-0 flex-1">
        <p className="text-[10px] font-extrabold uppercase tracking-widest text-[var(--accent-hover)]">Concept</p>
        <p className="truncate text-base font-extrabold tracking-tight">{concept || "This concept"}</p>
      </div>

      {showSteps && (
        <div className="flex shrink-0 items-center gap-1">
          {STEPS.map((s) => (
            <span key={s} className={`h-1.5 w-6 rounded-full transition-colors ${s === active ? "bg-[var(--accent)]" : "bg-[var(--border)]"}`} />
          ))}
        </div>
      )}

      {onLeave && (
        <button
          type="button"
          onClick={onLeave}
          aria-label="Leave"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--surface-2)] text-[var(--text)] transition hover:bg-[var(--border)] active:scale-95"
        >
          <Icon name="close" size={18} />
        </button>
      )}
    </div>
  );
}
