import { Link } from "react-router-dom";
import Button from "../ui/Button";
import Logo from "../ui/Logo";

// Shared top navbar for all public pages (landing + auth).
export default function PublicNav() {
  return (
    <header className="sticky top-0 z-30 border-b-2 border-[var(--border)] bg-[var(--surface)]">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <Link to="/">
          <Logo className="text-xl" />
        </Link>
        <div className="flex items-center gap-2">
          <Link to="/login">
            <Button variant="ghost" className="px-4 py-2">Sign in</Button>
          </Link>
          <Link to="/register">
            <Button className="px-4 py-2">Get started</Button>
          </Link>
        </div>
      </div>
    </header>
  );
}
