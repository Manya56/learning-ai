import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { useProfileStore } from "../store/profileStore";

export default function ProtectedRoute() {
  const location = useLocation();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const profile = useProfileStore((s) => s.profile);
  const loadProfile = useProfileStore((s) => s.loadProfile);
  const [loading, setLoading] = useState(true);
  const [profileMissing, setProfileMissing] = useState(false);

  useEffect(() => {
    let mounted = true;
    const run = async () => {
      if (!isAuthenticated) {
        if (mounted) setProfileMissing(false);
        if (mounted) setLoading(false);
        return;
      }
      if (profile) {
        if (mounted) setProfileMissing(false);
        if (mounted) setLoading(false);
        return;
      }
      try {
        await loadProfile();
        if (mounted) setProfileMissing(false);
      } catch (error) {
        // Only treat a 404 as "onboarding not completed yet".
        if (mounted) setProfileMissing(error?.response?.status === 404);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    run();
    return () => {
      mounted = false;
    };
  }, [isAuthenticated, profile, loadProfile]);

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (loading) {
    return <div className="h-24 animate-pulse rounded-xl bg-[var(--surface)]" />;
  }
  if (profileMissing && location.pathname !== "/onboarding") return <Navigate to="/onboarding" replace />;
  if (!profileMissing && profile && location.pathname === "/onboarding") return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
