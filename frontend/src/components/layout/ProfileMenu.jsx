import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { logoutApi } from "../../api/auth";
import { useAuthStore } from "../../store/authStore";
import { useProfileStore } from "../../store/profileStore";
import Icon from "../ui/Icon";

const getInitials = (name = "") =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || "")
    .join("") || "?";

const menu = [{ to: "/profile", label: "Profile", icon: "person" }];

export default function ProfileMenu() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const profile = useProfileStore((s) => s.profile);
  const logout = useAuthStore((s) => s.logout);
  const avatar = profile?.avatarUrl || profile?.photoUrl || profile?.picture || profile?.image;

  const handleLogout = async () => {
    setOpen(false);
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
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label="Open profile menu"
        className="flex h-9 w-9 items-center justify-center overflow-hidden rounded-full border-2 border-[var(--border)] bg-[var(--surface-2)] text-sm font-extrabold text-[var(--text)] transition-colors hover:border-[var(--accent)]"
      >
        {avatar ? <img src={avatar} alt="" className="h-full w-full object-cover" /> : getInitials(profile?.fullName)}
      </button>

      {open && (
        <>
          {/* click-outside backdrop */}
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] shadow-[0_16px_40px_rgba(0,0,0,0.12)]">
            <div className="border-b-2 border-[var(--border)] px-4 py-3">
              <p className="truncate text-sm font-extrabold text-[var(--text)]">{profile?.fullName || "Your account"}</p>
              {profile?.email && <p className="truncate text-xs font-medium text-[var(--text-muted)]">{profile.email}</p>}
            </div>
            <div className="py-1">
              {menu.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  onClick={() => setOpen(false)}
                  className="flex items-center gap-3 px-4 py-2 text-sm font-bold text-[var(--text)] transition-colors hover:bg-[var(--surface-2)]"
                >
                  <Icon name={item.icon} size={18} className="text-[var(--text-muted)]" />
                  {item.label}
                </Link>
              ))}
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="flex w-full items-center gap-3 border-t-2 border-[var(--border)] px-4 py-2.5 text-sm font-bold text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
            >
              <Icon name="logout" size={18} /> Logout
            </button>
          </div>
        </>
      )}
    </div>
  );
}
