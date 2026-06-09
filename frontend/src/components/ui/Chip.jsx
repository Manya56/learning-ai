// Shared selectable/suggestion chip (pill). `active` highlights it with the accent.
export default function Chip({ active = false, className = "", ...props }) {
  return (
    <button
      type="button"
      className={`rounded-full border-2 px-3.5 py-1.5 text-sm font-bold transition-colors active:scale-95 ${
        active
          ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--accent-hover)]"
          : "border-[var(--border)] bg-[var(--surface-2)] text-[var(--text-muted)] hover:border-[var(--accent)] hover:text-[var(--accent-hover)]"
      } ${className}`}
      {...props}
    />
  );
}
