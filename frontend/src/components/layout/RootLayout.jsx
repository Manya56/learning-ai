import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import ScrollToTop from "./ScrollToTop";
import Spinner from "../ui/Spinner";

// App root: scroll restoration + a Suspense boundary for lazily-loaded routes.
export default function RootLayout() {
  return (
    <>
      <ScrollToTop />
      <Suspense
        fallback={
          <div className="flex min-h-screen items-center justify-center bg-[var(--bg)]">
            <Spinner size={36} />
          </div>
        }
      >
        <Outlet />
      </Suspense>
    </>
  );
}
