import { Link, Outlet, useLocation } from "react-router-dom";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";

const mobileNav = [
  { to: "/dashboard", label: "Home" },
  { to: "/roadmap", label: "Roadmap" },
  { to: "/learn", label: "Learn" },
  { to: "/quiz", label: "Quiz" },
  { to: "/profile", label: "Profile" },
];

export default function AppShell() {
  const location = useLocation();
  const title = location.pathname.replace("/", "") || "dashboard";
  return (
    <div className="min-h-screen bg-[var(--bg)] text-[var(--text)] md:flex">
      <Sidebar />
      <main className="flex-1 p-4 pb-20 md:p-6">
        <TopBar title={title.charAt(0).toUpperCase() + title.slice(1)} />
        <Outlet />
      </main>
      <nav className="fixed bottom-0 left-0 right-0 grid grid-cols-5 border-t border-[var(--border)] bg-[var(--surface)] p-2 md:hidden">
        {mobileNav.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            className={`rounded-lg px-2 py-1 text-center text-xs ${
              location.pathname.startsWith(item.to) ? "bg-[var(--accent-light)]" : "text-[var(--text-muted)]"
            }`}
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </div>
  );
}
