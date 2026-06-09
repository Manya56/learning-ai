import PublicNav from "./PublicNav";

// Single-column, navbar-topped layout for auth pages. No gradients.
// (eyebrow/title/subtitle/highlights are accepted but the form card carries its own heading.)
export default function AuthShell({ footer, children }) {
  return (
    <div className="flex min-h-screen flex-col bg-[var(--surface)]">
      <PublicNav />
      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-md">
          {children}
          {footer ? <div className="mt-4 text-center text-sm font-medium text-[var(--text-muted)]">{footer}</div> : null}
        </div>
      </main>
    </div>
  );
}
