import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { unwrap } from "../api/axios";
import { getProfileApi } from "../api/profile";
import { useProfileStore } from "../store/profileStore";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Textarea from "../components/ui/Textarea";
import Chip from "../components/ui/Chip";
import Icon from "../components/ui/Icon";
import Spinner from "../components/ui/Spinner";
import RoadmapPath from "../components/ui/RoadmapPath";

const goals = ["DSA", "Finance", "Social Media Marketing", "Music Theory", "Python", "Cooking"];
const priorLevels = ["BEGINNER", "SOME_KNOWLEDGE", "INTERMEDIATE"];

export default function OnboardingPage() {
  const navigate = useNavigate();
  const setProfile = useProfileStore((s) => s.setProfile);
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    goal: "",
    goalDescription: "",
    preferredLanguage: "English",
    priorKnowledgeLevel: 1,
  });
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState([]);
  const [index, setIndex] = useState(0);
  const [roadmap, setRoadmap] = useState(null);
  const [error, setError] = useState("");
  const [navigating, setNavigating] = useState(false);

  const currentQuestion = questions[index];
  const containerClass = step === 4 ? "mx-auto max-w-6xl p-4" : "mx-auto max-w-3xl p-4";

  useEffect(() => {
    if (step !== 4 || roadmap) return;
    (async () => {
      setLoading(true);
      setError("");
      const payload = {
        onboarding: form,
        quizAnswers: {
          goal: form.goal,
          answers,
          correctAnswers: questions.map((q) => q.correctAnswerIndex),
        },
      };
      try {
        const onboardingData = await api.post("/api/onboarding/complete", payload).then(unwrap);
        const liveRoadmap = await api.get("/api/roadmap").then(unwrap).catch(() => null);
        setRoadmap(liveRoadmap || onboardingData);
      } catch (err) {
        setError(err?.response?.data?.message || "Could not build roadmap. Try again.");
      } finally {
        setLoading(false);
      }
    })();
  }, [step, roadmap, form, answers, questions]);

  const stepLabel = useMemo(() => `Step ${step} of 4`, [step]);

  const startAssessment = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await api.post("/api/onboarding/quiz", form).then(unwrap);
      const incomingQuestions = Array.isArray(data) ? data : data?.questions || [];
      setQuestions(incomingQuestions);
      setAnswers(new Array(incomingQuestions.length).fill(null));
      setStep(3);
    } catch (err) {
      setError(err?.response?.data?.message || "Could not load quiz. Try again.");
    } finally {
      setLoading(false);
    }
  };

  const startLearning = async () => {
    setNavigating(true);
    try {
      const profile = await getProfileApi();
      setProfile(profile);
    } catch {
      // If profile fetch fails here, route guard will handle fallback.
    } finally {
      navigate("/dashboard");
      setNavigating(false);
    }
  };

  const errorCallout = error ? (
    <div className="mb-4 flex items-start gap-2 rounded-2xl border-2 border-[var(--error)]/40 bg-[var(--error)]/5 p-3">
      <Icon name="error" size={18} className="mt-0.5 text-[var(--error)]" />
      <p className="text-sm font-medium text-[var(--error)]">{error}</p>
    </div>
  ) : null;

  return (
    <div className={containerClass}>
      <Card>
        {/* Step indicator — a progress bar that fills as you advance */}
        <div className="mb-5">
          <div className="mb-2 flex items-center justify-between">
            <p className="text-[10px] font-extrabold uppercase tracking-widest text-[var(--accent-hover)]">{stepLabel}</p>
            <p className="text-[10px] font-extrabold uppercase tracking-widest tabular-nums text-[var(--text-muted)]">
              {Math.round((step / 4) * 100)}%
            </p>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-[var(--surface-2)]">
            <div
              className="h-full rounded-full bg-[var(--accent)] transition-all duration-500 ease-out"
              style={{ width: `${(step / 4) * 100}%` }}
            />
          </div>
        </div>

        {step === 1 && (
          <>
            <h2 className="mb-4 text-2xl font-extrabold tracking-tight">What do you want to learn?</h2>
            <Input label="Goal" value={form.goal} onChange={(e) => setForm({ ...form, goal: e.target.value })} />
            <div className="mb-4 flex flex-wrap gap-2">
              {goals.map((goal) => (
                <Chip key={goal} active={form.goal === goal} onClick={() => setForm({ ...form, goal })}>
                  {goal}
                </Chip>
              ))}
            </div>
            <div className="mb-4">
              <Textarea
                label="Goal description"
                className="min-h-24 resize-y"
                placeholder="Tell us a bit more about your goal…"
                value={form.goalDescription}
                onChange={(e) => setForm({ ...form, goalDescription: e.target.value })}
              />
            </div>
            <Button className="w-full gap-1.5" onClick={() => setStep(2)}>
              Continue <Icon name="arrow_forward" size={18} />
            </Button>
          </>
        )}

        {step === 2 && (
          <>
            <h2 className="mb-4 text-2xl font-extrabold tracking-tight">How much do you already know?</h2>
            {errorCallout}
            <div className="mb-4 grid gap-2">
              {priorLevels.map((level, idx) => (
                <button
                  key={level}
                  type="button"
                  onClick={() => setForm({ ...form, priorKnowledgeLevel: idx + 1 })}
                  className={`rounded-2xl border-2 p-4 text-left font-bold capitalize transition-colors active:scale-[0.99] ${
                    form.priorKnowledgeLevel === idx + 1
                      ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
                      : "border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)] hover:text-[var(--text)]"
                  }`}
                >
                  {level.replace(/_/g, " ").toLowerCase()}
                </button>
              ))}
            </div>
            <Button className="w-full gap-1.5" onClick={startAssessment} disabled={loading}>
              {loading ? (
                <>
                  <Icon name="progress_activity" size={18} className="animate-spin" /> Loading quiz…
                </>
              ) : (
                <>
                  Continue <Icon name="arrow_forward" size={18} />
                </>
              )}
            </Button>
          </>
        )}

        {step === 3 && currentQuestion && (
          <>
            <h2 className="mb-1 text-xl font-extrabold tracking-tight">Quick assessment</h2>
            <p className="mb-4 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">
              Question {index + 1} of {questions.length}
            </p>
            <p className="mb-4 font-bold text-[var(--text)]">{currentQuestion.question}</p>
            <div className="space-y-2">
              {currentQuestion.options.map((opt, i) => (
                <button
                  key={opt}
                  type="button"
                  onClick={() => {
                    const next = [...answers];
                    next[index] = i;
                    setAnswers(next);
                  }}
                  className={`block w-full rounded-2xl border-2 p-3.5 text-left font-bold transition-colors active:scale-[0.99] ${
                    answers[index] === i
                      ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
                      : "border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)] hover:text-[var(--text)]"
                  }`}
                >
                  {opt}
                </button>
              ))}
            </div>
            <Button
              className="mt-4 w-full gap-1.5"
              disabled={answers[index] == null}
              onClick={() => (index < questions.length - 1 ? setIndex(index + 1) : setStep(4))}
            >
              {index < questions.length - 1 ? (
                <>
                  Next question <Icon name="arrow_forward" size={18} />
                </>
              ) : (
                <>
                  <Icon name="map" size={18} /> Generate roadmap
                </>
              )}
            </Button>
          </>
        )}
        {step === 3 && !currentQuestion && !loading ? (
          <p className="text-sm font-medium text-[var(--error)]">Quiz questions were not available. Please go back and try again.</p>
        ) : null}

        {step === 4 && (
          <>
            <div className="mb-6 text-center">
              <h2 className="text-2xl font-extrabold tracking-tight">
                {loading ? (
                  "Building your personalized roadmap…"
                ) : (
                  <span className="inline-flex items-center gap-2">
                    Your learning adventure begins! <Icon name="rocket_launch" size={28} className="text-[var(--accent)]" />
                  </span>
                )}
              </h2>
              {!loading && (
                <p className="mt-2 text-sm font-medium text-[var(--text-muted)]">
                  Here's your path with {roadmap?.topics?.length || 0} topics to master.
                </p>
              )}
            </div>

            {errorCallout}

            {loading ? (
              <div className="py-12">
                <Spinner size={36} label="Preparing your learning path…" />
              </div>
            ) : roadmap?.topics?.length ? (
              <>
                {/* Hero */}
                <div className="mb-6 rounded-2xl border-2 border-[var(--accent)] bg-[var(--accent-light)] p-6 text-center">
                  <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--accent)] text-white">
                    <Icon name="target" size={30} fill={1} />
                  </div>
                  <h3 className="text-lg font-extrabold tracking-tight">Ready to start your journey?</h3>
                  <p className="mt-1 text-sm font-medium text-[var(--text-muted)]">
                    Each topic builds on the last for a smooth learning experience.
                  </p>
                </div>

                {/* Roadmap */}
                <Card className="mb-6">
                  <div className="mb-4 text-center">
                    <h3 className="text-lg font-extrabold tracking-tight">Your learning roadmap</h3>
                    <p className="text-sm font-medium text-[var(--text-muted)]">Topics and concepts you'll master along the way</p>
                  </div>
                  <RoadmapPath
                    topics={roadmap.topics.map((t) => ({
                      topicName: t.topicName,
                      status: "UNLOCKED",
                      progressPercent: 0,
                      concepts: t.concepts || [],
                      completedConcepts: [],
                    }))}
                  />
                </Card>

                {/* CTA */}
                <div className="text-center">
                  <Button onClick={startLearning} disabled={navigating} className="w-full gap-2 sm:w-auto">
                    {navigating ? (
                      <>
                        <Icon name="progress_activity" size={18} className="animate-spin" /> Opening your dashboard…
                      </>
                    ) : (
                      <>
                        <Icon name="rocket_launch" size={18} fill={1} /> Start learning now
                      </>
                    )}
                  </Button>
                  <p className="mt-3 text-sm font-medium text-[var(--text-muted)]">
                    You can always review your roadmap later in your dashboard.
                  </p>
                </div>
              </>
            ) : null}
          </>
        )}
      </Card>
    </div>
  );
}
