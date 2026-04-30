export default function Card({ className = "", children }) {
  return <div className={`surface-card p-4 ${className}`}>{children}</div>;
}
