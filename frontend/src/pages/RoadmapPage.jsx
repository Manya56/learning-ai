import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

export default function RoadmapPage() {
  const [roadmap, setRoadmap] = useState(null);
  const [filter, setFilter] = useState("ALL");
  const [selectedTopic, setSelectedTopic] = useState(null);

  useEffect(() => {
    getRoadmapApi().then(setRoadmap).catch(() => setRoadmap({ topics: [] }));
  }, []);

  const topicFilters = ["ALL", "IN_PROGRESS", "COMPLETED", "UNLOCKED"];
  const visibleTopics = (roadmap?.topics || []).filter((t) => filter === "ALL" || t.status === filter);
  const statusColor = (status) =>
    status === "COMPLETED"
      ? "text-green-400 bg-green-500/10 border-green-500/30"
      : status === "IN_PROGRESS"
        ? "text-sky-300 bg-sky-500/10 border-sky-500/30"
        : status === "UNLOCKED"
          ? "text-violet-300 bg-violet-500/10 border-violet-500/30"
          : "text-[var(--text-muted)] bg-[var(--surface-2)] border-[var(--border)]";

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <div className="mb-3 rounded-xl border border-[var(--border)] bg-gradient-to-r from-[var(--surface-2)] to-transparent p-4">
          <h3 className="text-xl font-semibold">Your Learning Roadmap</h3>
          <p className="mt-1 text-sm text-[var(--text-muted)]">
            {(roadmap?.completedTopics ?? 0)}/{(roadmap?.totalTopics ?? roadmap?.topics?.length ?? 0)} topics completed •
            {" "}{roadmap?.overallProgressPercent ?? 0}% overall progress
          </p>
        </div>
        <div className="mb-3 flex flex-wrap gap-2">
          {topicFilters.map((item) => (
            <button
              key={item}
              onClick={() => setFilter(item)}
              className={`rounded-full border px-3 py-1.5 text-xs ${
                filter === item ? "border-[var(--accent)] bg-[var(--accent-light)]" : "border-[var(--border)] bg-[var(--surface-2)]"
              }`}
            >
              {item.replace("_", " ")}
            </button>
          ))}
        </div>
        {!roadmap ? (
          <div className="h-24 animate-pulse rounded bg-[var(--surface-2)]" />
        ) : visibleTopics.length ? (
          <div className="space-y-2">
            {visibleTopics.map((topic) => (
              <button
                key={topic.topicOrder}
                onClick={() => setSelectedTopic(topic)}
                className={`w-full rounded-xl border p-3 text-left transition ${selectedTopic?.topicOrder === topic.topicOrder ? "border-[var(--accent)] bg-[var(--accent-light)]/40" : "border-[var(--border)] bg-[var(--surface-2)]/40"}`}
              >
                <div className="flex items-start justify-between gap-2">
                  <p className="font-medium">{topic.topicOrder + 1}. {topic.topicName}</p>
                  <span className={`rounded-full border px-2 py-0.5 text-[10px] ${statusColor(topic.status)}`}>
                    {topic.status || "UNLOCKED"}
                  </span>
                </div>
                <p className="mt-1 text-xs text-[var(--text-muted)]">{topic.progressPercent ?? 0}% complete</p>
                <div className="mt-2 h-2 rounded bg-[var(--surface-2)]">
                  <div className="h-2 rounded bg-[var(--accent)]" style={{ width: `${topic.progressPercent || 0}%` }} />
                </div>
              </button>
            ))}
          </div>
        ) : (
          <p className="text-sm text-[var(--text-muted)]">No topics for this filter.</p>
        )}
      </Card>
      <Card>
        <h3 className="mb-2 font-semibold">Topic Details</h3>
        {!selectedTopic ? (
          <p className="text-sm text-[var(--text-muted)]">Select a topic to view details.</p>
        ) : (
          <>
            <p className="font-medium">{selectedTopic.topicName}</p>
            <div className="mt-1 flex items-center gap-2">
              <span className={`rounded-full border px-2 py-0.5 text-[10px] ${statusColor(selectedTopic.status)}`}>
                {selectedTopic.status}
              </span>
              <p className="text-xs text-[var(--text-muted)]">{selectedTopic.progressPercent ?? 0}% progress</p>
            </div>
            <div className="mt-3 space-y-1">
              {(selectedTopic.concepts || []).map((concept) => {
                const done = (selectedTopic.completedConcepts || []).includes(concept);
                return (
                  <div key={concept} className="rounded-lg bg-[var(--surface-2)] px-2 py-1 text-xs">
                    {done ? "✅" : "○"} {concept}
                  </div>
                );
              })}
            </div>
            <div className="mt-3 flex gap-2">
              <Link to={`/learn?concept=${encodeURIComponent(selectedTopic.nextConcept || "")}&topic=${encodeURIComponent(selectedTopic.topicName)}`}>
                <Button>Open Learn</Button>
              </Link>
              <Link to="/roadmap/current">
                <Button variant="ghost">Current Topic</Button>
              </Link>
            </div>
          </>
        )}
      </Card>
      <div className="lg:col-span-3">
        <Link to="/roadmap/current">
          <Button className="mt-2">Continue This Topic</Button>
        </Link>
      </div>
    </div>
  );
}
