import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Icon from "../components/ui/Icon";
import Button from "../components/ui/Button";
import Logo from "../components/ui/Logo";
import PublicNav from "../components/layout/PublicNav";

const GOALS = ["anything.", "to code.", "Spanish.", "calculus.", "to dance."];

// Types a word out, holds, deletes, moves to the next — looping.
function useTypewriter(words, { typeMs = 75, deleteMs = 35, holdMs = 1500 } = {}) {
  const [index, setIndex] = useState(0);
  const [sub, setSub] = useState(0);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const word = words[index % words.length];
    const atEnd = !deleting && sub === word.length;
    const atStart = deleting && sub === 0;
    const delay = atEnd ? holdMs : deleting ? deleteMs : typeMs;
    const t = setTimeout(() => {
      if (atEnd) setDeleting(true);
      else if (atStart) {
        setDeleting(false);
        setIndex((i) => (i + 1) % words.length);
      } else setSub((s) => s + (deleting ? -1 : 1));
    }, delay);
    return () => clearTimeout(t);
  }, [sub, deleting, index, words, typeMs, deleteMs, holdMs]);

  return words[index % words.length].slice(0, sub);
}

/* ── small building blocks ─────────────────────────────── */

const Eyebrow = ({ children }) => (
  <span className="inline-flex items-center gap-2 rounded-full bg-[var(--accent-light)] px-4 py-2 text-xs font-extrabold uppercase tracking-wide text-[var(--accent-hover)]">
    {children}
  </span>
);


/* ── page-level data ───────────────────────────────────── */

const TOPICS = [
  { icon: "code", label: "Data Structures" },
  { icon: "translate", label: "Spanish" },
  { icon: "functions", label: "Calculus" },
  { icon: "music_note", label: "Music Theory" },
  { icon: "smart_toy", label: "Machine Learning" },
  { icon: "record_voice_over", label: "Public Speaking" },
  { icon: "restaurant", label: "Cooking" },
  { icon: "fitness_center", label: "Fitness" },
];

const STEPS = [
  { n: "01", icon: "flag", title: "Set a goal", copy: "Type anything, pick your level and pace." },
  { n: "02", icon: "menu_book", title: "Learn + practice", copy: "Bite-sized lessons, then real exercises." },
  { n: "03", icon: "quiz", title: "Quiz + revise", copy: "Adaptive checks and spaced repetition." },
  { n: "04", icon: "trending_up", title: "Track + level up", copy: "Streaks, analytics, and XP keep you going." },
];

const BEFORE = ["20 open tabs, no idea what's next", "Watched the tutorial, can't rebuild it", "Quizzes test what you already know", "Forgot it all two weeks later"];
const AFTER = ["One clear next step, every day", "Build real things with AI feedback", "Quizzes hunt the stuff you miss", "Spaced reviews lock it into memory"];

const FAQ = [
  { q: "Can it really teach non-technical stuff?", a: "Yes. From DSA and system design to languages, music theory, or salsa — the roadmap and quizzes generate around whatever you type." },
  { q: "How is the roadmap personalized?", a: "You set your level and pace; the AI sequences topics for you and re-orders as your quiz and practice results come in." },
  { q: "What's the AI feedback on practice like?", a: "It reviews your actual attempt, points to the specific line or step, and explains the fix — guided, not just pass/fail." },
  { q: "Is it free?", a: "Start free, build your first roadmap, and take a lesson in minutes. No credit card needed." },
];

/* ── page ──────────────────────────────────────────────── */

