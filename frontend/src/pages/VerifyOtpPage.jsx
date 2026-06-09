import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import emailjs from "@emailjs/browser";
import { registerApi } from "../api/auth";
import { useAuthStore } from "../store/authStore";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import AuthShell from "../components/layout/AuthShell";
import { generateOtpFromEmailAndTime } from "../utils/otp";

export default function VerifyOtpPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((s) => s.login);
  const [otp, setOtp] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  const payload = useMemo(() => location.state || {}, [location.state]);
  const { fullName, email, password, generatedAtTime } = payload;
  const [activeGeneratedAtTime, setActiveGeneratedAtTime] = useState(generatedAtTime || "");
  const hasRegistrationState = Boolean(fullName && email && password && activeGeneratedAtTime);

  const getEmailJsConfig = () => {
    const serviceId = import.meta.env.VITE_EMAILJS_SERVICE_ID;
    const templateId = import.meta.env.VITE_EMAILJS_TEMPLATE_ID;
    const publicKey = import.meta.env.VITE_EMAILJS_PUBLIC_KEY;
    const missing = [
      !serviceId ? "VITE_EMAILJS_SERVICE_ID" : null,
      !templateId ? "VITE_EMAILJS_TEMPLATE_ID" : null,
      !publicKey ? "VITE_EMAILJS_PUBLIC_KEY" : null,
    ].filter(Boolean);

    return { serviceId, templateId, publicKey, missing };
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!hasRegistrationState) {
      setError("Registration session expired. Please register again.");
      return;
    }

    const expectedOtp = generateOtpFromEmailAndTime(email, activeGeneratedAtTime);
    const normalizedOtp = otp.replace(/\s+/g, "");
    if (normalizedOtp !== expectedOtp) {
      setError("Invalid OTP. Please check your email and try again.");
      return;
    }

    setLoading(true);
    try {
      const auth = await registerApi({ fullName, email, password });
      login(auth);
      navigate("/onboarding");
    } catch (err) {
      setError(err?.response?.data?.message || "Could not complete registration. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const onResendOtp = async () => {
    setError("");
    setInfo("");

    if (!fullName || !email) {
      setError("Registration session expired. Please register again.");
      return;
    }

    const { serviceId, templateId, publicKey, missing } = getEmailJsConfig();
    if (missing.length) {
      setError(`EmailJS is not configured. Missing: ${missing.join(", ")}. Add them in .env and restart app.`);
      return;
    }

    setResendLoading(true);
    try {
      // Always generate OTP from fresh current time for resend.
      const latestGeneratedAtTime = new Date().toTimeString().slice(0, 8);
      const latestOtp = generateOtpFromEmailAndTime(email, latestGeneratedAtTime);

      await emailjs.send(
        serviceId,
        templateId,
        {
          // EmailJS OTP template expects these exact keys.
          passcode: latestOtp,
          time: "10 minutes",
          email,
          to_email: email,
          user_name: fullName,
          otp_code: latestOtp,
          app_name: "Personalized Learning",
        },
        { publicKey }
      );

      setActiveGeneratedAtTime(latestGeneratedAtTime);
      setOtp("");
      setInfo("A new OTP has been sent to your email.");
    } catch (err) {
      const emailJsReason = err?.text ? ` (${err.text})` : "";
      setError(err?.response?.data?.message || err?.message || `Could not resend OTP. Please try again${emailJsReason}.`);
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <AuthShell
      footer={
        <>
          Wrong email? <Link to="/register" className="font-bold text-[var(--accent-hover)] hover:underline">Back to register</Link>
        </>
      }
    >
      <Card className="w-full">
        <div className="mb-6">
          <p className="text-xs font-extrabold uppercase tracking-wide text-[var(--text-muted)]">One-time code</p>
          <h2 className="mt-2 text-2xl font-extrabold tracking-tight text-[var(--text)]">Enter your OTP</h2>
          <p className="mt-2 text-sm font-medium leading-6 text-[var(--text-muted)]">
            Sent to <span className="font-bold text-[var(--text)]">{email || "your email"}</span>.
          </p>
        </div>

        {info ? (
          <p className="mb-4 rounded-2xl border-2 border-[var(--accent)]/30 bg-[var(--accent-light)] px-4 py-3 text-sm font-bold text-[var(--accent-hover)]">{info}</p>
        ) : null}
        {error ? (
          <p className="mb-4 rounded-2xl border-2 border-[var(--error)]/30 bg-[var(--error)]/10 px-4 py-3 text-sm font-bold text-[var(--error)]">{error}</p>
        ) : null}

        <form onSubmit={onSubmit}>
          <Input
            label="6-digit code"
            value={otp}
            maxLength={6}
            inputMode="numeric"
            autoComplete="one-time-code"
            className="text-center text-lg tracking-[0.5em]"
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
          />
          <Button className="w-full py-3 text-base" disabled={loading}>
            {loading ? "Verifying..." : "Verify OTP"}
          </Button>
        </form>

        <Button variant="ghost" className="mt-3 w-full" onClick={onResendOtp} disabled={resendLoading}>
          {resendLoading ? "Resending..." : "Resend OTP"}
        </Button>
      </Card>
    </AuthShell>
  );
}
