import { Suspense } from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";
import Icon from "../ui/Icon";
import Spinner from "../ui/Spinner";

const mobileNav = [
  { to: "/dashboard", label: "Home", icon: "home" },
  { to: "/roadmap", label: "Roadmap", icon: "map" },
  { to: "/analytics", label: "Analytics", icon: "analytics" },
  { to: "/leaderboard", label: "Board", icon: "leaderboard" },
  { to: "/profile", label: "Profile", icon: "person" },
];

export default function AppShell() {
  const location = useLocation();
  const title = location.pathname.replace("/", "") || "dashboard";
  return (
    <div className="min-h-screen bg-[var(--bg)] text-[var(--text)]">
      <Sidebar />
      <main className="pb-24 md:ml-60 md:pb-6">
        <TopBar title={title.charAt(0).toUpperCase() + title.slice(1)} />
        <div className="p-4 md:p-6">
          <Suspense fallback={<div className="flex min-h-[60vh] items-center justify-center"><Spinner size={32} /></div>}>
            <Outlet />
          </Suspense>
        </div>
      </main>
      <nav
        className="fixed bottom-0 left-0 right-0 z-30 grid grid-cols-5 border-t-2 border-[var(--border)] bg-[var(--surface)] px-1 pt-1.5 md:hidden"
        style={{ paddingBottom: "max(0.375rem, env(safe-area-inset-bottom))" }}
      >
        {mobileNav.map((item) => {
          const active = location.pathname.startsWith(item.to);
          return (
            <Link
              key={item.to}
              to={item.to}
              className={`flex flex-col items-center gap-0.5 rounded-xl py-1 text-[10px] font-bold ${
                active ? "text-[var(--accent)]" : "text-[var(--text-muted)]"
              }`}
            >
              <Icon name={item.icon} size={24} fill={active ? 1 : 0} />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
