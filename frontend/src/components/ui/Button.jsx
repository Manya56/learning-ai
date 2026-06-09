export default function Button({ variant = "primary", className = "", ...props }) {
  const variants = {
    // Single action color. Chunky 3D underside that compresses on press (Duolingo feel).
    primary:
      "bg-[var(--accent)] text-white shadow-[0_4px_0_0_var(--accent-hover)] hover:brightness-[1.03] active:translate-y-[3px] active:shadow-[0_1px_0_0_var(--accent-hover)]",
    secondary:
      "bg-[var(--surface-2)] text-[var(--text)] shadow-[0_4px_0_0_var(--border)] hover:brightness-[0.99] active:translate-y-[3px] active:shadow-[0_1px_0_0_var(--border)]",
    ghost:
      "border-2 border-[var(--border)] bg-transparent text-[var(--text)] hover:bg-[var(--surface-2)]",
    // "Mirror" — translucent, frosted, single-color tint.
    glass:
      "border border-[var(--accent)]/25 bg-[var(--accent)]/10 text-[var(--accent-hover)] backdrop-blur-md hover:bg-[var(--accent)]/15 active:translate-y-[1px]",
    danger:
      "bg-[var(--error)] text-white shadow-[0_4px_0_0_var(--error-hover)] hover:brightness-[1.03] active:translate-y-[3px] active:shadow-[0_1px_0_0_var(--error-hover)]",
  };
  return (
    <button
      className={`inline-flex cursor-pointer select-none items-center justify-center rounded-2xl px-5 py-2.5 text-sm font-bold tracking-wide transition-all duration-100 ease-out disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent)]/40 focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--bg)] ${variants[variant]} ${className}`}
      {...props}
    />
  );
}
