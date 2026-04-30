import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { unwrap } from "../api/axios";
import { getProfileApi } from "../api/profile";
import { useProfileStore } from "../store/profileStore";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";

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
  const topicsToShow = roadmap?.topics?.length
    ? roadmap.topics.map((topic) => topic?.topicName || topic).filter(Boolean)
    : roadmap?.roadmapTopics?.length
      ? roadmap.roadmapTopics
      : [];

  const currentQuestion = questions[index];

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

  return (
    <div className="mx-auto max-w-3xl p-4">
      <Card>
        <p className="mb-4 text-sm text-[var(--text-muted)]">{stepLabel}</p>
        {step === 1 && (
          <>
            <h2 className="mb-4 text-2xl font-semibold">What do you want to learn?</h2>
            <Input label="Goal" value={form.goal} onChange={(e) => setForm({ ...form, goal: e.target.value })} />
            <div className="mb-3 flex flex-wrap gap-2">
              {goals.map((goal) => (
                <button key={goal} className="rounded-full bg-[var(--surface-2)] px-3 py-1 text-xs" onClick={() => setForm({ ...form, goal })}>
                  {goal}
                </button>
              ))}
            </div>
            <label className="mb-3 block">
              <span className="mb-1 block text-sm text-[var(--text-muted)]">Goal Description</span>
              <textarea
                className="w-full rounded-md border border-[var(--border)] bg-[var(--surface-2)] p-2"
                value={form.goalDescription}
                onChange={(e) => setForm({ ...form, goalDescription: e.target.value })}
              />
            </label>
            <Button onClick={() => setStep(2)}>Continue</Button>
          </>
        )}
        {step === 2 && (
          <>
            <h2 className="mb-4 text-2xl font-semibold">How much do you already know?</h2>
            {error ? <p className="mb-3 rounded bg-red-500/10 p-2 text-sm text-red-400">{error}</p> : null}
            <div className="mb-4 grid gap-2">
              {priorLevels.map((level, idx) => (
                <button
                  key={level}
                  className={`rounded-lg border p-3 text-left ${form.priorKnowledgeLevel === idx + 1 ? "border-[var(--accent)]" : "border-[var(--border)]"}`}
                  onClick={() => setForm({ ...form, priorKnowledgeLevel: idx + 1 })}
                >
                  {level}
                </button>
              ))}
            </div>
            <Button onClick={startAssessment} disabled={loading}>
              {loading ? "Loading quiz..." : "Continue"}
            </Button>
          </>
        )}
        {step === 3 && currentQuestion && (
          <>
            <h2 className="mb-3 text-xl font-semibold">Quick Assessment</h2>
            <p className="mb-2 text-sm text-[var(--text-muted)]">
              Question {index + 1} of {questions.length}
            </p>
            <p className="mb-3">{currentQuestion.question}</p>
            <div className="space-y-2">
              {currentQuestion.options.map((opt, i) => (
                <button
                  key={opt}
                  className={`block w-full rounded-lg border p-2 text-left ${answers[index] === i ? "border-[var(--accent)] bg-[var(--accent-light)]" : "border-[var(--border)]"}`}
                  onClick={() => {
                    const next = [...answers];
                    next[index] = i;
                    setAnswers(next);
                  }}
                >
                  {opt}
                </button>
              ))}
            </div>
            <Button
              className="mt-3"
              disabled={answers[index] == null}
              onClick={() => (index < questions.length - 1 ? setIndex(index + 1) : setStep(4))}
            >
              {index < questions.length - 1 ? "Next Question" : "Generate Roadmap"}
            </Button>
          </>
        )}
        {step === 3 && !currentQuestion && !loading ? (
          <p className="text-sm text-red-400">Quiz questions were not available. Please go back and try again.</p>
        ) : null}
        {step === 4 && (
          <>
            <h2 className="mb-4 text-2xl font-semibold">{loading ? "Building your personalized roadmap..." : "Your roadmap is ready!"}</h2>
            {error ? <p className="mb-3 rounded bg-red-500/10 p-2 text-sm text-red-400">{error}</p> : null}
            {topicsToShow.length ? (
              <>
                <div className="mb-4 grid gap-2">
                  {topicsToShow.map((topicName, idx) => (
                    <div key={`${topicName}-${idx}`} className="rounded-xl border border-[var(--border)] bg-[var(--surface-2)] p-3">
                      <p className="text-xs text-[var(--text-muted)]">Topic {idx + 1}</p>
                      <p className="font-medium">{topicName}</p>
                    </div>
                  ))}
                </div>
                <Button onClick={startLearning} disabled={navigating}>
                  {navigating ? "Opening dashboard..." : "Start Learning"}
                </Button>
              </>
            ) : (
              <div className="h-24 animate-pulse rounded bg-[var(--surface-2)]" />
            )}
          </>
        )}
      </Card>
    </div>
  );
}
