// Shared multiline field, matching <Input> styling.
export default function Textarea({ label, error, className = "", ...props }) {
  return (
    <label className="block">
      {label && (
        <span className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">{label}</span>
      )}
      <textarea
        className={`w-full rounded-xl border-2 bg-[var(--surface)] px-4 py-3 font-medium text-[var(--text)] outline-none transition-colors placeholder:text-[var(--text-muted)] focus:ring-2 focus:ring-[var(--accent)]/20 ${
          error
            ? "border-[var(--error)] focus:border-[var(--error)] focus:ring-[var(--error)]/20"
            : "border-[var(--border)] focus:border-[var(--accent)]"
        } ${className}`}
        {...props}
      />
      {error ? <span className="mt-1.5 block text-xs font-bold text-[var(--error)]">{error}</span> : null}
    </label>
  );
}
