import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { evaluatePracticeApi, generatePracticeApi, getCodingHistoryApi } from "../api/practice";
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
          <div className="mt-3 rounded-lg bg-blue-50 p-3">
            <p className="text-sm"><span className="font-semibold">Score:</span> {result.score}/10</p>
            <p className={`text-sm font-semibold ${result.passed ? "text-green-600" : "text-red-600"}`}>
              {result.passed ? "✓ Passed" : "✗ Needs Improvement"}
            </p>
            {result.feedback && <p className="mt-2 text-sm text-[var(--text-muted)]">{result.feedback}</p>}
          </div>
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
                    className="rounded-lg border border-[var(--border)] bg-[var(--surface-2)] p-3"
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
