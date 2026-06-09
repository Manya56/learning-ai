import { useEffect, useState } from "react";
import { Link, useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import { evaluatePracticeApi, generatePracticeApi, getCodingHistoryApi, getCodingAttemptDetailsApi } from "../api/practice";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import WizardStepHeader from "../components/ui/WizardStepHeader";
import EmptyState from "../components/ui/EmptyState";
import Chip from "../components/ui/Chip";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";
import { motion } from "framer-motion";

// Small label used above grouped controls and sections.
const FieldLabel = ({ children }) => (
  <p className="mb-2 text-[11px] font-bold uppercase tracking-wide text-[var(--text-muted)]">{children}</p>
);

// Compact stat tile (matches the rest of the website).
const StatTile = ({ icon, value, label, fill = 0 }) => (
  <div className="rounded-2xl bg-[var(--surface-2)] p-4">
    <div className="flex items-center gap-1.5">
      <Icon name={icon} size={18} fill={fill} className="text-[var(--accent)]" />
      <span className="text-2xl font-extrabold text-[var(--text)]">{value}</span>
    </div>
    <p className="mt-1 text-[11px] font-bold uppercase tracking-wide text-[var(--text-muted)]">{label}</p>
  </div>
);

// Labeled feedback block with an icon header. `tone` switches accent vs error emphasis.
const FeedbackBlock = ({ icon, title, tone = "accent", children }) => {
  const color = tone === "error" ? "text-[var(--error)]" : "text-[var(--accent)]";
  return (
    <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-4">
      <h5 className={`mb-2 flex items-center gap-1.5 text-sm font-extrabold tracking-tight ${color}`}>
        <Icon name={icon} size={18} fill={1} /> {title}
      </h5>
      <div className="text-sm leading-relaxed text-[var(--text)]">{children}</div>
    </div>
  );
};

export default function PracticePage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const { refreshRoadmap } = useOutletContext() || {};
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
  const conceptFromUrl = params.get("concept") || "";
  const [showPickers, setShowPickers] = useState(!conceptFromUrl);
  const [advancing, setAdvancing] = useState(false);
  const [problem, setProblem] = useState(null);
  const [answer, setAnswer] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [startedAt, setStartedAt] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [currentTopic, setCurrentTopic] = useState(null);
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopicName, setSelectedTopicName] = useState("");
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [selectedAttemptDetails, setSelectedAttemptDetails] = useState(null);
  const [loadingAttemptDetails, setLoadingAttemptDetails] = useState(false);
  const [showAttemptModal, setShowAttemptModal] = useState(false);
  const conceptPool = getConceptPoolFromTopic(currentTopic);
  const conceptNames = getSelectableConceptNames(conceptPool);
  const topicOptions = getSelectableTopics(roadmap?.topics || []);
  const selectedTopic = topicOptions.find((t) => t.topicName === selectedTopicName) || currentTopic;
  const selectedConceptPool = selectedTopic ? getConceptPoolFromTopic(selectedTopic) : conceptPool;
  const selectedConceptNames = getSelectableConceptNames(selectedConceptPool);

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
      const data = await getCodingHistoryApi();
      setHistory(data || []);
    } catch (err) {
      console.error("Failed to load practice history:", err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const loadAttemptDetails = async (attemptId) => {
    setLoadingAttemptDetails(true);
    try {
      const details = await getCodingAttemptDetailsApi(attemptId);
      setSelectedAttemptDetails(details);
      setShowAttemptModal(true);
    } catch (err) {
      console.error("Failed to load attempt details:", err);
    } finally {
      setLoadingAttemptDetails(false);
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

  const generate = async () => {
    setGenerating(true);
    setError("");
    try {
      const data = await generatePracticeApi({ conceptName: concept, topicGoal: topic, difficulty: null, language: null });
      setProblem(data);
      setResult(null);
      setAnswer("");
      setStartedAt(Date.now());
    } catch (err) {
      setError(err?.response?.data?.message || "Could not generate practice problem. Try again.");
    } finally {
      setGenerating(false);
    }
  };

  const submit = async () => {
    setSubmitting(true);
    setError("");
    try {
      const data = await evaluatePracticeApi({
        problemStatement: problem.problemStatement,
        userSubmission: answer,
        conceptName: concept,
        problemType: problem.problemType,
        language: problem.language || "text",
        timeTakenMs: startedAt ? Date.now() - startedAt : 45000,
      });
      setResult(data);
    } catch (err) {
      setError(err?.response?.data?.message || "Could not submit answer. Try again.");
    } finally {
      setSubmitting(false);
    }
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

  const formatMs = (ms) => {
    const sec = Math.floor(ms / 1000);
    const m = String(Math.floor(sec / 60)).padStart(2, "0");
    const s = String(sec % 60).padStart(2, "0");
    return `${m}:${s}`;
  };

  const calculateStats = () => {
    if (!history || !history.length) {
      return { totalAttempts: 0, avgScore: 0, passRate: 0, avgTime: 0, byType: {} };
    }
    const totalAttempts = history.length;
    const avgScore = history.reduce((sum, h) => sum + (h.score || 0), 0) / totalAttempts;
    const passCount = history.filter(h => h.passed).length;
    const passRate = (passCount / totalAttempts) * 100;
    const avgTime = history.reduce((sum, h) => sum + (h.timeTakenMs || 0), 0) / totalAttempts;
    
    const byType = {};
    history.forEach(h => {
      if (!byType[h.problemType]) byType[h.problemType] = 0;
      byType[h.problemType]++;
    });
    
    return { totalAttempts, avgScore: Math.round(avgScore * 10) / 10, passRate: Math.round(passRate), avgTime, byType };
  };

  const stats = calculateStats();

  return (
    <div className="space-y-5">
      {/* Focus header: concept + Learn→Quiz→Practice indicator (top-level, matches Learn/Quiz) */}
      <WizardStepHeader concept={concept} active="practice" />

      <Card>
        {/* Concept focus row: read-only chip + Change toggle */}
        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--accent-light)] px-3 py-1.5 text-sm font-extrabold text-[var(--accent-hover)]">
            <Icon name="target" size={16} /> {concept || "Pick a concept"}
          </span>
          {conceptFromUrl ? (
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
                <FieldLabel>Choose topic</FieldLabel>
                <div className="flex flex-wrap gap-2">
                  {topicOptions.map((t) => (
                    <Chip
                      key={t.id || t.topicName}
                      active={topic === t.topicName}
                      onClick={() => {
                        setSelectedTopicName(t.topicName);
                        setTopic(t.topicName);
                        setProblem(null);
                        setResult(null);
                      }}
                    >
                      {t.topicName}
                    </Chip>
                  ))}
                </div>
              </div>
            ) : null}
            {selectedConceptNames.length ? (
              <div className="mb-4">
                <FieldLabel>Select concept</FieldLabel>
                <div className="flex flex-wrap gap-2">
                  {selectedConceptNames.map((name) => (
                    <Chip
                      key={name}
                      active={concept === name}
                      onClick={() => {
                        setConcept(name);
                        setProblem(null);
                        setResult(null);
                      }}
                    >
                      {name}
                    </Chip>
                  ))}
                </div>
              </div>
            ) : (
              <div className="mb-4 flex items-center gap-2 rounded-2xl bg-[var(--surface-2)] px-4 py-3 text-sm font-medium text-[var(--text-muted)]">
                <Icon name="lock" size={18} /> No unlocked concepts in this topic yet.
              </div>
            )}
          </div>
        ) : null}

        {!problem ? (
          <Button className="mt-4 w-full gap-1.5 active:scale-95" disabled={!concept || !topic || generating} onClick={generate}>
            <Icon name={generating ? "progress_activity" : "bolt"} size={18} className={generating ? "animate-spin" : ""} fill={1} />
            {generating ? "Generating..." : concept ? `Generate problem on ${concept.length > 26 ? `${concept.slice(0, 26)}…` : concept}` : "Generate Problem"}
          </Button>
        ) : (
          <>
            {/* Problem statement */}
            <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-4">
              <p className="mb-2 flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wide text-[var(--text-muted)]">
                <Icon name="assignment" size={16} /> Problem
                {problem.difficulty ? <span className="rounded-full bg-[var(--surface-2)] px-2 py-0.5 normal-case text-[var(--text-muted)]">{problem.difficulty}</span> : null}
                {problem.problemType ? <span className="rounded-full bg-[var(--surface-2)] px-2 py-0.5 normal-case text-[var(--text-muted)]">{problem.problemType}</span> : null}
              </p>
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-[var(--text)]">{problem.problemStatement}</p>
            </div>

            {/* Code editor */}
            <div className="mt-4 overflow-hidden rounded-2xl border-2 border-[var(--border)]">
              <div className="flex items-center justify-between border-b-2 border-[var(--border)] bg-[var(--surface-2)] px-4 py-2">
                <span className="flex items-center gap-1.5 text-xs font-bold text-[var(--text-muted)]">
                  <Icon name="terminal" size={16} /> Your solution
                  {problem.language ? <span className="rounded-full bg-[var(--surface)] px-2 py-0.5 text-[var(--text-muted)]">{problem.language}</span> : null}
                </span>
                <div className="flex items-center gap-1.5">
                  <span className="h-2.5 w-2.5 rounded-full bg-[var(--border)]" />
                  <span className="h-2.5 w-2.5 rounded-full bg-[var(--border)]" />
                  <span className="h-2.5 w-2.5 rounded-full bg-[var(--accent)]" />
                </div>
              </div>
              <textarea
                className="min-h-48 w-full resize-y bg-[var(--surface-2)] p-4 font-mono text-sm leading-relaxed text-[var(--text)] outline-none placeholder:text-[var(--text-muted)]"
                placeholder="Write your code here..."
                spellCheck={false}
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                disabled={submitting}
              />
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
              <Button className="gap-1.5" disabled={submitting} onClick={submit}>
                <Icon name={submitting ? "progress_activity" : "play_arrow"} size={18} className={submitting ? "animate-spin" : ""} fill={1} />
                {submitting ? "Evaluating..." : "Submit & Evaluate"}
              </Button>
              <Button variant="ghost" className="gap-1.5" disabled={generating || submitting} onClick={generate}>
                <Icon name="refresh" size={18} /> New problem
              </Button>
            </div>
          </>
        )}

        {error ? (
          <div className="mt-4 flex items-center gap-2 rounded-2xl bg-[var(--error)]/10 px-4 py-3 text-sm font-bold text-[var(--error)]">
            <Icon name="error" size={18} fill={1} /> {error}
          </div>
        ) : null}

        {result ? (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-5 space-y-4"
          >
            {/* Score Header */}
            <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <h4 className="flex items-center gap-1.5 text-base font-extrabold tracking-tight text-[var(--text)]">
                  <Icon
                    name={result.passed ? "check_circle" : "cancel"}
                    size={20}
                    fill={1}
                    className={result.passed ? "text-[var(--accent)]" : "text-[var(--error)]"}
                  />
                  Evaluation Result
                </h4>
                <span
                  className={`rounded-full px-3 py-1 text-xs font-bold ${
                    result.passed ? "bg-[var(--accent-light)] text-[var(--accent-hover)]" : "bg-[var(--error)]/10 text-[var(--error)]"
                  }`}
                >
                  {result.score}/10 · {result.passed ? "Passed" : "Needs work"}
                </span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-[var(--surface)]">
                <motion.div
                  className={`h-full rounded-full ${result.passed ? "bg-[var(--accent)]" : "bg-[var(--error)]"}`}
                  initial={{ width: 0 }}
                  animate={{ width: `${(result.score / 10) * 100}%` }}
                  transition={{ duration: 0.5 }}
                />
              </div>
            </div>

            {/* Detailed Feedback */}
            <div className="grid gap-4 md:grid-cols-2">
              {result.strengths && (
                <FeedbackBlock icon="fitness_center" title="Strengths" tone="accent">
                  <p className="whitespace-pre-wrap">{result.strengths}</p>
                </FeedbackBlock>
              )}

              {result.issues && (
                <FeedbackBlock icon="warning" title="Areas for Improvement" tone="error">
                  <p className="whitespace-pre-wrap">{result.issues}</p>
                </FeedbackBlock>
              )}
            </div>

            {result.suggestions && (
              <FeedbackBlock icon="lightbulb" title="Suggestions" tone="accent">
                <p className="whitespace-pre-wrap">{result.suggestions}</p>
              </FeedbackBlock>
            )}

            {result.correctedSolution && (
              <FeedbackBlock icon="auto_awesome" title="Suggested Solution" tone="accent">
                <pre className="mt-1 overflow-x-auto whitespace-pre-wrap rounded-2xl bg-[var(--surface-2)] p-4 font-mono text-sm leading-relaxed text-[var(--text)]">{result.correctedSolution}</pre>
              </FeedbackBlock>
            )}

            {result.lineFeedback && result.lineFeedback.length > 0 && (
              <FeedbackBlock icon="edit_note" title="Line-by-Line Feedback" tone="accent">
                <div className="space-y-2">
                  {result.lineFeedback.map((feedback, idx) => (
                    <div key={idx} className="rounded-2xl bg-[var(--surface-2)] p-3">
                      <code className="mb-1 block font-mono text-sm text-[var(--text)]">{feedback.line}</code>
                      <p className="text-sm text-[var(--text-muted)]">{feedback.comment}</p>
                    </div>
                  ))}
                </div>
              </FeedbackBlock>
            )}

            {/* Progress Info */}
            {(result.roadmapTopicProgress !== undefined || result.nextConceptToStudy) && (
              <FeedbackBlock icon="trending_up" title="Learning Progress" tone="accent">
                {result.roadmapTopicProgress !== undefined && (
                  <div className="mb-3">
                    <div className="mb-1 flex justify-between text-xs font-bold text-[var(--text-muted)]">
                      <span>Topic Progress</span>
                      <span>{result.roadmapTopicProgress}%</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-[var(--surface-2)]">
                      <motion.div
                        className="h-full rounded-full bg-[var(--accent)]"
                        initial={{ width: 0 }}
                        animate={{ width: `${result.roadmapTopicProgress}%` }}
                        transition={{ duration: 0.5 }}
                      />
                    </div>
                  </div>
                )}
                {result.nextConceptToStudy && (
                  <p className="text-sm text-[var(--text)]">
                    <span className="font-bold">Next:</span> {result.nextConceptToStudy}
                  </p>
                )}
                {result.roadmapMessage && (
                  <p className="mt-1 text-sm italic text-[var(--text-muted)]">{result.roadmapMessage}</p>
                )}
              </FeedbackBlock>
            )}

            {/* Next flow: advance without going back */}
            {(() => {
              const next = result?.nextConceptToStudy;
              const topicDone = (result?.roadmapTopicProgress ?? 0) >= 80 || !next;
              const goNextConcept = () =>
                navigate(`/space/learn?concept=${encodeURIComponent(next || "")}&topic=${encodeURIComponent(topic || "")}`);
              const goNextChapter = async () => {
                setAdvancing(true);
                try {
                  const nt = await getCurrentTopicApi(); // flips the next topic to IN_PROGRESS server-side
                  refreshRoadmap?.();
                  navigate(`/space/learn?concept=${encodeURIComponent(nt?.nextConcept || "")}&topic=${encodeURIComponent(nt?.topicName || "")}`);
                } catch {
                  navigate("/dashboard"); // roadmap fully complete
                } finally {
                  setAdvancing(false);
                }
              };
              return (
                <div className="space-y-2">
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
                        <Icon name="format_list_bulleted" size={18} fill={1} /> Back to topic
                      </Button>
                    </Link>
                  )}
                  <div className="flex flex-col gap-2 sm:flex-row">
                    <Button variant="ghost" className="w-full gap-1.5" disabled={generating || submitting} onClick={generate}>
                      <Icon name="refresh" size={16} /> Try again
                    </Button>
                    <Link to="/space" className="w-full">
                      <Button variant="ghost" className="w-full gap-1.5">
                        <Icon name="format_list_bulleted" size={16} /> Back to topic
                      </Button>
                    </Link>
                  </div>
                </div>
              );
            })()}
          </motion.div>
        ) : null}
      </Card>

      {/* History & Stats Section */}
      <Card>
        <button
          onClick={() => setShowHistory(!showHistory)}
          className="flex w-full items-center justify-between text-left"
        >
          <h3 className="flex items-center gap-1.5 text-lg font-extrabold tracking-tight text-[var(--text)]">
            <Icon name="history" size={20} className="text-[var(--accent)]" /> History &amp; Stats
          </h3>
          <span className="text-[var(--text-muted)]"><Icon name={showHistory ? "expand_less" : "expand_more"} size={22} /></span>
        </button>

        {showHistory && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} transition={{ duration: 0.2 }} className="mt-4 space-y-4">
            {/* Stats Tiles */}
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <StatTile icon="assignment" value={stats.totalAttempts} label="Attempts" />
              <StatTile icon="grade" fill={1} value={`${stats.avgScore}/10`} label="Avg Score" />
              <StatTile icon="verified" value={`${stats.passRate}%`} label="Pass Rate" />
              <StatTile icon="timer" value={formatMs(stats.avgTime)} label="Avg Time" />
            </div>

            {/* Problem Type Breakdown */}
            {Object.keys(stats.byType).length > 0 && (
              <div className="rounded-2xl bg-[var(--surface-2)] p-4">
                <FieldLabel>By Problem Type</FieldLabel>
                <div className="flex flex-wrap gap-2">
                  {Object.entries(stats.byType).map(([type, count]) => (
                    <span key={type} className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs font-bold text-[var(--text-muted)]">
                      {type}: {count}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* History List */}
            {loadingHistory ? (
              <div className="space-y-2">
                {[0, 1, 2].map((i) => (
                  <div key={i} className="h-20 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
                ))}
              </div>
            ) : history.length === 0 ? (
              <EmptyState dashed icon="terminal" title="No practice history yet" message="Generate and submit a problem to track your progress." />
            ) : (
              <div className="max-h-80 space-y-2 overflow-y-auto pr-1">
                {history.slice(0, 10).map((attempt, idx) => (
                  <motion.button
                    key={idx}
                    type="button"
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.2, delay: idx * 0.05 }}
                    className="block w-full rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-4 text-left transition-colors hover:border-[var(--accent)] hover:bg-[var(--surface-2)]"
                    onClick={() => loadAttemptDetails(attempt.id)}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-extrabold text-[var(--text)]">{attempt.conceptName}</p>
                        <p className="text-xs font-medium text-[var(--text-muted)]">{formatDate(attempt.attemptedAt)}</p>
                      </div>
                      <span
                        className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-bold ${
                          attempt.passed ? "bg-[var(--accent-light)] text-[var(--accent-hover)]" : "bg-[var(--error)]/10 text-[var(--error)]"
                        }`}
                      >
                        {attempt.score}/10
                      </span>
                    </div>
                    <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs font-bold text-[var(--text-muted)]">
                      <span className="inline-flex items-center gap-1"><Icon name="assignment" size={14} /> {attempt.problemType || "Unknown"}</span>
                      <span className="inline-flex items-center gap-1"><Icon name="timer" size={14} /> {formatMs(attempt.timeTakenMs || 0)}</span>
                      <span className="inline-flex items-center gap-1"><Icon name="target" size={14} /> {attempt.difficulty || "Unknown"}</span>
                      <span className="ml-auto inline-flex items-center gap-1 text-[var(--accent)]">
                        {loadingAttemptDetails ? <Icon name="progress_activity" size={14} className="animate-spin" /> : <Icon name="visibility" size={14} />} View Details
                      </span>
                    </div>
                  </motion.button>
                ))}
              </div>
            )}
          </motion.div>
        )}
      </Card>

      {/* Practice Attempt Details Modal */}
      {showAttemptModal && selectedAttemptDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[var(--text)]/40 p-4 backdrop-blur-sm">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] shadow-xl"
          >
            <div className="p-6">
              {/* Header */}
              <div className="mb-6 flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <h2 className="text-2xl font-extrabold tracking-tight text-[var(--text)]">{selectedAttemptDetails.conceptName} Practice Review</h2>
                  <p className="mt-1 flex flex-wrap items-center gap-x-2 text-sm font-medium text-[var(--text-muted)]">
                    <span>{formatDate(selectedAttemptDetails.attemptedAt)}</span>
                    <span>·</span><span>{selectedAttemptDetails.problemType}</span>
                    <span>·</span><span>{selectedAttemptDetails.difficulty}</span>
                    <span>·</span><span>{formatMs(selectedAttemptDetails.timeTakenMs)}</span>
                  </p>
                </div>
                <button
                  onClick={() => setShowAttemptModal(false)}
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 border-[var(--border)] text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
                  aria-label="Close"
                >
                  <Icon name="close" size={18} />
                </button>
              </div>

              {/* Score Summary */}
              <div className="mb-5 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-5">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <h3 className="flex items-center gap-1.5 text-base font-extrabold tracking-tight text-[var(--text)]">
                      <Icon
                        name={selectedAttemptDetails.passed ? "check_circle" : "cancel"}
                        size={20}
                        fill={1}
                        className={selectedAttemptDetails.passed ? "text-[var(--accent)]" : "text-[var(--error)]"}
                      />
                      Evaluation Result
                    </h3>
                    <p className="mt-1 text-sm font-bold text-[var(--text-muted)]">
                      {selectedAttemptDetails.passed ? "Passed" : "Needs improvement"}
                    </p>
                  </div>
                  <div className="text-right">
                    <div className={`text-3xl font-extrabold ${selectedAttemptDetails.passed ? "text-[var(--accent)]" : "text-[var(--error)]"}`}>
                      {selectedAttemptDetails.score}/10
                    </div>
                  </div>
                </div>
                <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-[var(--surface)]">
                  <div
                    className={`h-full rounded-full ${selectedAttemptDetails.passed ? "bg-[var(--accent)]" : "bg-[var(--error)]"}`}
                    style={{ width: `${(selectedAttemptDetails.score / 10) * 100}%` }}
                  />
                </div>
              </div>

              {/* Problem Statement */}
              <div className="mb-5 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5">
                <h3 className="mb-3 flex items-center gap-1.5 text-sm font-extrabold tracking-tight text-[var(--text)]">
                  <Icon name="assignment" size={18} className="text-[var(--accent)]" /> Problem Statement
                </h3>
                <p className="whitespace-pre-wrap text-sm leading-relaxed text-[var(--text)]">{selectedAttemptDetails.problemStatement}</p>
              </div>

              {/* User's Submission */}
              <div className="mb-5 overflow-hidden rounded-2xl border-2 border-[var(--border)]">
                <div className="flex items-center gap-1.5 border-b-2 border-[var(--border)] bg-[var(--surface-2)] px-4 py-2 text-xs font-bold text-[var(--text-muted)]">
                  <Icon name="terminal" size={16} /> Your Submission
                </div>
                <pre className="overflow-x-auto whitespace-pre-wrap bg-[var(--surface-2)] p-4 font-mono text-sm leading-relaxed text-[var(--text)]">{selectedAttemptDetails.userSubmission}</pre>
              </div>

              {/* Feedback */}
              {selectedAttemptDetails.feedback && (
                <div className="mb-5 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5">
                  <h3 className="mb-3 flex items-center gap-1.5 text-sm font-extrabold tracking-tight text-[var(--accent)]">
                    <Icon name="auto_awesome" size={18} fill={1} /> AI Feedback
                  </h3>
                  <p className="whitespace-pre-wrap text-sm leading-relaxed text-[var(--text)]">{selectedAttemptDetails.feedback}</p>
                </div>
              )}

              {selectedAttemptDetails.lineFeedback && selectedAttemptDetails.lineFeedback.length > 0 && (
                <div className="mb-5 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5">
                  <h3 className="mb-3 flex items-center gap-1.5 text-sm font-extrabold tracking-tight text-[var(--accent)]">
                    <Icon name="edit_note" size={18} /> Line-by-Line Feedback
                  </h3>
                  <div className="space-y-2">
                    {selectedAttemptDetails.lineFeedback.map((feedback, idx) => (
                      <div key={idx} className="rounded-2xl bg-[var(--surface-2)] p-4">
                        <code className="mb-2 block font-mono text-sm text-[var(--text)]">{feedback.line}</code>
                        <p className="text-sm text-[var(--text-muted)]">{feedback.comment}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {(selectedAttemptDetails.roadmapTopicProgress !== undefined || selectedAttemptDetails.nextConceptToStudy) && (
                <div className="rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-5">
                  <h3 className="mb-3 flex items-center gap-1.5 text-sm font-extrabold tracking-tight text-[var(--accent)]">
                    <Icon name="trending_up" size={18} /> Learning Progress
                  </h3>
                  {selectedAttemptDetails.roadmapTopicProgress !== undefined && (
                    <div className="mb-3">
                      <div className="mb-1 flex justify-between text-xs font-bold text-[var(--text-muted)]">
                        <span>Topic Progress</span>
                        <span>{selectedAttemptDetails.roadmapTopicProgress}%</span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full bg-[var(--surface-2)]">
                        <div className="h-full rounded-full bg-[var(--accent)]" style={{ width: `${selectedAttemptDetails.roadmapTopicProgress}%` }} />
                      </div>
                    </div>
                  )}
                  {selectedAttemptDetails.nextConceptToStudy && (
                    <p className="text-sm text-[var(--text)]">
                      <span className="font-bold">Next:</span> {selectedAttemptDetails.nextConceptToStudy}
                    </p>
                  )}
                  {selectedAttemptDetails.roadmapMessage && (
                    <p className="mt-1 text-sm italic text-[var(--text-muted)]">{selectedAttemptDetails.roadmapMessage}</p>
                  )}
                </div>
              )}

              <div className="mt-6 flex justify-end">
                <Button className="gap-1.5" onClick={() => setShowAttemptModal(false)}>
                  <Icon name="check" size={18} /> Close Review
                </Button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
