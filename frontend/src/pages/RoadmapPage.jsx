import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import RoadmapPath from "../components/ui/RoadmapPath";
import Icon from "../components/ui/Icon";
import { motion, AnimatePresence } from "framer-motion";

const STATUS_META = {
  COMPLETED: { label: "Completed", pill: "bg-[var(--accent-light)] text-[var(--accent-hover)]" },
  IN_PROGRESS: { label: "In progress", pill: "bg-[var(--accent)] text-white" },
  UNLOCKED: { label: "Unlocked", pill: "bg-[var(--surface-2)] text-[var(--text)]" },
  LOCKED: { label: "Locked", pill: "bg-[var(--surface-2)] text-[var(--text-muted)]" },
};

export default function RoadmapPage() {
  const navigate = useNavigate();
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopic, setSelectedTopic] = useState(null);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    getRoadmapApi().then(setRoadmap).catch(() => setRoadmap({ topics: [] }));
  }, []);

  const topics = roadmap?.topics || [];
  const completedCount = topics.filter((t) => t.status === "COMPLETED").length;
  const overallProgress = roadmap?.overallProgressPercent ?? 0;

  // Click a topic → open the details popup (as before).
  const handleTopicSelect = (topicName) => {
    const topic = topics.find((t) => t.topicName === topicName);
    if (topic) {
      setSelectedTopic(topic);
      setShowModal(true);
    }
  };

  const selectedMeta = STATUS_META[selectedTopic?.status] || STATUS_META.LOCKED;
  const selectedLocked = (selectedTopic?.status || "LOCKED") === "LOCKED";

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      {/* Header */}
      <Card>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-extrabold tracking-tight">Your Learning Journey</h1>
            <p className="mt-1 text-sm font-medium text-[var(--text-muted)]">
              {completedCount} of {topics.length} topics completed
            </p>
          </div>
          <span className="text-3xl font-extrabold text-[var(--accent)]">{overallProgress}%</span>
        </div>
        <div className="mt-4 h-3 overflow-hidden rounded-full bg-[var(--surface-2)]">
          <motion.div
            className="h-full rounded-full bg-[var(--accent)]"
            initial={{ width: 0 }}
            animate={{ width: `${overallProgress}%` }}
            transition={{ duration: 0.8, ease: "easeOut" }}
          />
        </div>
      </Card>

      {/* The roadmap path */}
      {!roadmap ? (
        <Card>
          <div className="h-72 animate-pulse rounded-2xl bg-[var(--surface-2)]" />
        </Card>
      ) : topics.length ? (
        <Card>
          <RoadmapPath topics={topics} onTopicClick={handleTopicSelect} />
        </Card>
      ) : (
        <Card>
          <p className="text-sm font-medium text-[var(--text-muted)]">No roadmap yet. Complete onboarding to generate your path.</p>
        </Card>
      )}

      {/* Topic details popup */}
      <AnimatePresence>
        {showModal && selectedTopic && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
            onClick={() => setShowModal(false)}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.96 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.96 }}
              onClick={(e) => e.stopPropagation()}
              className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-6"
            >
              <div className="mb-5 flex items-start justify-between gap-3">
                <div>
                  <span className={`rounded-full px-3 py-1 text-xs font-bold ${selectedMeta.pill}`}>{selectedMeta.label}</span>
                  <h2 className="mt-3 text-2xl font-extrabold tracking-tight">{selectedTopic.topicName}</h2>
                  <p className="mt-1 text-sm font-medium text-[var(--text-muted)]">
                    {selectedTopic.concepts?.length || 0} concepts • {selectedTopic.progressPercent || 0}% complete
                  </p>
                </div>
                <button
                  onClick={() => setShowModal(false)}
                  className="rounded-full border-2 border-[var(--border)] p-1.5 text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
                >
                  <Icon name="close" size={18} />
                </button>
              </div>

              <div className="mb-6 h-3 overflow-hidden rounded-full bg-[var(--surface-2)]">
                <div className="h-full rounded-full bg-[var(--accent)]" style={{ width: `${selectedTopic.progressPercent || 0}%` }} />
              </div>

              <h3 className="mb-3 text-sm font-extrabold uppercase tracking-wide text-[var(--text-muted)]">Concepts to master</h3>
              <div className="mb-6 space-y-2">
                {(selectedTopic.concepts || []).map((concept, idx) => {
                  const done = (selectedTopic.completedConcepts || []).includes(concept);
                  return (
                    <div
                      key={idx}
                      className={`flex items-center gap-3 rounded-xl border-2 p-3 ${
                        done ? "border-[var(--accent)]/30 bg-[var(--accent-light)]" : "border-[var(--border)] bg-[var(--surface-2)]"
                      }`}
                    >
                      <Icon name={done ? "check_circle" : "radio_button_unchecked"} size={20} fill={done ? 1 : 0} className={done ? "text-[var(--accent)]" : "text-[var(--text-muted)]"} />
                      <span className={`font-bold ${done ? "text-[var(--accent-hover)]" : "text-[var(--text)]"}`}>{concept}</span>
                    </div>
                  );
                })}
                {(selectedTopic.concepts || []).length === 0 && (
                  <p className="text-sm font-medium text-[var(--text-muted)]">No concepts listed for this topic.</p>
                )}
              </div>

              {/* Action → enter the learning space */}
              <div className="flex flex-col gap-3 sm:flex-row">
                {!selectedLocked && (
                  <Button className="flex-1 gap-1.5" onClick={() => navigate("/space")}>
                    <Icon name="play_arrow" size={18} fill={1} />
                    {selectedTopic.status === "COMPLETED" ? "Review in space" : "Enter learning space"}
                  </Button>
                )}
                <Button variant="ghost" onClick={() => setShowModal(false)} className="w-full sm:w-auto">
                  Close
                </Button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
