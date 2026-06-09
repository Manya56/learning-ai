// The app wordmark — identical everywhere (matches the dashboard sidebar).
export default function Logo({ className = "" }) {
  return <span className={`font-extrabold tracking-tight text-[var(--text)] ${className}`}>LearnAI</span>;
}
