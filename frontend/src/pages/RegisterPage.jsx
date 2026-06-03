import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import emailjs from "@emailjs/browser";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import AuthShell from "../components/layout/AuthShell";
import { BookOpen, Layers3, ShieldCheck } from "lucide-react";
import { generateOtpFromEmailAndTime } from "../utils/otp";

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

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

  const validateForm = () => {
    const nextErrors = {};
    if (!form.fullName.trim()) {
      nextErrors.fullName = "Full name is required.";
    }
    if (!form.email.trim()) {
      nextErrors.email = "Email is required.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      nextErrors.email = "Enter a valid email address.";
    }
    if (!form.password) {
      nextErrors.password = "Password is required.";
    } else if (form.password.length < 8) {
      nextErrors.password = "Password must be at least 8 characters.";
    }
    setFieldErrors(nextErrors);
    return Object.keys(nextErrors).length ? "Please check the highlighted fields." : "";
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const generatedAtTime = new Date().toTimeString().slice(0, 8);
      const otp = generateOtpFromEmailAndTime(form.email, generatedAtTime);

      const { serviceId, templateId, publicKey, missing } = getEmailJsConfig();

      if (missing.length) {
        throw new Error(`EmailJS is not configured. Missing: ${missing.join(", ")}. Add them in .env and restart app.`);
      }

      await emailjs.send(
        serviceId,
        templateId,
        {
          // EmailJS OTP template expects these exact keys.
          passcode: otp,
          time: "10 minutes",
          email: form.email,
          to_email: form.email,
          user_name: form.fullName,
          otp_code: otp,
          app_name: "Personalized Learning",
        },
        { publicKey }
      );

      navigate("/register/verify-otp", {
        state: {
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          generatedAtTime,
        },
      });
    } catch (err) {
      const emailJsReason = err?.text ? ` (${err.text})` : "";
      setError(err?.response?.data?.message || err?.message || `Could not send OTP. Please try again${emailJsReason}.`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      eyebrow="LearnAI"
      title="Create your account"
      subtitle="Set up your profile once, then follow a learning experience that stays consistent from landing page to lesson."
      highlights={[
        {
          icon: Layers3,
          title: "Personal roadmap",
          copy: "Get a study path tailored to your goals, starting level, and pace.",
        },
        {
          icon: ShieldCheck,
          title: "Better onboarding",
          copy: "Improved field validation helps registration feel clearer and faster.",
        },
        {
          icon: BookOpen,
          title: "Unified styling",
          copy: "The same surfaces, spacing, and accent colors carry through the app.",
        },
      ]}
      footer={
        <>
          Already have an account? <Link to="/login" className="text-(--accent) hover:underline">Sign in</Link>
        </>
      }
    >
      <Card className="w-full">
        <div className="mb-6">
          <p className="text-xs font-medium uppercase tracking-[0.22em] text-(--text-muted)">Sign up</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-tight text-white">Start your personalized learning flow</h2>
          <p className="mt-2 text-sm leading-6 text-(--text-muted)">Create an account to generate your roadmap, verify your email, and unlock the full experience.</p>
        </div>

        {error ? (
          <p className="mb-4 rounded-2xl border border-(--error)/30 bg-(--error)/10 px-4 py-3 text-sm text-red-200">
            {error}
          </p>
        ) : null}

        <form onSubmit={onSubmit} noValidate>
          <Input
            label="Full Name"
            autoComplete="name"
            value={form.fullName}
            error={fieldErrors.fullName}
            onChange={(e) => {
              setForm({ ...form, fullName: e.target.value });
              if (fieldErrors.fullName) {
                setFieldErrors((current) => ({ ...current, fullName: undefined }));
              }
            }}
          />
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
            autoComplete="new-password"
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
            {loading ? "Sending OTP..." : "Create account"}
          </Button>
        </form>

      </Card>
    </AuthShell>
  );
}
