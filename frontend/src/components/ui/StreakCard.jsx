import { Link } from "react-router-dom";
import Button from "./Button";
import Icon from "./Icon";

// Day streak shown the same way as the landing page: a progress ring with a
// flame in the center and the count + "day streak" beside it. Children (e.g. the
// dashboard quick stats) render below, with a bottom action button.
export default function StreakCard({ streak = 0, percent = 0, to = "/analytics", actionLabel = "See analytics", className = "", children }) {
  const size = 104;
  const stroke = 10;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.max(0, Math.min(100, percent));
  const offset = circumference * (1 - clamped / 100);

  return (
    <div className={`rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5 ${className}`}>
      <div className="flex items-center gap-4">
        <div className="relative shrink-0" style={{ width: size, height: size }}>
          <svg width={size} height={size} className="-rotate-90">
            <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="var(--surface-2)" strokeWidth={stroke} />
            <circle
              cx={size / 2}
              cy={size / 2}
              r={radius}
              fill="none"
              stroke="var(--accent)"
              strokeWidth={stroke}
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={offset}
              style={{ transition: "stroke-dashoffset 0.6s ease" }}
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <Icon name="local_fire_department" size={34} fill={1} className="text-[var(--accent)]" />
          </div>
        </div>
        <div>
          <p className="text-4xl font-extrabold leading-none text-[var(--text)]">{streak}</p>
          <p className="mt-1.5 text-[11px] font-extrabold uppercase tracking-wide text-[var(--text-muted)]">day streak</p>
        </div>
      </div>

      {/* Merged content (e.g. quick stats) */}
      {children && <div className="mt-5 border-t-2 border-[var(--border)] pt-4">{children}</div>}

      {/* Bottom action */}
      <Link to={to} className="mt-5 block">
        <Button variant="secondary" className="w-full gap-2">
          <Icon name="analytics" size={18} /> {actionLabel}
        </Button>
      </Link>
    </div>
  );
}
