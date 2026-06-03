import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginApi } from "../api/auth";
import { getProfileApi } from "../api/profile";
import { useAuthStore } from "../store/authStore";
import { useProfileStore } from "../store/profileStore";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import AuthShell from "../components/layout/AuthShell";
import { BookOpen, LayoutDashboard, ShieldCheck } from "lucide-react";

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const setProfile = useProfileStore((s) => s.setProfile);
  const [form, setForm] = useState({ email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const validateForm = () => {
    const nextErrors = {};
    if (!form.email.trim()) {
      nextErrors.email = "Email is required.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      nextErrors.email = "Enter a valid email address.";
    }
    if (!form.password) {
      nextErrors.password = "Password is required.";
    }
    setFieldErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      const auth = await loginApi(form);
      login(auth);
      try {
        const profile = await getProfileApi();
        setProfile(profile);
        navigate("/dashboard");
      } catch (profileError) {
        if (profileError?.response?.status === 404) {
          navigate("/onboarding");
        } else {
          throw profileError;
        }
      }
    } catch {
      setError("Something went wrong. Try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      eyebrow="LearnAI"
      title="Welcome back"
      subtitle="Pick up where you left off with the same clean, focused experience from the homepage."
      highlights={[
        {
          icon: LayoutDashboard,
          title: "Fast access",
          copy: "Jump directly back into your roadmap, quiz history, and mentor sessions.",
        },
        {
          icon: ShieldCheck,
          title: "Clear feedback",
          copy: "Validation and loading states now match the rest of the app.",
        },
        {
          icon: BookOpen,
          title: "Consistent design",
          copy: "The same glassy surfaces and accent-driven buttons used on the homepage.",
        },
      ]}
      footer={
        <>
          Don’t have an account? <Link to="/register" className="text-(--accent) hover:underline">Create one</Link>
        </>
      }
    >
      <Card className="w-full">
        <div className="mb-6">
          <p className="text-xs font-medium uppercase tracking-[0.22em] text-(--text-muted)">Sign in</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white">Access your learning dashboard</h2>
          <p className="mt-2 text-sm leading-6 text-(--text-muted)">Use your LearnAI account to continue studying, reviewing, and practicing.</p>
        </div>

        {error ? (
          <p className="mb-4 rounded-2xl border border-(--error)/30 bg-(--error)/10 px-4 py-3 text-sm text-red-200">
            {error}
          </p>
        ) : null}

        <form onSubmit={onSubmit} noValidate>
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            value={form.email}
            error={fieldErrors.email}
            onChange={(e) => {
              setForm({ ...form, email: e.target.value });
              if (fieldErrors.email) {
                setFieldErrors((current) => ({ ...current, email: undefined }));
              }
            }}
          />
          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            error={fieldErrors.password}
            onChange={(e) => {
              setForm({ ...form, password: e.target.value });
              if (fieldErrors.password) {
                setFieldErrors((current) => ({ ...current, password: undefined }));
              }
            }}
          />
          <Button className="mt-1 w-full py-3 text-base" disabled={loading}>
            {loading ? "Signing in..." : "Sign in"}
          </Button>
        </form>

        <Link to="/register" className="mt-5 block text-center text-sm text-(--text-muted) hover:text-white">
          Need an account? <span className="text-(--accent)">Register</span>
        </Link>
      </Card>
    </AuthShell>
  );
}
