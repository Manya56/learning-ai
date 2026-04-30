import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginApi } from "../api/auth";
import { getProfileApi } from "../api/profile";
import { useAuthStore } from "../store/authStore";
import { useProfileStore } from "../store/profileStore";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const setProfile = useProfileStore((s) => s.setProfile);
  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
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
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <h2 className="mb-4 text-xl font-semibold">Sign in</h2>
        {error ? <p className="mb-3 rounded bg-red-500/10 p-2 text-sm text-red-400">{error}</p> : null}
        <form onSubmit={onSubmit}>
          <Input label="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <Input
            label="Password"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
          <Button className="mt-2 w-full" disabled={loading}>
            {loading ? "Signing in..." : "Sign in"}
          </Button>
        </form>
        <Link to="/register" className="mt-3 block text-sm text-[var(--text-muted)]">
          Need an account? Register
        </Link>
      </Card>
    </div>
  );
}
