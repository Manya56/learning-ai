import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { completeQuizApi, startQuizApi, submitQuizAnswerApi } from "../api/quiz";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import { useUiStore } from "../store/uiStore";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";

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
    }
  };

  const submit = async () => {
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

  if (result) {
    return (
      <Card>
        <h3 className="mb-2 text-2xl font-semibold">Score: {result.totalCorrect}/{result.totalQuestions}</h3>
        <p className="mb-2 text-xs text-[var(--text-muted)]">Accuracy: {result.accuracyPercent ?? 0}%</p>
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
        <Button disabled={!concept || !topic} onClick={start}>Start Quiz</Button>
      </Card>
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
      <div className="space-y-2">
        {(question.options || []).map((opt, i) => (
          <button
            key={opt}
            onClick={() => !revealed && setSelected(i)}
            className={`block w-full rounded-lg border p-2 text-left ${selected === i ? "border-[var(--accent)]" : "border-[var(--border)]"}`}
          >
            {opt}
          </button>
        ))}
      </div>
      {!revealed ? (
        <Button className="mt-3" disabled={selected == null} onClick={submit}>Submit Answer</Button>
      ) : (
        <div className="mt-3 space-y-2">
          <p className="text-sm">{revealed.explanation}</p>
          {revealed.correct === false ? (
            <p className="text-sm text-[var(--warning)]">
              Correct answer: {(question.options || [])[revealed.correctIndex] ?? "N/A"}
            </p>
          ) : null}
          <Button
            onClick={async () => {
              setSelected(null);
              setRevealed(false);
              if (index < session.questions.length - 1) {
                setIndex(index + 1);
                setQuestionStartedAt(Date.now());
              }
              else {
                try {
                  const final = await completeQuizApi(session.sessionId);
                  clearQuizSession();
                  sessionStorage.removeItem("activeQuizSessionId");
                  setResult(final);
                  setError("");
                } catch (err) {
                  setError(err?.response?.data?.message || "Could not complete quiz. Try again.");
                }
              }
            }}
          >
            {index < session.questions.length - 1 ? "Next Question" : "See Results"}
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
  );
}
