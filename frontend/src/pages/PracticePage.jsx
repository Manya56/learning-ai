import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { evaluatePracticeApi, generatePracticeApi } from "../api/practice";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";

export default function PracticePage() {
  const [params, setParams] = useSearchParams();
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
  const [problem, setProblem] = useState(null);
  const [answer, setAnswer] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [startedAt, setStartedAt] = useState(null);
  const [currentTopic, setCurrentTopic] = useState(null);
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopicName, setSelectedTopicName] = useState("");
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
    setError("");
    try {
      const data = await generatePracticeApi({ conceptName: concept, topicGoal: topic, difficulty: null, language: null });
      setProblem(data);
      setResult(null);
      setAnswer("");
      setStartedAt(Date.now());
    } catch (err) {
      setError(err?.response?.data?.message || "Could not generate practice problem. Try again.");
    }
  };

  const submit = async () => {
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
    }
  };

  return (
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
        <Button disabled={!concept || !topic} onClick={generate}>Generate Problem</Button>
      ) : (
        <>
          <p className="mb-3 whitespace-pre-wrap">{problem.problemStatement}</p>
          <textarea className="min-h-40 w-full rounded-md bg-[var(--surface-2)] p-2" value={answer} onChange={(e) => setAnswer(e.target.value)} />
          <Button className="mt-3" onClick={submit}>Submit Answer</Button>
        </>
      )}
      {error ? <p className="mt-3 text-sm text-red-400">{error}</p> : null}
      {result ? <p className="mt-3 text-sm">Score: {result.score}/10</p> : null}
    </Card>
  );
}
