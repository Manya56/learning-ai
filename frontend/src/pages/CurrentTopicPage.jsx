import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getCurrentTopicApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { getConceptPoolFromTopic, getPreferredConcept, pickConceptName } from "../utils/study";

export default function CurrentTopicPage() {
  const [data, setData] = useState(null);
  useEffect(() => {
    getCurrentTopicApi().then(setData).catch(() => setData({ concepts: [] }));
  }, []);
  const topic = data?.topicName || "Current Topic";
  const conceptPool = useMemo(() => getConceptPoolFromTopic(data), [data]);
  const currentConcept = useMemo(() => data?.nextConcept || getPreferredConcept(conceptPool), [data, conceptPool]);
  const query = `concept=${encodeURIComponent(currentConcept)}&topic=${encodeURIComponent(topic)}`;
  const completedSet = useMemo(() => new Set(data?.completedConcepts || []), [data]);
  return (
    <Card>
      <h3 className="mb-2 font-semibold">Topic: {topic}</h3>
      <p className="mb-3 text-sm text-[var(--text-muted)]">Progress: {data?.progressPercent ?? 0}%</p>
      {!data ? <div className="mb-3 h-16 animate-pulse rounded bg-[var(--surface-2)]" /> : null}
      <div className="mb-4 space-y-2">
        {(data?.concepts || []).map((concept) => (
          <div
            key={pickConceptName(concept)}
            className={`rounded-lg border p-2 ${pickConceptName(concept) === currentConcept ? "border-[var(--accent)]" : "border-[var(--border)]"}`}
          >
            {completedSet.has(pickConceptName(concept)) ? "✅" : pickConceptName(concept) === currentConcept ? "🎯" : "○"} {pickConceptName(concept)}
          </div>
        ))}
      </div>
      <div className="flex flex-wrap gap-2">
        <Link to={`/learn?${query}`}><Button>Learn This</Button></Link>
        <Link to={`/quiz?${query}`}><Button variant="secondary">Take Quiz</Button></Link>
        <Link to={`/practice?${query}`}><Button variant="ghost">Practice</Button></Link>
      </div>
    </Card>
  );
}
