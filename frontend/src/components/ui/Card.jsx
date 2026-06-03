export default function Card({ className = "", children }) {
  return <div className={`surface-card rounded-2xl border border-white/8 bg-white/[0.04] p-5 shadow-[0_24px_80px_rgba(0,0,0,0.24)] backdrop-blur ${className}`}>{children}</div>;
}
