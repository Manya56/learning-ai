export default function Input({ label, error, ...props }) {
  return (
    <label className="mb-3 block">
      <span className="mb-1 block text-sm text-[var(--text-muted)]">{label}</span>
      <input
        className="w-full rounded-md border border-[var(--border)] bg-[var(--surface-2)] p-2 text-[var(--text)]"
        {...props}
      />
      {error ? <span className="mt-1 block text-xs text-[var(--error)]">{error}</span> : null}
    </label>
  );
}