export default function LandingPage() {
  const typed = useTypewriter(GOALS);
  const [after, setAfter] = useState(false);
  return (
    <div className="min-h-screen bg-[var(--surface)] text-[var(--text)]">
      <PublicNav />

      {/* Hero */}
      <section className="mx-auto max-w-3xl px-4 pb-12 pt-16 text-center sm:pt-24">
        <Eyebrow>One app · every goal</Eyebrow>
        <h1 className="mt-6 text-4xl font-extrabold leading-[1.1] tracking-tight sm:text-5xl lg:text-6xl">
          Learn <span className="text-[var(--accent)]">{typed}</span>
          <span className="ml-0.5 animate-pulse font-extrabold text-[var(--accent)]">|</span>
        </h1>
        <p className="mx-auto mt-5 max-w-xl text-base font-medium leading-7 text-[var(--text-muted)] sm:text-lg">
          LearnAI builds you a personal roadmap, drills your weak spots with adaptive quizzes, reviews your code, and never lets you forget what you learned.
        </p>

        <div className="mt-8 flex justify-center">
          <Link to="/register" className="w-full sm:w-auto">
            <Button className="w-full px-7 py-3.5 text-base sm:w-auto sm:min-w-44">Start free</Button>
          </Link>
        </div>

        <p className="mt-5 text-sm font-medium text-[var(--text-muted)]">
          Already learning with us? <Link to="/login" className="font-bold text-[var(--accent-hover)] hover:underline">Sign in</Link>
        </p>
      </section>

      {/* Breadth strip */}
      <section className="border-y-2 border-[var(--border)] bg-[var(--surface-2)]/50">
        <div className="mx-auto max-w-5xl px-4 py-10 text-center">
          <p className="text-xs font-extrabold uppercase tracking-widest text-[var(--text-muted)]">From your next interview to your next hobby</p>
          <div className="mt-5 flex flex-wrap justify-center gap-2">
            {TOPICS.map((t) => (
              <span key={t.label} className="flex items-center gap-1.5 rounded-full border-2 border-[var(--border)] bg-[var(--surface)] px-3 py-1.5 text-sm font-bold">
                <Icon name={t.icon} size={16} className="text-[var(--accent)]" /> {t.label}
              </span>
            ))}
          </div>
          <p className="mt-5 text-sm font-bold text-[var(--text)]">If you can name it, LearnAI can map it.</p>
        </div>
      </section>

      {/* How it flows */}
      <section className="border-y-2 border-[var(--border)] bg-[var(--surface-2)]/50">
        <div className="mx-auto max-w-6xl px-4 py-16">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-extrabold tracking-tight">Four steps, then it runs itself</h2>
            <p className="mt-3 font-medium text-[var(--text-muted)]">And Aria is there for everything in between.</p>
          </div>
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {STEPS.map((s) => (
              <div key={s.title} className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5">
                <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-2xl bg-[var(--accent)] text-white shadow-[0_3px_0_0_var(--accent-hover)]">
                  <Icon name={s.icon} size={22} />
                </div>
                <h3 className="text-base font-extrabold tracking-tight">{s.title}</h3>
                <p className="mt-1 text-sm font-medium leading-6 text-[var(--text-muted)]">{s.copy}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Before / After — section + card backgrounds flip with the toggle */}
      <section
        className={`border-y-2 transition-colors duration-300 ${
          after ? "border-[var(--accent)] bg-[var(--accent)]" : "border-[var(--border)] bg-[var(--surface-2)]"
        }`}
      >
        <div className="mx-auto max-w-3xl px-4 py-16">
          <h2 className={`text-center text-3xl font-extrabold tracking-tight transition-colors duration-300 ${after ? "text-white" : "text-[var(--text)]"}`}>
            From confused to confident
          </h2>

          {/* Toggle — under the title, not in the card */}
          <div className="mt-6 flex items-center justify-center gap-3">
            <span className={`text-sm font-extrabold uppercase tracking-wide ${after ? "text-white/60" : "text-[var(--text)]"}`}>Before</span>
            <button
              type="button"
              role="switch"
              aria-checked={after}
              aria-label="Toggle before and after"
              onClick={() => setAfter((v) => !v)}
              className={`relative h-8 w-16 shrink-0 rounded-full border-2 transition-colors duration-300 ${
                after ? "border-white/50 bg-white/25" : "border-[var(--border)] bg-[var(--surface)]"
              }`}
            >
              <span
                className={`absolute top-1/2 h-6 w-6 -translate-y-1/2 rounded-full shadow-sm transition-all duration-300 ${
                  after ? "left-[calc(100%-1.625rem)] bg-white" : "left-[2px] bg-[var(--accent)]"
                }`}
              />
            </button>
            <span className={`text-sm font-extrabold uppercase tracking-wide ${after ? "text-white" : "text-[var(--text-muted)]"}`}>After</span>
          </div>

          {/* Card — light gray when before, white when after; keeps the smiley */}
          <div
            className={`mx-auto mt-8 max-w-lg rounded-2xl border-2 border-[var(--border)] p-6 transition-colors duration-300 ${
              after ? "bg-[var(--surface)]" : "bg-[var(--surface-2)]"
            }`}
          >
            <p className={`mb-4 flex items-center gap-2 text-sm font-extrabold uppercase tracking-wide ${after ? "text-[var(--accent-hover)]" : "text-[var(--text-muted)]"}`}>
              <Icon
                name={after ? "sentiment_very_satisfied" : "sentiment_dissatisfied"}
                size={20}
                fill={after ? 1 : 0}
                className={after ? "text-[var(--accent)]" : ""}
              />
              {after ? "After" : "Before"}
            </p>
            <ul className="space-y-2.5">
              {(after ? AFTER : BEFORE).map((b) => (
                <li key={b} className={`flex items-start gap-2 text-sm font-bold ${after ? "text-[var(--text)]" : "text-[var(--text-muted)]"}`}>
                  <Icon
                    name={after ? "check_circle" : "close"}
                    size={18}
                    fill={after ? 1 : 0}
                    className={`mt-0.5 shrink-0 ${after ? "text-[var(--accent)]" : "text-[var(--text-muted)]"}`}
                  />
                  {b}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section className="mx-auto max-w-3xl px-4 py-16">
        <h2 className="text-center text-3xl font-extrabold tracking-tight">Straight answers</h2>
        <div className="mt-8 space-y-3">
          {FAQ.map((f, i) => (
            <details key={f.q} className="group rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-4" open={i === 0}>
              <summary className="flex cursor-pointer list-none items-center justify-between gap-3 font-extrabold tracking-tight [&::-webkit-details-marker]:hidden">
                {f.q}
                <Icon name="expand_more" size={20} className="shrink-0 text-[var(--text-muted)] transition-transform group-open:rotate-180" />
              </summary>
              <p className="mt-3 text-sm font-medium leading-6 text-[var(--text-muted)]">{f.a}</p>
            </details>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t-2 border-[var(--border)]">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-4 py-6 text-sm font-medium text-[var(--text-muted)] sm:flex-row">
          <Logo className="text-lg" />
          <span>© LearnAI — learn anything, the smart way.</span>
          <div className="flex gap-4">
            <Link to="/login" className="hover:text-[var(--text)]">Sign in</Link>
            <Link to="/register" className="hover:text-[var(--text)]">Register</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
