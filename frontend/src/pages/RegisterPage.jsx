import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import emailjs from "@emailjs/browser";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import { generateOtpFromEmailAndTime } from "../utils/otp";

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
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
    if (!form.fullName.trim() || !form.email.trim() || !form.password) {
      return "Please fill in full name, email, and password.";
    }
    if (form.password.length < 8) {
      return "Password must be at least 8 characters.";
    }
    return "";
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
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <h2 className="mb-4 text-xl font-semibold">Create account</h2>
        {error ? <p className="mb-3 rounded bg-red-500/10 p-2 text-sm text-red-400">{error}</p> : null}
        <form onSubmit={onSubmit}>
          <Input label="Full Name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          <Input label="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <Input
            label="Password"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
          <Button className="mt-2 w-full" disabled={loading}>
            {loading ? "Sending OTP..." : "Create account"}
          </Button>
        </form>
        <Link to="/login" className="mt-3 block text-sm text-[var(--text-muted)]">
          Already have an account? Sign in
        </Link>
      </Card>
    </div>
  );
}
