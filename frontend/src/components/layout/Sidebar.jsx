import { Link, useLocation } from "react-router-dom";
import { LayoutDashboard, Map, BookOpen, Brain, User, CircleHelp, Code2, RefreshCcw, ChartNoAxesCombined } from "lucide-react";
import { useAuthStore } from "../../store/authStore";
import { useProfileStore } from "../../store/profileStore";

const items = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/roadmap", label: "Roadmap", icon: Map },
  { to: "/roadmap/current", label: "Current Topic", icon: Map },
  { to: "/learn", label: "Learn", icon: BookOpen },
  { to: "/quiz", label: "Quiz", icon: CircleHelp },
  { to: "/practice", label: "Practice", icon: Code2 },
  { to: "/mentor", label: "Mentor", icon: Brain },
  { to: "/revision", label: "Revision", icon: RefreshCcw },
  { to: "/analytics", label: "Analytics", icon: ChartNoAxesCombined },
  { to: "/profile", label: "Profile", icon: User },
];

export default function Sidebar() {
  const location = useLocation();
  const logout = useAuthStore((s) => s.logout);
  const revisionDueCount = useProfileStore((s) => s.revisionDueCount);
  return (
    <aside className="hidden w-60 border-r border-[var(--border)] bg-[var(--surface)] p-4 md:block">
      <h1 className="mb-6 text-xl font-semibold">LearnAI</h1>
      <nav className="space-y-1">
        {items.map((item) => {
          const Icon = item.icon;
          const active = location.pathname.startsWith(item.to);
          return (
            <Link
              key={item.to}
              to={item.to}
              className={`flex items-center gap-2 rounded-lg px-3 py-2 ${
                active ? "bg-[var(--accent-light)] text-[var(--text)]" : "text-[var(--text-muted)]"
              }`}
            >
              <Icon size={16} /> {item.label}
              {item.to === "/revision" && revisionDueCount > 0 ? (
                <span className="ml-auto rounded-full bg-red-500 px-2 py-0.5 text-[10px] text-white">
                  {revisionDueCount}
                </span>
              ) : null}
            </Link>
          );
        })}
      </nav>
      <button className="mt-8 text-sm text-[var(--error)]" onClick={logout}>
        Logout
      </button>
    </aside>
  );
}
