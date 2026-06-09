import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { forgotPasswordApi, resetPasswordApi } from "../api/auth";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import AuthShell from "../components/layout/AuthShell";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const hasToken = Boolean(token);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [fieldError, setFieldError] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [loading, setLoading] = useState(false);

  const requestReset = async (e) => {
    e.preventDefault();
    setError("");
    setInfo("");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setFieldError("Enter a valid email address.");
      return;
    }
    setFieldError("");
    setLoading(true);
    try {
      await forgotPasswordApi({ email });
      setInfo("If an account exists for that email, a reset link is on its way.");
    } catch (err) {
      setError(err?.response?.data?.message || "Could not send the reset link. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const setNewPassword = async (e) => {
    e.preventDefault();
    setError("");
    setInfo("");
    if (password.length < 8) {
      setFieldError("Password must be at least 8 characters.");
      return;
    }
    if (password !== confirm) {
      setFieldError("Passwords do not match.");
      return;
    }
    setFieldError("");
    setLoading(true);
    try {
      await resetPasswordApi({ token, newPassword: password });
      setInfo("Password updated! Redirecting to sign in…");
      setTimeout(() => navigate("/login"), 1200);
    } catch (err) {
      setError(err?.response?.data?.message || "Could not reset your password. The link may have expired.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      footer={
        <>
          Remembered it? <Link to="/login" className="font-bold text-[var(--accent-hover)] hover:underline">Sign in</Link>
        </>
      }
    >
      <Card className="w-full">
        <div className="mb-6">
          <p className="text-xs font-extrabold uppercase tracking-wide text-[var(--text-muted)]">
            {hasToken ? "New password" : "Forgot password"}
          </p>
          <h2 className="mt-2 text-2xl font-extrabold tracking-tight text-[var(--text)]">
            {hasToken ? "Set a new password" : "Get a reset link"}
          </h2>
        </div>

        {info ? (
          <p className="mb-4 rounded-2xl border-2 border-[var(--accent)]/30 bg-[var(--accent-light)] px-4 py-3 text-sm font-bold text-[var(--accent-hover)]">{info}</p>
        ) : null}
        {error ? (
          <p className="mb-4 rounded-2xl border-2 border-[var(--error)]/30 bg-[var(--error)]/10 px-4 py-3 text-sm font-bold text-[var(--error)]">{error}</p>
        ) : null}

        {hasToken ? (
          <form onSubmit={setNewPassword} noValidate>
            <Input
              label="New password"
              type="password"
              autoComplete="new-password"
              value={password}
              error={fieldError}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Input
              label="Confirm password"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
            <Button className="w-full py-3 text-base" disabled={loading}>
              {loading ? "Updating..." : "Update password"}
            </Button>
          </form>
        ) : (
          <form onSubmit={requestReset} noValidate>
            <Input
              label="Email"
              type="email"
              autoComplete="email"
              value={email}
              error={fieldError}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Button className="w-full py-3 text-base" disabled={loading}>
              {loading ? "Sending..." : "Send reset link"}
            </Button>
          </form>
        )}
      </Card>
    </AuthShell>
  );
}
