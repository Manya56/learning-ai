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
    } catch (err) {
    console.error("Login Error:", err); 
    const message = err.response?.data?.message || "Invalid email or password. Please try again.";
    setError(message);
  } finally {
    setLoading(false);
  }
};


  return (
    <AuthShell
      footer={
        <>
          Don’t have an account? <Link to="/register" className="font-bold text-[var(--accent-hover)] hover:underline">Create one</Link>
        </>
      }
    >
      <Card className="w-full">
        <div className="mb-6">
          <p className="text-xs font-extrabold uppercase tracking-wide text-[var(--text-muted)]">Sign in</p>
          <h2 className="mt-2 text-2xl font-extrabold tracking-tight text-[var(--text)]">Access your learning dashboard</h2>
          <p className="mt-2 text-sm font-medium leading-6 text-[var(--text-muted)]">Use your LearnAI account to continue studying, reviewing, and practicing.</p>
        </div>

        {error ? (
          <p className="mb-4 rounded-2xl border-2 border-[var(--error)]/30 bg-[var(--error)]/10 px-4 py-3 text-sm font-bold text-[var(--error)]">
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
          <div className="mb-3 text-right">
            <Link to="/reset-password" className="text-xs font-bold text-[var(--accent-hover)] hover:underline">
              Forgot password?
            </Link>
          </div>
          <Button className="w-full py-3 text-base" disabled={loading}>
            {loading ? "Signing in..." : "Sign in"}
          </Button>
        </form>
      </Card>
    </AuthShell>
  );
}
