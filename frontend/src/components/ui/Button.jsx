export default function Button({ variant = "primary", className = "", ...props }) {
  const variants = {
    primary:
      "bg-[var(--accent)] text-white shadow-[0_12px_30px_rgba(99,102,241,0.25)] hover:bg-[var(--accent-hover)] hover:shadow-[0_16px_36px_rgba(99,102,241,0.35)]",
    secondary:
      "bg-[var(--surface-2)] text-[var(--text)] border border-white/5 hover:bg-white/10",
    ghost:
      "bg-transparent text-[var(--text)] border border-[var(--border)] hover:border-white/20 hover:bg-white/5",
    danger: "bg-[var(--error)] text-white shadow-[0_12px_30px_rgba(239,68,68,0.22)] hover:shadow-[0_16px_36px_rgba(239,68,68,0.3)]",
  };
  return (
    <button
      className={`inline-flex cursor-pointer items-center justify-center rounded-lg px-4 py-2 font-medium transition-all duration-200 ease-out hover:-translate-y-0.5 active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]/70 focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--bg)] ${variants[variant]} ${className}`}
      {...props}
    />
  );
}
