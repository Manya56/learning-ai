import { Link } from "react-router-dom";
import { Brain, Map, Target } from "lucide-react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

export default function LandingPage() {
  return (
    <div className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,#1b1f3a_0%,#0f0f0f_46%,#0b0b0b_100%)] px-4 py-8 text-[var(--text)]">
      <div className="pointer-events-none absolute inset-0 opacity-60">
        <div className="absolute left-[-8rem] top-20 h-72 w-72 rounded-full bg-[var(--accent)]/15 blur-3xl" />
        <div className="absolute right-[-6rem] top-36 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl" />
        <div className="absolute bottom-0 left-1/2 h-56 w-[40rem] -translate-x-1/2 rounded-full bg-[var(--accent)]/10 blur-3xl" />
      </div>

      <div className="relative mx-auto flex min-h-[calc(100vh-4rem)] max-w-6xl flex-col justify-center">
        <div className="mx-auto max-w-4xl text-center">
          <div className="mx-auto mb-5 inline-flex items-center rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-[var(--text-muted)] shadow-lg shadow-black/10 backdrop-blur">
            Personalized learning paths, quizzes, and practice in one place
          </div>

          <h1 className="text-5xl font-semibold tracking-tight text-white sm:text-6xl lg:text-7xl">
            LearnAI
          </h1>

          <div className="mt-5 flex justify-center overflow-hidden rounded-full border border-[var(--border)] bg-[var(--surface)]/70 px-4 py-3 shadow-[0_0_40px_rgba(99,102,241,0.12)] backdrop-blur">
            <div className="typewriter-line text-lg font-medium text-white sm:text-xl">
              learn anything from DSA to Dance
            </div>
          </div>

          <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-[var(--text-muted)] sm:text-lg">
            AI-powered roadmaps, quizzes, and practice problems for any topic.
            Build momentum faster with a clear path, guided feedback, and daily
            progress that stays easy to follow.
          </p>

          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link to="/register">
              <Button className="min-w-40 px-6 py-3 text-base">Get Started</Button>
            </Link>
            <Link to="/login">
              <Button variant="ghost" className="min-w-40 px-6 py-3 text-base">
                Sign In
              </Button>
            </Link>
          </div>
        </div>

        <div className="mt-14 grid gap-4 md:grid-cols-3">
          {[
            {
              icon: Map,
              label: "Plan",
              title: "Personalized Roadmap",
              copy: "A guided plan that adapts to your current level and target outcome.",
            },
            {
              icon: Target,
              label: "Test",
              title: "Adaptive Quizzes",
              copy: "Short checkpoints that adjust difficulty and reinforce weak spots.",
            },
            {
              icon: Brain,
              label: "Ask",
              title: "AI Mentor Aria",
              copy: "Ask questions, get explanations, and keep moving without losing context.",
            },
          ].map((feature) => (
            <Card
              key={feature.title}
              className="border-white/8 bg-white/[0.03] p-5 shadow-[0_20px_60px_rgba(0,0,0,0.18)] backdrop-blur"
            >
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-[var(--accent)]/15 text-white ring-1 ring-[var(--accent)]/20">
                <feature.icon className="h-6 w-6" />
              </div>
              <h3 className="text-lg font-semibold text-white">{feature.title}</h3>
              <p className="mt-1 text-xs uppercase tracking-[0.22em] text-[var(--text-muted)]">
                {feature.label}
              </p>
              <p className="mt-2 text-sm leading-6 text-[var(--text-muted)]">
                {feature.copy}
              </p>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
