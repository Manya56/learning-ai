import { Link, useLocation, useNavigate } from "react-router-dom";
import Icon from "../ui/Icon";
import Logo from "../ui/Logo";
import { logoutApi } from "../../api/auth";
import { useAuthStore } from "../../store/authStore";

const items = [
  { to: "/dashboard", label: "Dashboard", icon: "dashboard" },
  { to: "/roadmap", label: "Roadmap", icon: "map" },
  { to: "/analytics", label: "Analytics", icon: "analytics" },
  { to: "/leaderboard", label: "Leaderboard", icon: "leaderboard" },
  { to: "/profile", label: "Profile", icon: "person" },
];

export default function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      // Local logout should still complete if the server session is already expired.
    } finally {
      logout();
      navigate("/login", { replace: true });
    }
  };

  return (
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r-2 border-[var(--border)] bg-[var(--surface)] p-4 md:flex">
      <h1 className="mb-6"><Logo className="text-xl" /></h1>
      <nav className="flex-1 space-y-1 overflow-y-auto">
        {items.map((item) => {
          const active = location.pathname.startsWith(item.to);
          return (
            <Link
              key={item.to}
              to={item.to}
              className={`flex items-center gap-2 rounded-lg px-3 py-2 ${
                active ? "bg-[var(--accent-light)] text-[var(--text)]" : "text-[var(--text-muted)]"
              }`}
            >
              <Icon name={item.icon} size={20} fill={active ? 1 : 0} /> {item.label}
            </Link>
          );
        })}
      </nav>
      <button
        className="mt-4 flex items-center gap-2 rounded-xl border-2 border-[var(--border)] px-3 py-2 text-sm font-bold text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
        onClick={handleLogout}
      >
        <Icon name="logout" size={18} /> Logout
      </button>
    </aside>
  );
}
