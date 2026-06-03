export default function Input({ label, error, className = "", ...props }) {
  return (
    <label className="mb-3 block">
      <span className="mb-2 block text-xs font-medium uppercase tracking-[0.18em] text-[var(--text-muted)]">{label}</span>
      <input
        className={`w-full rounded-xl border border-white/10 bg-white/[0.04] px-4 py-3 text-[var(--text)] placeholder:text-white/30 transition-colors duration-200 outline-none focus:border-[var(--accent)]/60 focus:bg-white/[0.06] focus:ring-2 focus:ring-[var(--accent)]/20 ${error ? "border-[var(--error)]/70 focus:border-[var(--error)] focus:ring-[var(--error)]/20" : ""} ${className}`}
        {...props}
      />
      {error ? <span className="mt-2 block text-xs text-[var(--error)]">{error}</span> : null}
    </label>
  );
}
