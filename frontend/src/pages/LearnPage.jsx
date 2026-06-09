import { useEffect, useState } from "react";
import { Link, useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import { explainApi } from "../api/learn";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import WizardStepHeader from "../components/ui/WizardStepHeader";
import EmptyState from "../components/ui/EmptyState";
import { getConceptPoolFromTopic, getPreferredConcept, getSelectableConceptNames, getSelectableTopics } from "../utils/study";

const sourceTypeLabel = {
  RETRIEVED: { icon: "menu_book", text: "From web sources" },
  AI_KNOWLEDGE: { icon: "psychology", text: "From AI knowledge base" },
  SCRAPED_FRESH: { icon: "public", text: "Freshly retrieved" },
  AI_FALLBACK: { icon: "smart_toy", text: "AI generated" },
};

// Token-colored renderers so the explanation reads cleanly on white (no typography plugin needed).
const markdownComponents = {
  h1: (props) => <h1 className="mt-5 mb-2 text-xl font-extrabold tracking-tight text-[var(--text)]" {...props} />,
  h2: (props) => <h2 className="mt-5 mb-2 text-lg font-extrabold tracking-tight text-[var(--text)]" {...props} />,
  h3: (props) => <h3 className="mt-4 mb-2 text-base font-bold tracking-tight text-[var(--text)]" {...props} />,
  p: (props) => <p className="my-2.5 leading-relaxed text-[var(--text)]" {...props} />,
  ul: (props) => <ul className="my-2.5 ml-5 list-disc space-y-1 text-[var(--text)] marker:text-[var(--text-muted)]" {...props} />,
  ol: (props) => <ol className="my-2.5 ml-5 list-decimal space-y-1 text-[var(--text)] marker:text-[var(--text-muted)]" {...props} />,
  li: (props) => <li className="leading-relaxed" {...props} />,
  a: (props) => <a className="font-medium text-[var(--accent)] underline underline-offset-2 hover:text-[var(--accent-hover)]" target="_blank" rel="noreferrer" {...props} />,
  strong: (props) => <strong className="font-bold text-[var(--text)]" {...props} />,
  blockquote: (props) => (
    <blockquote className="my-3 border-l-4 border-[var(--border)] bg-[var(--surface-2)] py-1 pl-4 text-[var(--text-muted)]" {...props} />
  ),
  hr: (props) => <hr className="my-4 border-[var(--border)]" {...props} />,
  code: ({ inline, className = "", children, ...props }) =>
    inline ? (
      <code className="rounded-md bg-[var(--surface-2)] px-1.5 py-0.5 font-mono text-[0.85em] text-[var(--accent-hover)]" {...props}>
        {children}
      </code>
    ) : (
      <code className={`font-mono text-[0.85em] ${className}`} {...props}>
        {children}
      </code>
    ),
  pre: (props) => (
    <pre
      className="my-3 overflow-x-auto rounded-2xl border-2 border-[var(--border)] bg-[var(--surface-2)] p-4 text-sm leading-relaxed text-[var(--text)]"
      {...props}
    />
  ),
  table: (props) => (
    <div className="my-3 overflow-x-auto rounded-2xl border-2 border-[var(--border)]">
      <table className="w-full border-collapse text-sm text-[var(--text)]" {...props} />
    </div>
  ),
  th: (props) => <th className="border-b-2 border-[var(--border)] bg-[var(--surface-2)] px-3 py-2 text-left font-bold" {...props} />,
  td: (props) => <td className="border-b border-[var(--border)] px-3 py-2 align-top" {...props} />,
};

export default function LearnPage() {
  const navigate = useNavigate();
  const { concept: ctxConcept } = useOutletContext() || {};
  const [params, setParams] = useSearchParams();
  const [concept, setConcept] = useState(params.get("concept") || "");
  const [topic, setTopic] = useState(params.get("topic") || "");
  const [question, setQuestion] = useState(`Explain ${params.get("concept") || "this concept"} in detail`);
  // When a concept is present in the URL the pickers start collapsed (topic focus); a "Change" toggle reveals them.
  const [showPickers, setShowPickers] = useState(!params.get("concept"));
  const headerConcept = ctxConcept || concept || topic;
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

  const sourceLabel = data?.answer ? sourceTypeLabel[data.sourceType] || sourceTypeLabel.AI_FALLBACK : null;

  return (
    <div className="space-y-5">
      {/* Persistent focus header — Learn has no destructive session, so X just returns to the Space. */}
      <WizardStepHeader concept={headerConcept} active="learn" />

      {/* Setup Card — concept focus row, collapsible pickers, question input, then the primary action. */}
      <Card>
        {/* Concept focus row: read-only chip + Change toggle */}
        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--accent-light)] px-3 py-1.5 text-sm font-extrabold text-[var(--accent-hover)]">
            <Icon name="target" size={16} /> {concept || "Pick a concept"}
          </span>
          <button
            type="button"
            onClick={() => setShowPickers((v) => !v)}
            className="text-xs font-extrabold text-[var(--accent-hover)] active:scale-95"
          >
            {showPickers ? "Done" : "Change"}
          </button>
        </div>

        {showPickers ? (
          <div className="mt-4">
            {topicOptions.length ? (
              <div className="mb-4">
                <p className="mb-2 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Choose topic</p>
                <div className="flex flex-wrap gap-2">
                  {topicOptions.map((t) => {
                    const active = topic === t.topicName;
                    return (
                      <button
                        key={t.id || t.topicName}
                        onClick={() => {
                          setSelectedTopicName(t.topicName);
                          setTopic(t.topicName);
                        }}
                        className={`rounded-2xl border-2 px-3 py-1.5 text-xs font-bold tracking-wide transition-colors active:scale-95 ${
                          active
                            ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--accent-hover)]"
                            : "border-[var(--border)] bg-[var(--surface-2)] text-[var(--text-muted)] hover:text-[var(--text)]"
                        }`}
                      >
                        {t.topicName}
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}

            {selectedConceptNames.length ? (
              <div>
                <p className="mb-2 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">Select concept</p>
                <div className="flex flex-wrap gap-2">
                  {selectedConceptNames.map((name) => {
                    const active = concept === name;
                    return (
                      <button
                        key={name}
                        onClick={() => setConcept(name)}
                        className={`inline-flex items-center gap-1.5 rounded-2xl border-2 px-3 py-1.5 text-xs font-bold tracking-wide transition-colors active:scale-95 ${
                          active
                            ? "border-[var(--accent)] bg-[var(--accent-light)] text-[var(--accent-hover)]"
                            : "border-[var(--border)] bg-[var(--surface-2)] text-[var(--text-muted)] hover:text-[var(--text)]"
                        }`}
                      >
                        {active ? <Icon name="check_circle" size={14} className="text-[var(--accent)]" /> : null}
                        {name}
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-2 rounded-2xl bg-[var(--surface-2)] px-4 py-3 text-sm text-[var(--text-muted)]">
                <Icon name="lock" size={18} className="text-[var(--text-muted)]" />
                No unlocked concepts in this topic yet.
              </div>
            )}
          </div>
        ) : null}

        {/* Page-specific input — Learn: the question textarea */}
        <div className="mt-4">
          <label htmlFor="learn-question" className="mb-2 block text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">
            What do you want to understand?
          </label>
          <textarea
            id="learn-question"
            className="min-h-28 w-full resize-y rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-3.5 font-medium text-[var(--text)] outline-none transition-colors placeholder:text-[var(--text-muted)] focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--accent)]/20"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            maxLength={1500}
            placeholder="Ask anything about this concept..."
          />
          <div className="mt-1.5 text-right text-xs text-[var(--text-muted)]">{question.length}/1500</div>
        </div>

        {/* One full-width primary action */}
        <Button disabled={!concept || loading} onClick={submit} className="mt-4 w-full gap-1.5 active:scale-95">
          <Icon name={loading ? "progress_activity" : "auto_awesome"} size={18} className={loading ? "animate-spin" : ""} />
          {loading ? "Generating..." : "Get explanation"}
        </Button>
      </Card>

      {/* Error state */}
      {error ? (
        <Card className="border-[var(--error)]">
          <div className="flex items-center gap-2 text-sm font-medium text-[var(--error)]">
            <Icon name="error" size={18} />
            {error}
          </div>
        </Card>
      ) : null}

      {/* Loading skeleton */}
      {loading ? (
        <Card>
          <div className="flex items-center gap-2 text-sm font-medium text-[var(--text-muted)]">
            <Icon name="progress_activity" size={18} className="animate-spin text-[var(--accent)]" />
            Crafting your explanation...
          </div>
          <div className="mt-4 space-y-2.5">
            <div className="h-4 w-1/3 animate-pulse rounded-full bg-[var(--surface-2)]" />
            <div className="h-3 w-full animate-pulse rounded-full bg-[var(--surface-2)]" />
            <div className="h-3 w-11/12 animate-pulse rounded-full bg-[var(--surface-2)]" />
            <div className="h-3 w-4/5 animate-pulse rounded-full bg-[var(--surface-2)]" />
          </div>
        </Card>
      ) : null}

      {/* Explanation */}
      {data?.answer ? (
        <Card>
          <div className="mb-4 flex flex-wrap items-center gap-2">
            {sourceLabel ? (
              <span className="inline-flex items-center gap-1.5 rounded-2xl bg-[var(--accent-light)] px-3 py-1 text-xs font-bold text-[var(--accent-hover)]">
                <Icon name={sourceLabel.icon} size={15} className="text-[var(--accent)]" />
                {sourceLabel.text}
              </span>
            ) : null}
            {data.detectedLanguage ? (
              <span className="inline-flex items-center gap-1.5 rounded-2xl bg-[var(--surface-2)] px-3 py-1 text-xs font-medium text-[var(--text-muted)]">
                <Icon name="translate" size={15} />
                {data.detectedLanguage}
              </span>
            ) : null}
          </div>

          <div className="text-[0.95rem] text-[var(--text)]">
            <ReactMarkdown components={markdownComponents}>{data.answer}</ReactMarkdown>
          </div>

          {Array.isArray(data.sources) && data.sources.length ? (
            <div className="mt-5 border-t-2 border-[var(--border)] pt-4">
              <p className="mb-2 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">
                <Icon name="link" size={15} className="text-[var(--accent)]" />
                Sources
              </p>
              <div className="space-y-2">
                {data.sources.slice(0, 3).map((source) => (
                  <a
                    key={source.url}
                    href={source.url}
                    target="_blank"
                    rel="noreferrer"
                    className="group flex items-center gap-2 rounded-2xl bg-[var(--surface-2)] px-3.5 py-2.5 text-sm font-medium text-[var(--text)] transition-colors hover:bg-[var(--accent-light)]"
                  >
                    <Icon name="open_in_new" size={16} className="shrink-0 text-[var(--text-muted)] group-hover:text-[var(--accent)]" />
                    <span className="truncate group-hover:text-[var(--accent-hover)]">{source.title || source.url}</span>
                  </a>
                ))}
              </div>
            </div>
          ) : null}

          <div className="mt-5 flex flex-col gap-2 border-t-2 border-[var(--border)] pt-4 sm:flex-row">
            <Link to={`/space/quiz?${query}`} className="w-full sm:flex-1">
              <Button className="w-full active:scale-95">
                <Icon name="quiz" size={18} className="mr-2" />
                Take Quiz
              </Button>
            </Link>
            <Link to={`/space/practice?${query}`} className="w-full sm:flex-1">
              <Button variant="secondary" className="w-full active:scale-95">
                <Icon name="code" size={18} className="mr-2" />
                Practice Problem
              </Button>
            </Link>
          </div>
        </Card>
      ) : null}

      {/* Empty state */}
      {!data?.answer && !loading && !error ? (
        <Card>
          <EmptyState
            icon="lightbulb"
            iconClassName="text-[var(--accent)]"
            iconSize={28}
            title="Ready when you are"
            message="Pick a concept above and hit Get Explanation to start learning."
          />
        </Card>
      ) : null}
    </div>
  );
}
