import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import { completeQuizApi, startQuizApi, submitQuizAnswerApi, getQuizHistoryApi, getQuizHintApi, getQuizDetailsApi } from "../api/quiz";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import { useUiStore } from "../store/uiStore";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import WizardStepHeader from "../components/ui/WizardStepHeader";
import Input from "../components/ui/Input";
import Spinner from "../components/ui/Spinner";
import EmptyState from "../components/ui/EmptyState";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";
import { AnimatePresence, motion } from "framer-motion";

export default function QuizPage() {
  const [params, setParams] = useSearchParams();
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
  // Collapse the pickers by default when a concept came in via the URL (topic focus); a "Change" toggle reveals them.
  const [showPickers, setShowPickers] = useState(!params.get("concept"));
  const [session, setSession] = useState(null);
  const [index, setIndex] = useState(0);
  const [selected, setSelected] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [incompleteId, setIncompleteId] = useState(null);
  const [currentTopic, setCurrentTopic] = useState(null);
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopicName, setSelectedTopicName] = useState("");
  const [revealed, setRevealed] = useState(false);
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
  const navigate = useNavigate();
  const { refreshRoadmap } = useOutletContext() || {};
  const [advancing, setAdvancing] = useState(false);
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
      const msg = err?.response?.data?.message || "";
      // A previous quiz is still open — offer to continue it via a popup.
      const stuckId = (msg.match(/[0-9a-fA-F-]{36}/) || [])[0];
      if (stuckId) setIncompleteId(stuckId);
      else setError(msg || "Could not start quiz. Check concept/topic and try again.");
    } finally {
      setStarting(false);
    }
  };

  // Re-open an in-progress session so the user can finish it.
  const resumeSession = async (sessionId) => {
    if (!sessionId) return;
    setIncompleteId(null);
    setError("");
    setStarting(true);
    try {
      const details = await getQuizDetailsApi(sessionId);
      const raw = details.questions || [];
      const questions = raw.map((q) => ({ question: q.question, options: q.options }));
      if (!questions.length) {
        setError("This quiz session has no questions.");
        return;
      }
      if ((details.status || "").toUpperCase() !== "IN_PROGRESS") {
        setError("This quiz is already completed.");
        return;
      }
      if (details.conceptName) setConcept(details.conceptName);
      setSession({ sessionId: details.sessionId, questions, totalQuestions: details.totalQuestions || questions.length });
      setActiveQuizSession(details.sessionId);
      sessionStorage.setItem("activeQuizSessionId", details.sessionId);

      const firstUnanswered = raw.findIndex((q) => q.selectedAnswerIndex == null || q.selectedAnswerIndex < 0);
      if (firstUnanswered < 0) {
        // Everything was answered — just finalize it.
        const final = await completeQuizApi(details.sessionId);
        clearQuizSession();
        sessionStorage.removeItem("activeQuizSessionId");
        setSession(null);
        setResult(final);
      } else {
        setIndex(firstUnanswered);
        setSelected(null);
        setRevealed(false);
        setQuestionStartedAt(Date.now());
      }
    } catch (e) {
      setError(e?.response?.data?.message || "Could not open the quiz.");
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
    const accuracy = result.accuracyPercent ?? 0;
    const passed = accuracy >= 60;
    const next = result.nextConceptToStudy;
    const topicDone = result.roadmapTopicCompleted === true || (result.roadmapTopicProgress ?? 0) >= 80 || !next;
    const goNextConcept = () => {
      navigate(`/space/learn?concept=${encodeURIComponent(next)}&topic=${encodeURIComponent(topic)}`);
      setResult(null);
    };
    const goNextChapter = async () => {
      setAdvancing(true);
      try {
        const nt = await getCurrentTopicApi(); // flips the next topic to IN_PROGRESS server-side
        refreshRoadmap?.();
        navigate(`/space/learn?concept=${encodeURIComponent(nt?.nextConcept || "")}&topic=${encodeURIComponent(nt?.topicName || "")}`);
        setResult(null);
      } catch {
        navigate("/dashboard"); // roadmap fully complete
      } finally {
        setAdvancing(false);
      }
    };
    return (
      <Card className="text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl border-2 border-[var(--accent)] bg-[var(--accent-light)]">
          <Icon name={passed ? "emoji_events" : "school"} size={34} fill={1} className="text-[var(--accent)]" />
        </div>
        <p className="text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Quiz complete</p>
        <h3 className="mt-1 text-3xl font-extrabold tracking-tight text-[var(--text)]">
          {result.totalCorrect}<span className="text-[var(--text-muted)]">/{result.totalQuestions}</span>
        </h3>
        <p className="mt-1 text-sm font-bold text-[var(--accent)]">{accuracy.toFixed(0)}% accuracy</p>

        <div className="mx-auto mt-4 grid max-w-sm grid-cols-2 gap-2">
          <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3">
            <div className="flex items-center justify-center gap-1.5 text-[var(--text-muted)]">
              <Icon name="check_circle" size={16} className="text-[var(--accent)]" />
              <span className="text-xs font-bold uppercase tracking-wide">Correct</span>
            </div>
            <p className="mt-1 text-xl font-extrabold text-[var(--text)]">{result.totalCorrect}</p>
          </div>
          <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3">
            <div className="flex items-center justify-center gap-1.5 text-[var(--text-muted)]">
              <Icon name="percent" size={16} className="text-[var(--accent)]" />
              <span className="text-xs font-bold uppercase tracking-wide">Accuracy</span>
            </div>
            <p className="mt-1 text-xl font-extrabold text-[var(--text)]">{accuracy.toFixed(0)}%</p>
          </div>
        </div>

        <div className="mx-auto mt-4 max-w-md rounded-2xl border-2 border-[var(--accent)] bg-[var(--accent-light)] p-3">
          <p className="text-sm font-medium text-[var(--text)]">{result.roadmapMessage || "Keep practicing."}</p>
        </div>

        {result.difficultyChanged && result.updatedDifficulty && (
          <div className="mx-auto mt-3 inline-flex items-center gap-1.5 rounded-full bg-[var(--accent-light)] px-3 py-1 text-xs font-extrabold capitalize text-[var(--accent-hover)]">
            <Icon name="trending_up" size={14} /> Leveled up to {result.updatedDifficulty}
          </div>
        )}

        <div className="mx-auto mt-5 max-w-md space-y-2">
          {topicDone ? (
            <Button className="w-full gap-1.5 active:scale-95" disabled={advancing} onClick={goNextChapter}>
              <Icon name={advancing ? "progress_activity" : "arrow_forward"} size={18} fill={1} className={advancing ? "animate-spin" : ""} />
              {advancing ? "Loading next chapter…" : "Next chapter"}
            </Button>
          ) : next ? (
            <Button className="w-full gap-1.5 active:scale-95" onClick={goNextConcept}>
              <Icon name="arrow_forward" size={18} fill={1} /> Next: {next.length > 26 ? `${next.slice(0, 26)}…` : next}
            </Button>
          ) : (
            <Link to="/space">
              <Button className="w-full gap-1.5 active:scale-95">
                <Icon name="check" size={18} fill={1} /> Done
              </Button>
            </Link>
          )}
          <div className="flex gap-2">
            <Link to={`/space/practice?concept=${encodeURIComponent(concept)}&topic=${encodeURIComponent(topic)}`} className="flex-1">
              <Button variant="ghost" className="w-full gap-1.5">
                <Icon name="fitness_center" size={16} /> Practice it
              </Button>
            </Link>
            <Link to="/space" className="flex-1">
              <Button variant="ghost" className="w-full gap-1.5">
                <Icon name="format_list_bulleted" size={16} /> Topic
              </Button>
            </Link>
          </div>
        </div>
      </Card>
    );
  }

  if (!session) {
    return (
      <div className="space-y-5">
        {/* Focus header: concept + Learn→Quiz→Practice indicator (top-level, matches Learn/Practice) */}
        <WizardStepHeader concept={concept} active="quiz" />

        <Card>
          {/* Concept focus row: read-only chip + Change toggle */}
          <div className="flex items-center justify-between gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--accent-light)] px-3 py-1.5 text-sm font-extrabold text-[var(--accent-hover)]">
              <Icon name="target" size={16} /> {concept || "Pick a concept"}
            </span>
            {params.get("concept") ? (
              <button
                type="button"
                onClick={() => setShowPickers((v) => !v)}
                className="text-xs font-extrabold text-[var(--accent-hover)] active:scale-95"
              >
                {showPickers ? "Done" : "Change"}
              </button>
            ) : null}
          </div>

          {/* Collapsible pickers — collapsed by default when a concept is in the URL */}
          {showPickers ? (
            <div className="mt-4">
              {topicOptions.length ? (
                <div className="mb-4">
                  <p className="mb-2 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Topic</p>
                  <div className="flex flex-wrap gap-2">
                    {topicOptions.map((t) => (
                      <button
                        key={t.id || t.topicName}
                        onClick={() => {
                          setSelectedTopicName(t.topicName);
                          setTopic(t.topicName);
                        }}
                        className={`rounded-2xl border-2 px-3.5 py-1.5 text-sm font-semibold transition-colors ${
                          topic === t.topicName
                            ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
                            : "border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)] hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
                        }`}
                      >
                        {t.topicName}
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              {selectedConceptNames.length ? (
                <div className="mb-4">
                  <p className="mb-2 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Concept</p>
                  <div className="flex flex-wrap gap-2">
                    {selectedConceptNames.map((name) => (
                      <button
                        key={name}
                        onClick={() => setConcept(name)}
                        className={`inline-flex items-center gap-1.5 rounded-2xl border-2 px-3.5 py-1.5 text-sm font-semibold transition-colors ${
                          concept === name
                            ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
                            : "border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)] hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
                        }`}
                      >
                        {concept === name ? <Icon name="check_circle" size={16} className="text-[var(--accent)]" /> : null}
                        {name}
                      </button>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="mb-4 flex items-center gap-2 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3 text-sm text-[var(--text-muted)]">
                  <Icon name="lock" size={18} />
                  No unlocked concepts in this topic yet.
                </div>
              )}
            </div>
          ) : null}

          {activeQuizSessionId ? (
            <div className="mt-4 flex items-start gap-2 rounded-2xl border-2 border-[var(--warning)]/40 bg-[var(--warning)]/10 p-3">
              <Icon name="info" size={18} className="mt-0.5 text-[var(--warning)]" />
              <p className="text-sm font-medium text-[var(--text)]">
                You have an active session in this browser. Continue it or abandon.
              </p>
            </div>
          ) : null}

          {error ? (
            <div className="mt-4 flex items-start gap-2 rounded-2xl border-2 border-[var(--error)]/40 bg-[var(--error)]/5 p-3">
              <Icon name="error" size={18} className="mt-0.5 text-[var(--error)]" />
              <p className="text-sm font-medium text-[var(--error)]">{error}</p>
            </div>
          ) : null}

          <Button className="mt-4 w-full gap-1.5 active:scale-95" disabled={!concept || !topic || starting} onClick={start}>
            {starting ? (
              <>
                <Icon name="progress_activity" size={18} className="animate-spin" /> Starting...
              </>
            ) : (
              <>
                <Icon name="play_arrow" size={20} fill={1} /> Start Quiz
              </>
            )}
          </Button>
        </Card>

        {/* History & Stats Section */}
        <Card>
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="flex w-full items-center justify-between text-left"
          >
            <div className="flex items-center gap-2.5">
              <Icon name="history" size={22} className="text-[var(--accent)]" />
              <h3 className="text-lg font-extrabold tracking-tight text-[var(--text)]">History &amp; Stats</h3>
            </div>
            <span className="flex h-8 w-8 items-center justify-center rounded-xl text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-2)] hover:text-[var(--text)]">
              <Icon name={showHistory ? "expand_less" : "expand_more"} size={20} />
            </span>
          </button>

          {showHistory && (
            <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} transition={{ duration: 0.2 }} className="mt-4 space-y-4 overflow-hidden">
              {/* Stats Cards */}
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3 text-center">
                  <Icon name="format_list_numbered" size={18} className="text-[var(--accent)]" />
                  <p className="mt-1 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Attempts</p>
                  <p className="text-xl font-extrabold text-[var(--text)]">{stats.totalAttempts}</p>
                </div>
                <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3 text-center">
                  <Icon name="target" size={18} className="text-[var(--accent)]" />
                  <p className="mt-1 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Avg Accuracy</p>
                  <p className="text-xl font-extrabold text-[var(--text)]">{stats.avgAccuracy.toFixed(0)}%</p>
                </div>
                <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3 text-center">
                  <Icon name="verified" size={18} className="text-[var(--accent)]" />
                  <p className="mt-1 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Pass Rate</p>
                  <p className="text-xl font-extrabold text-[var(--text)]">{stats.passRate}%</p>
                </div>
                <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3 text-center">
                  <Icon name="timer" size={18} className="text-[var(--accent)]" />
                  <p className="mt-1 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Avg Time</p>
                  <p className="text-xl font-extrabold text-[var(--text)]">{formatMs(stats.avgTime)}</p>
                </div>
              </div>

              {/* History List */}
              {loadingHistory ? (
                <Spinner size={28} label="Loading history..." className="py-8" />
              ) : history.length === 0 ? (
                <EmptyState dashed icon="inbox" title="No quiz history yet" message="Start a quiz to see your progress!" />
              ) : (
                <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
                  {history.slice(0, 10).map((quiz, idx) => {
                    const pct = Math.round((quiz.totalCorrect / quiz.totalQuestions) * 100);
                    const passed = quiz.totalCorrect / quiz.totalQuestions >= 0.6;
                    const inProgress = (quiz.status || "").toUpperCase() === "IN_PROGRESS";
                    return (
                      <motion.div
                        key={idx}
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.2, delay: idx * 0.05 }}
                        className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-3.5 transition-colors hover:border-[var(--accent)]/40"
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-bold text-[var(--text)]">{quiz.conceptName}</p>
                            <p className="mt-0.5 inline-flex items-center gap-1 text-xs text-[var(--text-muted)]">
                              <Icon name="calendar_today" size={13} /> {formatDate(quiz.startedAt)}
                            </p>
                          </div>
                          <div className="shrink-0 text-right">
                            <p className="text-lg font-extrabold text-[var(--text)]">{quiz.totalCorrect}/{quiz.totalQuestions}</p>
                            {inProgress ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-[var(--accent-light)] px-2 py-0.5 text-xs font-bold text-[var(--accent-hover)]">
                                <Icon name="hourglass_top" size={13} /> In progress
                              </span>
                            ) : (
                              <span
                                className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-bold ${
                                  passed ? "bg-[var(--accent-light)] text-[var(--accent)]" : "bg-[var(--error)]/10 text-[var(--error)]"
                                }`}
                              >
                                <Icon name={passed ? "check_circle" : "cancel"} size={13} fill={1} /> {pct}%
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="mt-2.5 flex flex-wrap items-center gap-2 text-xs text-[var(--text-muted)]">
                          <span className="inline-flex items-center gap-1 rounded-full border-2 border-[var(--border)] bg-[var(--surface-2)] px-2 py-0.5 font-semibold">
                            <Icon name="timer" size={13} /> {formatMs(quiz.timeTakenMs || 0)}
                          </span>
                          <span className="inline-flex items-center gap-1 rounded-full border-2 border-[var(--border)] bg-[var(--surface-2)] px-2 py-0.5 font-semibold capitalize">
                            <Icon name="bar_chart" size={13} /> {quiz.difficulty || "Unknown"}
                          </span>
                          <span className="inline-flex items-center gap-1 rounded-full border-2 border-[var(--border)] bg-[var(--surface-2)] px-2 py-0.5 font-semibold">
                            <Icon name="tag" size={13} /> {quiz.sessionId?.slice(-6) || "-"}
                          </span>
                        </div>
                        {inProgress && (
                          <Button className="mt-2.5 w-full gap-1.5" onClick={() => resumeSession(quiz.sessionId)}>
                            <Icon name="play_arrow" size={16} fill={1} /> Continue this quiz
                          </Button>
                        )}
                      </motion.div>
                    );
                  })}
                </div>
              )}
            </motion.div>
          )}
        </Card>

        {/* Unfinished-quiz popup */}
        <AnimatePresence>
          {incompleteId && (
            <div
              className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
              onClick={() => setIncompleteId(null)}
            >
              <motion.div
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                onClick={(e) => e.stopPropagation()}
                className="w-full max-w-sm rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-6 text-center"
              >
                <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--accent-light)]">
                  <Icon name="quiz" size={26} className="text-[var(--accent)]" />
                </div>
                <h3 className="text-lg font-extrabold tracking-tight">Unfinished quiz</h3>
                <p className="mt-2 text-sm font-medium text-[var(--text-muted)]">
                  You already have a quiz in progress. Do you want to continue it?
                </p>
                <div className="mt-5 flex flex-col gap-2">
                  <Button className="w-full gap-1.5" onClick={() => resumeSession(incompleteId)}>
                    <Icon name="play_arrow" size={18} fill={1} /> Continue quiz
                  </Button>
                  <Button variant="ghost" className="w-full" onClick={() => setIncompleteId(null)}>
                    Not now
                  </Button>
                </div>
              </motion.div>
            </div>
          )}
        </AnimatePresence>
      </div>
    );
  }

  if (!question) {
    return (
      <Card>
        <Spinner size={30} label="Loading question..." className="py-10" />
      </Card>
    );
  }

  const totalQ = session.totalQuestions || session.questions.length;
  const progressPct = Math.round(((index + 1) / totalQ) * 100);

  return (
    <div className="relative">
      <Card>
      <WizardStepHeader concept={concept} active="quiz" />
      {/* Progress header */}
      <div className="mb-4">
        <div className="mb-2 flex items-center justify-between">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--accent-light)] px-3 py-1 text-xs font-bold text-[var(--accent)]">
            <Icon name="quiz" size={14} fill={1} /> Q {index + 1}/{totalQ}
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full border-2 border-[var(--border)] bg-[var(--surface-2)] px-3 py-1 text-xs font-bold text-[var(--text)]">
            <Icon name="timer" size={14} /> {formatMs(elapsed)}
          </span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-[var(--surface-2)]">
          <div className="h-full rounded-full bg-[var(--accent)] transition-all duration-300" style={{ width: `${progressPct}%` }} />
        </div>
      </div>

      <h3 className="mb-4 text-lg font-extrabold leading-snug tracking-tight text-[var(--text)]">{question.question}</h3>

      {/* Hints Section */}
      <div className="mb-4 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-3">
        <p className="mb-2 inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">
          <Icon name="lightbulb" size={15} className="text-[var(--accent)]" /> Need a hand?
        </p>
        <div className="mb-1 flex flex-wrap gap-2">
          <Button
            variant="ghost"
            className="px-3.5 py-1.5"
            disabled={loadingHint || revealed}
            onClick={() => getHint(1)}
          >
            <Icon name="lightbulb" size={16} className="mr-1" /> Hint 1
          </Button>
          <Button
            variant="ghost"
            className="px-3.5 py-1.5"
            disabled={loadingHint || revealed || !hints[`${index}-1`]}
            onClick={() => getHint(2)}
          >
            <Icon name="lightbulb" size={16} className="mr-1" /> Hint 2
          </Button>
          <Button
            variant="ghost"
            className="px-3.5 py-1.5"
            disabled={loadingHint || revealed || !hints[`${index}-2`]}
            onClick={() => getHint(3)}
          >
            <Icon name="lightbulb" size={16} className="mr-1" /> Hint 3
          </Button>
        </div>
        {loadingHint && (
          <p className="mt-2 inline-flex items-center gap-1.5 text-sm text-[var(--text-muted)]">
            <Icon name="progress_activity" size={15} className="animate-spin text-[var(--accent)]" /> Loading hint...
          </p>
        )}
        {Object.entries(hints).filter(([key]) => key.startsWith(`${index}-`)).map(([key, hint]) => (
          <div key={key} className="mt-2 flex items-start gap-2 rounded-xl border-2 border-[var(--accent)]/30 bg-[var(--accent-light)] p-2.5">
            <Icon name="tips_and_updates" size={16} fill={1} className="mt-0.5 shrink-0 text-[var(--accent)]" />
            <p className="text-sm font-medium text-[var(--text)]">{hint}</p>
          </div>
        ))}
      </div>

      <div className="space-y-2.5">
        {(question.options || []).map((opt, i) => {
          const isCorrect = revealed?.correctIndex === i;
          const isSelected = selected === i;
          const optionClass = revealed
            ? isCorrect
              ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
              : isSelected
                ? "border-[var(--error)]/40 bg-[var(--error)]/5 text-[var(--text)]"
                : "border-[var(--border)] bg-[var(--surface)] text-[var(--text-muted)]"
            : isSelected
              ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--text)]"
              : "border-[var(--border)] bg-[var(--surface)] text-[var(--text)] hover:bg-[var(--surface-2)]";

          const letter = String.fromCharCode(65 + i);
          const showCheck = revealed && isCorrect;
          const showCross = revealed && isSelected && !isCorrect;

          return (
            <button
              key={opt}
              disabled={revealed || submitting}
              onClick={() => !revealed && !submitting && setSelected(i)}
              className={`flex w-full items-center gap-3 rounded-2xl border-2 p-3 text-left transition-colors disabled:cursor-default ${optionClass}`}
            >
              <span
                className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs font-extrabold ${
                  isSelected || showCheck
                    ? showCross
                      ? "bg-[var(--error)]/10 text-[var(--error)]"
                      : "bg-[var(--accent)] text-white"
                    : "bg-[var(--surface-2)] text-[var(--text-muted)]"
                }`}
              >
                {showCheck ? <Icon name="check" size={16} /> : showCross ? <Icon name="close" size={16} /> : letter}
              </span>
              <span className="flex-1 text-sm font-medium">{opt}</span>
              {showCheck ? <Icon name="check_circle" size={20} fill={1} className="text-[var(--accent)]" /> : null}
              {showCross ? <Icon name="cancel" size={20} fill={1} className="text-[var(--error)]" /> : null}
            </button>
          );
        })}
      </div>
      {!revealed ? (
        <Button className="mt-4 w-full" disabled={selected == null || submitting} onClick={submit}>
          {submitting ? (
            <>
              <Icon name="progress_activity" size={18} className="mr-1.5 animate-spin" /> Submitting...
            </>
          ) : (
            <>
              <Icon name="send" size={18} className="mr-1.5" /> Submit Answer
            </>
          )}
        </Button>
      ) : (
        <div className="mt-4 space-y-3">
          <div
            className={`flex items-start gap-2 rounded-2xl border-2 p-3 ${
              revealed.correct === false
                ? "border-[var(--error)]/40 bg-[var(--error)]/5"
                : "border-[var(--accent)] bg-[var(--accent-light)]"
            }`}
          >
            <Icon
              name={revealed.correct === false ? "cancel" : "check_circle"}
              size={18}
              fill={1}
              className={`mt-0.5 shrink-0 ${revealed.correct === false ? "text-[var(--error)]" : "text-[var(--accent)]"}`}
            />
            <div className="space-y-1">
              <p className={`text-sm font-bold ${revealed.correct === false ? "text-[var(--error)]" : "text-[var(--accent)]"}`}>
                {revealed.correct === false ? "Not quite" : "Correct!"}
              </p>
              <p className="text-sm font-medium text-[var(--text)]">{revealed.explanation}</p>
              {revealed.correct === false ? (
                <p className="text-sm text-[var(--text-muted)]">
                  Correct answer:{" "}
                  <span className="font-semibold text-[var(--text)]">{(question.options || [])[revealed.correctIndex] ?? "N/A"}</span>
                </p>
              ) : null}
            </div>
          </div>
          <Button
            className="w-full"
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
            {finalizing ? (
              <>
                <Icon name="progress_activity" size={18} className="mr-1.5 animate-spin" /> Finishing...
              </>
            ) : index < session.questions.length - 1 ? (
              <>
                Next Question <Icon name="arrow_forward" size={18} className="ml-1.5" />
              </>
            ) : (
              <>
                <Icon name="flag" size={18} className="mr-1.5" /> See Results
              </>
            )}
          </Button>
        </div>
      )}
      {error ? (
        <div className="mt-4 flex items-start gap-2 rounded-2xl border-2 border-[var(--error)]/40 bg-[var(--error)]/5 p-3">
          <Icon name="error" size={18} className="mt-0.5 text-[var(--error)]" />
          <p className="text-sm font-medium text-[var(--error)]">{error}</p>
        </div>
      ) : null}
    </Card>
  </div>
  );
}
