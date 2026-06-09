import { useEffect } from "react";
import { useLocation } from "react-router-dom";

// Resets scroll to the top on every route change (SPA scroll restoration).
export default function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" });
  }, [pathname]);
  return null;
}
