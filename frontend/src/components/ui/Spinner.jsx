import Icon from "./Icon";

// Shared loading spinner used for inline loads and Suspense fallbacks.
export default function Spinner({ size = 28, label, className = "" }) {
  return (
    <div className={`flex flex-col items-center justify-center gap-2 text-[var(--text-muted)] ${className}`}>
      <Icon name="progress_activity" size={size} className="animate-spin text-[var(--accent)]" />
      {label ? <p className="text-sm font-medium">{label}</p> : null}
    </div>
  );
}
