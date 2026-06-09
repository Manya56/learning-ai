export default function Card({ className = "", children }) {
  return (
    <div className={`rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5 ${className}`}>
      {children}
    </div>
  );
}
