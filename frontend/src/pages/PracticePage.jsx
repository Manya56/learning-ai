import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { evaluatePracticeApi, generatePracticeApi, getCodingHistoryApi, getCodingAttemptDetailsApi } from "../api/practice";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";
import { motion } from "framer-motion";

export default function PracticePage() {
  const [params, setParams] = useSearchParams();
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
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
    <div className="space-y-4">
      <Card>
        <h3 className="mb-2 font-semibold">Practice: {concept || "No concept selected"}</h3>
        <p className="mb-3 text-sm text-[var(--text-muted)]">Topic: {topic}</p>
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
                    setProblem(null);
                    setResult(null);
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
                  onClick={() => {
                    setConcept(name);
                    setProblem(null);
                    setResult(null);
                  }}
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
        {!problem ? (
          <Button disabled={!concept || !topic || generating} onClick={generate}>
            {generating ? "Generating..." : "Generate Problem"}
          </Button>
        ) : (
          <>
            <p className="mb-3 whitespace-pre-wrap">{problem.problemStatement}</p>
            <textarea
              className="min-h-40 w-full rounded-md bg-[var(--surface-2)] p-2"
              value={answer}
              onChange={(e) => setAnswer(e.target.value)}
              disabled={submitting}
            />
            <Button className="mt-3" disabled={submitting} onClick={submit}>
              {submitting ? "Submitting..." : "Submit Answer"}
            </Button>
          </>
        )}
        {error ? <p className="mt-3 text-sm text-red-400">{error}</p> : null}
        {result ? (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 space-y-4"
          >
            {/* Score Header */}
            <div className={`rounded-lg p-4 ${result.passed ? "bg-green-50 border border-green-200" : "bg-red-50 border border-red-200"}`}>
              <div className="flex items-center justify-between mb-2">
                <h4 className="font-semibold text-lg">Evaluation Result</h4>
                <div className={`px-3 py-1 rounded-full text-sm font-bold ${result.passed ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}`}>
                  {result.score}/10 - {result.passed ? "PASSED" : "NEEDS WORK"}
                </div>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full ${result.passed ? "bg-green-500" : "bg-red-500"}`}
                  style={{ width: `${(result.score / 10) * 100}%` }}
                ></div>
              </div>
            </div>

            {/* Detailed Feedback */}
            <div className="grid gap-4 md:grid-cols-2">
              {result.strengths && (
                <div className="rounded-lg bg-green-50 border border-green-200 p-4">
                  <h5 className="font-semibold text-green-800 mb-2 flex items-center">
                    <span className="mr-2">💪</span> Strengths
                  </h5>
                  <p className="text-sm text-green-700 whitespace-pre-wrap">{result.strengths}</p>
                </div>
              )}

              {result.issues && (
                <div className="rounded-lg bg-orange-50 border border-orange-200 p-4">
                  <h5 className="font-semibold text-orange-800 mb-2 flex items-center">
                    <span className="mr-2">⚠️</span> Areas for Improvement
                  </h5>
                  <p className="text-sm text-orange-700 whitespace-pre-wrap">{result.issues}</p>
                </div>
              )}
            </div>

            {result.suggestions && (
              <div className="rounded-lg bg-blue-50 border border-blue-200 p-4">
                <h5 className="font-semibold text-blue-800 mb-2 flex items-center">
                  <span className="mr-2">💡</span> Suggestions
                </h5>
                <p className="text-sm text-blue-700 whitespace-pre-wrap">{result.suggestions}</p>
              </div>
            )}

            {result.correctedSolution && (
              <div className="rounded-lg bg-purple-50 border border-purple-200 p-4">
                <h5 className="font-semibold text-purple-800 mb-2 flex items-center">
                  <span className="mr-2">✨</span> Suggested Solution
                </h5>
                <pre className="text-sm text-purple-700 bg-purple-100 p-3 rounded overflow-x-auto whitespace-pre-wrap">{result.correctedSolution}</pre>
              </div>
            )}

            {result.lineFeedback && result.lineFeedback.length > 0 && (
              <div className="rounded-lg bg-gray-50 border border-gray-200 p-4">
                <h5 className="font-semibold text-gray-800 mb-3 flex items-center">
                  <span className="mr-2">📝</span> Line-by-Line Feedback
                </h5>
                <div className="space-y-2">
                  {result.lineFeedback.map((feedback, idx) => (
                    <div key={idx} className="bg-white p-3 rounded border">
                      <code className="text-sm font-mono text-gray-800 block mb-1">{feedback.line}</code>
                      <p className="text-sm text-gray-600">{feedback.comment}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Progress Info */}
            {(result.roadmapTopicProgress !== undefined || result.nextConceptToStudy) && (
              <div className="rounded-lg bg-indigo-50 border border-indigo-200 p-4">
                <h5 className="font-semibold text-indigo-800 mb-2 flex items-center">
                  <span className="mr-2">📈</span> Learning Progress
                </h5>
                {result.roadmapTopicProgress !== undefined && (
                  <div className="mb-2">
                    <div className="flex justify-between text-sm mb-1">
                      <span>Topic Progress</span>
                      <span>{result.roadmapTopicProgress}%</span>
                    </div>
                    <div className="w-full bg-indigo-200 rounded-full h-2">
                      <div
                        className="bg-indigo-500 h-2 rounded-full"
                        style={{ width: `${result.roadmapTopicProgress}%` }}
                      ></div>
                    </div>
                  </div>
                )}
                {result.nextConceptToStudy && (
                  <p className="text-sm text-indigo-700">
                    <span className="font-semibold">Next:</span> {result.nextConceptToStudy}
                  </p>
                )}
                {result.roadmapMessage && (
                  <p className="text-sm text-indigo-600 mt-1 italic">{result.roadmapMessage}</p>
                )}
              </div>
            )}
          </motion.div>
        ) : null}
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
              <div className="rounded-lg bg-purple-50 p-3 text-center">
                <p className="text-xs text-[var(--text-muted)]">Avg Score</p>
                <p className="text-xl font-bold text-purple-600">{stats.avgScore}/10</p>
              </div>
              <div className="rounded-lg bg-green-50 p-3 text-center">
                <p className="text-xs text-[var(--text-muted)]">Pass Rate</p>
                <p className="text-xl font-bold text-green-600">{stats.passRate}%</p>
              </div>
              <div className="rounded-lg bg-orange-50 p-3 text-center">
                <p className="text-xs text-[var(--text-muted)]">Avg Time</p>
                <p className="text-xl font-bold text-orange-600">{formatMs(stats.avgTime)}</p>
              </div>
            </div>

            {/* Problem Type Breakdown */}
            {Object.keys(stats.byType).length > 0 && (
              <div className="rounded-lg border border-[var(--border)] bg-[var(--surface-2)] p-3">
                <p className="mb-2 text-sm font-semibold">By Problem Type</p>
                <div className="flex flex-wrap gap-2">
                  {Object.entries(stats.byType).map(([type, count]) => (
                    <span key={type} className="rounded-full bg-[var(--surface)] px-3 py-1 text-xs">
                      {type}: {count}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* History List */}
            {loadingHistory ? (
              <p className="py-4 text-center text-sm text-[var(--text-muted)]">Loading history...</p>
            ) : history.length === 0 ? (
              <p className="py-4 text-center text-sm text-[var(--text-muted)]">No practice history yet. Generate and submit a problem to see your progress!</p>
            ) : (
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {history.slice(0, 10).map((attempt, idx) => (
                  <motion.div
                    key={idx}
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.2, delay: idx * 0.05 }}
                    className="rounded-lg border border-[var(--border)] bg-[var(--surface-2)] p-3 cursor-pointer hover:bg-[var(--surface)] transition-colors"
                    onClick={() => loadAttemptDetails(attempt.id)}
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="font-semibold text-sm">{attempt.conceptName}</p>
                        <p className="text-xs text-[var(--text-muted)]">{formatDate(attempt.attemptedAt)}</p>
                      </div>
                      <div className={`rounded-lg px-2 py-1 text-right font-bold ${attempt.passed ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                        {attempt.score}/10
                      </div>
                    </div>
                    <div className="mt-2 flex gap-4 text-xs text-[var(--text-muted)]">
                      <span>📋 {attempt.problemType || "Unknown"}</span>
                      <span>⏱️ {formatMs(attempt.timeTakenMs || 0)}</span>
                      <span>🎯 {attempt.difficulty || "Unknown"}</span>
                      <span className="ml-auto text-blue-600">👁️ View Details</span>
                    </div>
                  </motion.div>
                ))}
              </div>
            )}
          </motion.div>
        )}
      </Card>

      {/* Practice Attempt Details Modal */}
      {showAttemptModal && selectedAttemptDetails && (
        <div className="fixed inset-0 bg-slate-950/80 flex items-center justify-center p-4 z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-slate-900 text-slate-100 rounded-3xl max-w-4xl w-full max-h-[90vh] overflow-y-auto ring-1 ring-slate-700 shadow-2xl shadow-slate-950/40"
          >
            <div className="p-6">
              <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between mb-6">
                <div>
                  <h2 className="text-3xl font-bold tracking-tight">{selectedAttemptDetails.conceptName} Practice Review</h2>
                  <p className="mt-2 text-sm text-slate-300">
                    {formatDate(selectedAttemptDetails.attemptedAt)} • {selectedAttemptDetails.problemType} • {selectedAttemptDetails.difficulty} • {formatMs(selectedAttemptDetails.timeTakenMs)}
                  </p>
                </div>
                <button
                  onClick={() => setShowAttemptModal(false)}
                  className="self-start rounded-full border border-slate-700 px-3 py-1 text-slate-300 transition hover:bg-slate-800 hover:text-white"
                >
                  Close
                </button>
              </div>

              {/* Score Summary */}
              <div className={`rounded-3xl p-5 mb-6 ${selectedAttemptDetails.passed ? "bg-emerald-950/70 border border-emerald-700" : "bg-rose-950/70 border border-rose-700"}`}>
                <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                  <div>
                    <h3 className="text-xl font-semibold">Evaluation Result</h3>
                    <p className={`mt-1 text-sm ${selectedAttemptDetails.passed ? "text-emerald-300" : "text-rose-300"}`}>
                      Score: {selectedAttemptDetails.score}/10 — {selectedAttemptDetails.passed ? "PASSED" : "NEEDS IMPROVEMENT"}
                    </p>
                  </div>
                  <div className="text-right">
                    <div className={`text-4xl font-bold ${selectedAttemptDetails.passed ? "text-emerald-300" : "text-rose-300"}`}>
                      {selectedAttemptDetails.score}/10
                    </div>
                    <div className={`w-48 rounded-full h-3 mt-3 ${selectedAttemptDetails.passed ? "bg-emerald-700" : "bg-rose-700"}`}>
                      <div
                        className={`h-3 rounded-full ${selectedAttemptDetails.passed ? "bg-emerald-400" : "bg-rose-400"}`}
                        style={{ width: `${(selectedAttemptDetails.score / 10) * 100}%` }}
                      ></div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Problem Statement */}
              <div className="mb-6 rounded-3xl bg-slate-800 border border-slate-700 p-5">
                <h3 className="text-lg font-semibold mb-3">Problem Statement</h3>
                <p className="whitespace-pre-wrap text-slate-200">{selectedAttemptDetails.problemStatement}</p>
              </div>

              {/* User's Submission */}
              <div className="mb-6 rounded-3xl bg-slate-800 border border-slate-700 p-5">
                <h3 className="text-lg font-semibold mb-3">Your Submission</h3>
                <pre className="whitespace-pre-wrap text-sm font-mono text-slate-100 bg-slate-950 rounded-2xl p-4 overflow-x-auto">{selectedAttemptDetails.userSubmission}</pre>
              </div>

              {/* Feedback */}
              {selectedAttemptDetails.feedback && (
                <div className="mb-6 rounded-3xl bg-slate-800 border border-slate-700 p-5">
                  <h3 className="text-lg font-semibold mb-3">AI Feedback</h3>
                  <p className="whitespace-pre-wrap text-slate-200 text-sm">{selectedAttemptDetails.feedback}</p>
                </div>
              )}

              {selectedAttemptDetails.lineFeedback && selectedAttemptDetails.lineFeedback.length > 0 && (
                <div className="mb-6 rounded-3xl bg-slate-800 border border-slate-700 p-5">
                  <h3 className="text-lg font-semibold mb-3">Line-by-Line Feedback</h3>
                  <div className="space-y-3">
                    {selectedAttemptDetails.lineFeedback.map((feedback, idx) => (
                      <div key={idx} className="rounded-2xl bg-slate-950 border border-slate-700 p-4">
                        <code className="text-sm font-mono text-slate-100 block mb-2">{feedback.line}</code>
                        <p className="text-sm text-slate-300">{feedback.comment}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {(selectedAttemptDetails.roadmapTopicProgress !== undefined || selectedAttemptDetails.nextConceptToStudy) && (
                <div className="rounded-3xl bg-slate-800 border border-slate-700 p-5">
                  <h3 className="text-lg font-semibold mb-3">Learning Progress</h3>
                  {selectedAttemptDetails.roadmapTopicProgress !== undefined && (
                    <div className="mb-4">
                      <div className="flex justify-between text-sm text-slate-300 mb-2">
                        <span>Topic Progress</span>
                        <span>{selectedAttemptDetails.roadmapTopicProgress}%</span>
                      </div>
                      <div className="w-full rounded-full h-3 bg-slate-700">
                        <div className="h-3 rounded-full bg-indigo-500" style={{ width: `${selectedAttemptDetails.roadmapTopicProgress}%` }} />
                      </div>
                    </div>
                  )}
                  {selectedAttemptDetails.nextConceptToStudy && (
                    <p className="text-sm text-slate-200">
                      <span className="font-semibold">Next:</span> {selectedAttemptDetails.nextConceptToStudy}
                    </p>
                  )}
                  {selectedAttemptDetails.roadmapMessage && (
                    <p className="text-sm text-slate-400 mt-2 italic">{selectedAttemptDetails.roadmapMessage}</p>
                  )}
                </div>
              )}

              <div className="mt-6 flex justify-end">
                <Button onClick={() => setShowAttemptModal(false)}>Close Review</Button>
              </div>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
