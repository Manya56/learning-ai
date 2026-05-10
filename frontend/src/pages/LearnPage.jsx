import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import { explainApi } from "../api/learn";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";

const sourceTypeLabel = {
  RETRIEVED: "📚 From web sources",
  AI_KNOWLEDGE: "🧠 From AI knowledge base",
  SCRAPED_FRESH: "🌐 Freshly retrieved",
  AI_FALLBACK: "🤖 AI generated",
};

export default function LearnPage() {
  const [params, setParams] = useSearchParams();
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
  const [question, setQuestion] = useState(`Explain ${params.get("concept") || "this concept"} in detail`);
  const [currentTopic, setCurrentTopic] = useState(null);
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopicName, setSelectedTopicName] = useState("");
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const query = `concept=${encodeURIComponent(concept)}&topic=${encodeURIComponent(topic)}`;
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
    if (!topic && selectedTopic?.topicName) {
      setTopic(selectedTopic.topicName);
    }
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
    setQuestion(`Explain ${concept || "this concept"} in detail`);
  }, [concept]);

  const submit = async () => {
    setLoading(true);
    setError("");
    try {
      setData(await explainApi({ conceptName: concept, topicGoal: topic, question }));
    } catch (err) {
      setError(err?.response?.data?.message || "Could not load explanation. Try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <Card>
        <h3 className="font-semibold">{concept || "No concept selected"}</h3>
        <p className="text-sm text-[var(--text-muted)]">{topic}</p>
        {topicOptions.length ? (
          <div className="mt-3">
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
          <div className="mt-3">
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
          <p className="mt-3 text-xs text-[var(--text-muted)]">No unlocked concepts in this topic yet.</p>
        )}
      </Card>
      <Card>
        <label className="mb-2 block text-sm text-[var(--text-muted)]">What do you want to understand?</label>
        <textarea className="min-h-28 w-full rounded-md bg-[var(--surface-2)] p-2" value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={1500}/>
        <Button className="mt-3" disabled={!concept || loading} onClick={submit}>
          {loading ? "Loading..." : "Get Explanation"}
        </Button>
      </Card>
      {error ? <Card><p className="text-sm text-red-400">{error}</p></Card> : null}
      {loading ? <div className="h-24 animate-pulse rounded-xl bg-[var(--surface)]" /> : null}
      {data?.answer ? (
        <Card>
          <div className="mb-3 flex flex-wrap items-center gap-2 text-xs text-[var(--text-muted)]">
            <span className="rounded-full bg-[var(--surface-2)] px-2 py-1">
              {sourceTypeLabel[data.sourceType] || sourceTypeLabel.AI_FALLBACK}
            </span>
            {data.detectedLanguage ? <span>{data.detectedLanguage}</span> : null}
          </div>
          <ReactMarkdown>{data.answer}</ReactMarkdown>
          {Array.isArray(data.sources) && data.sources.length ? (
            <div className="mt-4 space-y-1 text-sm">
              <p className="text-[var(--text-muted)]">Sources</p>
              {data.sources.slice(0, 3).map((source) => (
                <a
                  key={source.url}
                  href={source.url}
                  target="_blank"
                  rel="noreferrer"
                  className="block text-[var(--accent)] underline"
                >
                  {source.title || source.url}
                </a>
              ))}
            </div>
          ) : null}
          <div className="mt-4 flex gap-2">
            <Link to={`/quiz?${query}`}><Button>Take Quiz</Button></Link>
            <Link to={`/practice?${query}`}><Button variant="secondary">Practice Problem</Button></Link>
          </div>
        </Card>
      ) : null}
    </div>
  );
}
