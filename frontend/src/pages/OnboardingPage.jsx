import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { unwrap } from "../api/axios";
import { getProfileApi } from "../api/profile";
import { useProfileStore } from "../store/profileStore";
import Button from "../components/ui/Button";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import AnimatedRoadmap from "../components/ui/AnimatedRoadmap";

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

  return (
    <div className={containerClass}>
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
            <div className="text-center mb-8">
              <h2 className="text-4xl font-bold bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
                {loading ? "Building your personalized roadmap..." : "Your Learning Adventure Begins! 🚀"}
              </h2>
              {!loading && (
                <p className="mt-3 text-lg text-slate-600 dark:text-slate-400">
                  Here's your customized learning path with {roadmap?.topics?.length || 0} exciting topics to master
                </p>
              )}
            </div>

            {error ? (
              <div className="mb-6 rounded-2xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950/30 p-4">
                <p className="text-red-600 dark:text-red-400">{error}</p>
              </div>
            ) : null}

            {roadmap?.topics?.length ? (
              <>
                {/* Hero Section */}
                <div className="mb-8 rounded-3xl bg-gradient-to-br from-indigo-500 via-purple-600 to-pink-600 p-8 text-white shadow-2xl">
                  <div className="text-center">
                    <div className="mb-4 text-6xl">🎯</div>
                    <h3 className="text-2xl font-bold mb-2">Ready to Start Your Journey?</h3>
                    <p className="text-indigo-100">
                      Your personalized roadmap is designed just for you. Each topic builds upon the last,
                      creating a smooth learning experience.
                    </p>
                  </div>
                </div>

                {/* Animated Roadmap with Topics and Subtopics */}
                <Card className="rounded-3xl p-8 mb-8 bg-gradient-to-br from-slate-50 via-white to-slate-50 dark:from-slate-900 dark:via-slate-800 dark:to-slate-900 border-2 border-slate-200/50 dark:border-slate-700/50">
                  <div className="text-center mb-6">
                    <h3 className="text-2xl font-bold text-slate-900 dark:text-white mb-2">Your Learning Roadmap</h3>
                    <p className="text-slate-600 dark:text-slate-400">Topics and concepts you'll master along the way</p>
                  </div>

                  <AnimatedRoadmap
                    topics={roadmap.topics.map((t) => ({
                      topicName: t.topicName,
                      status: "UNLOCKED",
                      progressPercent: 0,
                      concepts: t.concepts || [],
                      completedConcepts: []
                    }))}
                    currentProgress={0}
                    showStatus={false}
                    showProgress={false}
                    onboardingMode={true}
                  />
                </Card>

                {/* Call to Action */}
                <div className="text-center">
                  <Button
                    onClick={startLearning}
                    disabled={navigating}
                    className="px-12 py-4 text-lg font-semibold bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white rounded-2xl shadow-xl hover:shadow-2xl transform hover:scale-105 transition-all duration-200"
                  >
                    {navigating ? (
                      <div className="flex items-center gap-3">
                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        Opening Your Dashboard...
                      </div>
                    ) : (
                      <div className="flex items-center gap-3">
                        🚀 Start Learning Now
                      </div>
                    )}
                  </Button>

                  <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
                    Don't worry, you can always review your roadmap later in your dashboard
                  </p>
                </div>
              </>
            ) : !loading ? (
              <div className="text-center py-12">
                <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-slate-200 dark:bg-slate-700 animate-pulse"></div>
                <p className="text-slate-600 dark:text-slate-400">Preparing your learning path...</p>
              </div>
            ) : null}
          </>
        )}
      </Card>
    </div>
  );
}
