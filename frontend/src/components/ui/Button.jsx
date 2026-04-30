export default function Button({ variant = "primary", className = "", ...props }) {
  const variants = {
    primary: "bg-[var(--accent)] hover:bg-[var(--accent-hover)] text-white",
    secondary: "bg-[var(--surface-2)] text-[var(--text)]",
    ghost: "bg-transparent text-[var(--text)] border border-[var(--border)]",
    danger: "bg-[var(--error)] text-white",
  };
  return (
    <button
      className={`rounded-lg px-4 py-2 font-medium transition disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    />
  );
}
