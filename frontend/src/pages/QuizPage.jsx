import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { completeQuizApi, startQuizApi, submitQuizAnswerApi, getQuizHistoryApi, getQuizHintApi } from "../api/quiz";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import { useUiStore } from "../store/uiStore";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";
import { motion } from "framer-motion";

export default function QuizPage() {
  const [params, setParams] = useSearchParams();
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
  const [session, setSession] = useState(null);
  const [index, setIndex] = useState(0);
  const [selected, setSelected] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [currentTopic, setCurrentTopic] = useState(null);
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopicName, setSelectedTopicName] = useState("");
  const [revealed, setRevealed] = useState(false);
  const [showLeavePrompt, setShowLeavePrompt] = useState(false);
  const [pendingHref, setPendingHref] = useState("");
  const [questionStartedAt, setQuestionStartedAt] = useState(Date.now());
  const [elapsed, setElapsed] = useState(0);
  const [starting, setStarting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [finalizing, setFinalizing] = useState(false);
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [hints, setHints] = useState({});
  const [loadingHint, setLoadingHint] = useState(false);
  const setActiveQuizSession = useUiStore((s) => s.setActiveQuizSession);
  const clearQuizSession = useUiStore((s) => s.clearQuizSession);
  const activeQuizSessionId = useUiStore((s) => s.activeQuizSessionId);
  const conceptPool = getConceptPoolFromTopic(currentTopic);
  const conceptNames = getSelectableConceptNames(conceptPool);
  const topicOptions = getSelectableTopics(roadmap?.topics || []);
  const selectedTopic = topicOptions.find((t) => t.topicName === selectedTopicName) || currentTopic;
  const selectedConceptPool = selectedTopic ? getConceptPoolFromTopic(selectedTopic) : conceptPool;
  const selectedConceptNames = getSelectableConceptNames(selectedConceptPool);

  const question = useMemo(() => session?.questions?.[index], [session, index]);

  useEffect(() => {
    getCurrentTopicApi().then(setCurrentTopic).catch(() => setCurrentTopic({ concepts: [] }));
    getRoadmapApi().then(setRoadmap).catch(() => setRoadmap({ topics: [] }));
  }, []);

  useEffect(() => {
    if (!selectedTopicName && currentTopic?.topicName) setSelectedTopicName(currentTopic.topicName);
  }, [currentTopic, selectedTopicName]);

  useEffect(() => {
    if (!showHistory) return;
    if (history.length) return;
    loadHistory();
  }, [showHistory]);

  const loadHistory = async () => {
    setLoadingHistory(true);
    try {
      const data = await getQuizHistoryApi();
      setHistory(data || []);
    } catch (err) {
      console.error("Failed to load quiz history:", err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const getHint = async (hintNumber = 1) => {
    if (!session || loadingHint) return;

    setLoadingHint(true);
    try {
      const hint = await getQuizHintApi(session.sessionId, index, hintNumber);
      setHints(prev => ({
        ...prev,
        [`${index}-${hintNumber}`]: hint.hint
      }));
    } catch (err) {
      console.error("Failed to get hint:", err);
    } finally {
      setLoadingHint(false);
    }
  };

  useEffect(() => {
    if (!selectedTopic) return;
    if (!topic && selectedTopic?.topicName) setTopic(selectedTopic.topicName);
    if (!selectedConceptNames.length) return;
    if (!concept || !selectedConceptNames.includes(concept)) {
      setConcept(selectedTopic?.nextConcept || getPreferredConcept(selectedConceptPool));
    }
  }, [selectedTopic, concept, topic, selectedConceptNames, selectedConceptPool]);

  useEffect(() => {
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      if (concept) next.set("concept", concept);
      if (topic) next.set("topic", topic);
      return next;
    }, { replace: true });
  }, [concept, topic, setParams]);

  useEffect(() => {
    if (!session) return;
    const timer = setInterval(() => setElapsed(Date.now() - questionStartedAt), 250);
    return () => clearInterval(timer);
  }, [session, questionStartedAt]);

  useEffect(() => {
    const onBeforeUnload = (e) => {
      if (!session) return;
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [session]);

  const start = async () => {
    setStarting(true);
    setError("");
    try {
      const data = await startQuizApi({ conceptName: concept, topicGoal: topic });
      setSession(data);
      setActiveQuizSession(data.sessionId);
      sessionStorage.setItem("activeQuizSessionId", data.sessionId);
      setIndex(0);
      setSelected(null);
      setRevealed(false);
      setQuestionStartedAt(Date.now());
    } catch (err) {
      setError(err?.response?.data?.message || "Could not start quiz. Check concept/topic and try again.");
    } finally {
      setStarting(false);
    }
  };

  const submit = async () => {
    setSubmitting(true);
    try {
      const answer = await submitQuizAnswerApi(session.sessionId, {
        questionIndex: index,
        selectedAnswerIndex: selected,
        timeTakenMs: Math.max(500, elapsed),
      });
      setRevealed(answer);
      setError("");
    } catch (err) {
      setError(err?.response?.data?.message || "Could not submit answer. Try again.");
    } finally {
      setSubmitting(false);
      setSubmitting(false);
    }
  };

  const abandon = () => {
    setSession(null);
    setSelected(null);
    setResult(null);
    setRevealed(false);
    clearQuizSession();
    sessionStorage.removeItem("activeQuizSessionId");
    setShowLeavePrompt(false);
  };

  const formatMs = (ms) => {
    const sec = Math.floor(ms / 1000);
    const m = String(Math.floor(sec / 60)).padStart(2, "0");
    const s = String(sec % 60).padStart(2, "0");
    return `${m}:${s}`;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("en-US", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
    } catch {
      return dateString;
    }
  };

  const calculateStats = () => {
    if (!history || !history.length) {
      return { totalAttempts: 0, avgAccuracy: 0, avgTime: 0, passRate: 0 };
    }
    const totalAttempts = history.length;
    const avgAccuracy = history.reduce((sum, h) => sum + (h.totalCorrect || 0), 0) / totalAttempts / Math.max(1, history[0]?.totalQuestions || 1) * 100;
    const avgTime = history.reduce((sum, h) => sum + (h.timeTakenMs || 0), 0) / totalAttempts;
    const passCount = history.filter(h => (h.totalCorrect / (h.totalQuestions || 1)) >= 0.6).length;
    const passRate = (passCount / totalAttempts) * 100;
    return { totalAttempts, avgAccuracy: Math.round(avgAccuracy), avgTime, passRate: Math.round(passRate) };
  };

  const stats = calculateStats();

  if (result) {
    return (
      <Card>
        <h3 className="mb-2 text-2xl font-semibold">Score: {result.totalCorrect}/{result.totalQuestions}</h3>
        <p className="mb-2 text-xs text-[var(--text-muted)]">Accuracy: {(result.accuracyPercent ?? 0).toFixed(2)}%</p>
        <p className="text-sm text-[var(--text-muted)]">{result.roadmapMessage || "Keep practicing."}</p>
        <div className="mt-3 flex gap-2">
          <Link to={`/practice?concept=${encodeURIComponent(concept)}&topic=${encodeURIComponent(topic)}`}>
            <Button>Practice This Concept</Button>
          </Link>
          <Link to="/roadmap/current"><Button variant="ghost">Back to Roadmap</Button></Link>
        </div>
      </Card>
    );
  }

  if (!session) {
    return (
      <div className="space-y-4">
        <Card>
          <h3 className="mb-3 font-semibold">Quiz: {concept || "No concept selected"}</h3>
          <p className="mb-4 text-sm text-[var(--text-muted)]">Topic: {topic}</p>
          {topicOptions.length ? (
            <div className="mb-3">
              <p className="mb-2 text-sm text-[var(--text-muted)]">Choose topic</p>
              <div className="flex flex-wrap gap-2">
                {topicOptions.map((t) => (
                  <button
                    key={t.id || t.topicName}
                    onClick={() => {
                      setSelectedTopicName(t.topicName);
                      setTopic(t.topicName);
                    }}
                    className={`rounded-full border px-3 py-1.5 text-xs ${
                      topic === t.topicName ? "border-[var(--accent)] bg-[var(--accent-light)]" : "border-[var(--border)] bg-[var(--surface-2)]"
                    }`}
                  >
                    {t.topicName}
                  </button>
                ))}
              </div>
            </div>
          ) : null}
          {selectedConceptNames.length ? (
            <div className="mb-3">
              <p className="mb-2 text-sm text-[var(--text-muted)]">Select concept</p>
              <div className="flex flex-wrap gap-2">
                {selectedConceptNames.map((name) => (
                  <button
                    key={name}
                    onClick={() => setConcept(name)}
                    className={`rounded-full border px-3 py-1.5 text-xs transition ${
                      concept === name
                        ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
                        : "border-[var(--border)] bg-[var(--surface-2)] text-[var(--text-muted)] hover:text-[var(--text)]"
                    }`}
                  >
                    {name}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <p className="mb-3 text-xs text-[var(--text-muted)]">No unlocked concepts in this topic yet.</p>
          )}
          {activeQuizSessionId ? (
            <p className="mb-3 rounded-lg bg-[var(--warning)]/10 p-2 text-xs text-[var(--warning)]">
              You have an active session in this browser. Continue it or abandon.
            </p>
          ) : null}
          {error ? <p className="mb-3 text-sm text-red-400">{error}</p> : null}
          <Button disabled={!concept || !topic || starting} onClick={start}>
            {starting ? "Starting..." : "Start Quiz"}
          </Button>
        </Card>

        {/* History & Stats Section */}
        <Card>
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="mb-3 flex w-full items-center justify-between text-left"
          >
            <h3 className="font-semibold">History & Stats</h3>
            <span className="text-sm text-[var(--text-muted)]">{showHistory ? "▼" : "▶"}</span>
          </button>

          {showHistory && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} transition={{ duration: 0.2 }} className="space-y-3">
              {/* Stats Cards */}
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                <div className="rounded-lg bg-blue-50 p-3 text-center">
                  <p className="text-xs text-[var(--text-muted)]">Attempts</p>
                  <p className="text-xl font-bold text-blue-600">{stats.totalAttempts}</p>
                </div>
                <div className="rounded-lg bg-green-50 p-3 text-center">
                  <p className="text-xs text-[var(--text-muted)]">Avg Accuracy</p>
                  <p className="text-xl font-bold text-green-600">{stats.avgAccuracy.toFixed(2)}%</p>
                </div>
                <div className="rounded-lg bg-purple-50 p-3 text-center">
                  <p className="text-xs text-[var(--text-muted)]">Pass Rate</p>
                  <p className="text-xl font-bold text-purple-600">{stats.passRate}%</p>
                </div>
                <div className="rounded-lg bg-orange-50 p-3 text-center">
                  <p className="text-xs text-[var(--text-muted)]">Avg Time</p>
                  <p className="text-xl font-bold text-orange-600">{formatMs(stats.avgTime)}</p>
                </div>
              </div>

              {/* History List */}
              {loadingHistory ? (
                <p className="py-4 text-center text-sm text-[var(--text-muted)]">Loading history...</p>
              ) : history.length === 0 ? (
                <p className="py-4 text-center text-sm text-[var(--text-muted)]">No quiz history yet. Start a quiz to see your progress!</p>
              ) : (
                <div className="space-y-2 max-h-64 overflow-y-auto">
                  {history.slice(0, 10).map((quiz, idx) => (
                    <motion.div
                      key={idx}
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.2, delay: idx * 0.05 }}
                      className="rounded-lg border border-[var(--border)] bg-[var(--surface-2)] p-3 transition-colors"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="font-semibold text-sm">{quiz.conceptName}</p>
                          <p className="text-xs text-[var(--text-muted)]">{formatDate(quiz.startedAt)}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-lg font-bold">{quiz.totalCorrect}/{quiz.totalQuestions}</p>
                          <p className={`text-xs font-semibold ${(quiz.totalCorrect / quiz.totalQuestions) >= 0.6 ? "text-green-600" : "text-red-600"}`}>
                            {Math.round((quiz.totalCorrect / quiz.totalQuestions) * 100)}%
                          </p>
                        </div>
                      </div>
                      <div className="mt-2 flex flex-wrap gap-2 text-xs text-[var(--text-muted)]">
                        <span>⏱️ {formatMs(quiz.timeTakenMs || 0)}</span>
                        <span>📊 {quiz.difficulty || "Unknown"}</span>
                        <span className="rounded-full bg-[var(--surface)] px-2 py-1 text-[var(--text-muted)]">Session {quiz.sessionId?.slice(-6) || "-"}</span>
                      </div>
                    </motion.div>
                  ))}
                </div>
              )}
            </motion.div>
          )}
        </Card>
      </div>
    );
  }

  if (!question) {
    return (
      <Card>
        <p className="text-sm text-[var(--text-muted)]">Loading question...</p>
      </Card>
    );
  }

  return (
    <div className="relative">
      <Card>
      {showLeavePrompt ? (
        <div className="mb-3 rounded-lg border border-[var(--border)] bg-[var(--surface-2)] p-3">
          <p className="mb-2 text-sm">You have an active quiz session. Continue or abandon?</p>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => setShowLeavePrompt(false)}>Continue</Button>
            <Button
              variant="danger"
              onClick={() => {
                abandon();
                if (pendingHref) window.location.href = pendingHref;
              }}
            >
              Abandon
            </Button>
          </div>
        </div>
      ) : null}
      <p className="mb-2 text-sm text-[var(--text-muted)]">Question {index + 1} / {session.totalQuestions || session.questions.length} • {formatMs(elapsed)}</p>
      <h3 className="mb-3 text-lg">{question.question}</h3>

      {/* Hints Section */}
      <div className="mb-4">
        <div className="flex gap-2 mb-2">
          <Button
            variant="outline"
            size="sm"
            disabled={loadingHint || revealed}
            onClick={() => getHint(1)}
          >
            💡 Hint 1
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={loadingHint || revealed || !hints[`${index}-1`]}
            onClick={() => getHint(2)}
          >
            💡 Hint 2
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={loadingHint || revealed || !hints[`${index}-2`]}
            onClick={() => getHint(3)}
          >
            💡 Hint 3
          </Button>
        </div>
        {loadingHint && <p className="text-sm text-[var(--text-muted)]">Loading hint...</p>}
        {Object.entries(hints).filter(([key]) => key.startsWith(`${index}-`)).map(([key, hint]) => (
          <div key={key} className="bg-blue-50 border border-blue-200 rounded p-2 mb-2">
            <p className="text-sm text-blue-800">{hint}</p>
          </div>
        ))}
      </div>

      <div className="space-y-2">
        {(question.options || []).map((opt, i) => {
          const isCorrect = revealed?.correctIndex === i;
          const isSelected = selected === i;
          const optionClass = revealed
            ? isCorrect
              ? "border-green-400 bg-green-50 text-green-900"
              : isSelected
                ? "border-red-400 bg-red-50 text-red-900"
                : "border-[var(--border)] bg-[var(--surface-2)] text-[var(--text)]"
            : isSelected
              ? "border-[var(--accent)] text-[var(--text)]"
              : "border-[var(--border)] text-[var(--text)] hover:bg-[var(--surface-2)]";

          return (
            <button
              key={opt}
              disabled={revealed || submitting}
              onClick={() => !revealed && !submitting && setSelected(i)}
              className={`block w-full rounded-lg border p-2 text-left ${optionClass}`}
            >
              {opt}
            </button>
          );
        })}
      </div>
      {!revealed ? (
        <Button className="mt-3" disabled={selected == null || submitting} onClick={submit}>
          {submitting ? "Submitting..." : "Submit Answer"}
        </Button>
      ) : (
        <div className="mt-3 space-y-2">
          <p className="text-sm">{revealed.explanation}</p>
          {revealed.correct === false ? (
            <p className="text-sm text-[var(--warning)]">
              Correct answer: {(question.options || [])[revealed.correctIndex] ?? "N/A"}
            </p>
          ) : null}
          <Button
            disabled={finalizing}
            onClick={async () => {
              setSelected(null);
              setRevealed(false);
              if (index < session.questions.length - 1) {
                setIndex(index + 1);
                setQuestionStartedAt(Date.now());
              }
              else {
                setFinalizing(true);
                try {
                  const final = await completeQuizApi(session.sessionId);
                  clearQuizSession();
                  sessionStorage.removeItem("activeQuizSessionId");
                  setResult(final);
                  setError("");
                } catch (err) {
                  setError(err?.response?.data?.message || "Could not complete quiz. Try again.");
                } finally {
                  setFinalizing(false);
                }
              }
            }}
          >
            {finalizing ? "Finishing..." : index < session.questions.length - 1 ? "Next Question" : "See Results"}
          </Button>
        </div>
      )}
      <div className="mt-3">
        {error ? <p className="mb-2 text-sm text-red-400">{error}</p> : null}
        <button
          className="text-xs text-[var(--text-muted)] underline"
          onClick={() => {
            setPendingHref("/dashboard");
            setShowLeavePrompt(true);
          }}
        >
          Leave quiz
        </button>
      </div>
    </Card>
  </div>
  );
}
