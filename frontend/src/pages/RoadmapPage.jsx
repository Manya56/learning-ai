import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getRoadmapApi } from "../api/roadmap";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import AnimatedRoadmap from "../components/ui/AnimatedRoadmap";
import { motion, AnimatePresence } from "framer-motion";

export default function RoadmapPage() {
  const [roadmap, setRoadmap] = useState(null);
  const [selectedTopic, setSelectedTopic] = useState(null);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    getRoadmapApi().then(setRoadmap).catch(() => setRoadmap({ topics: [] }));
  }, []);

  const completedCount = (roadmap?.topics || []).filter((t) => t.status === "COMPLETED").length;

  const handleTopicSelect = (topicName) => {
    const topic = (roadmap?.topics || []).find((t) => t.topicName === topicName);
    if (topic) {
      setSelectedTopic(topic);
      setShowModal(true);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "COMPLETED":
        return "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300";
      case "IN_PROGRESS":
        return "bg-sky-100 text-sky-800 dark:bg-sky-950/50 dark:text-sky-300";
      case "UNLOCKED":
        return "bg-violet-100 text-violet-800 dark:bg-violet-950/50 dark:text-violet-300";
      default:
        return "bg-slate-100 text-slate-800 dark:bg-slate-900/50 dark:text-slate-300";
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="rounded-3xl bg-gradient-to-r from-indigo-600 via-purple-700 to-pink-700 p-6 text-white shadow-2xl shadow-slate-900/30">
        <h1 className="text-4xl font-bold tracking-tight">Your Learning Journey</h1>
        <p className="mt-2 text-sm text-slate-200">
          {completedCount} of {roadmap?.topics?.length || 0} topics completed • {roadmap?.overallProgressPercent || 0}% progress
        </p>
      </div>

      {/* Animated Roadmap Journey */}
      {roadmap?.topics && (
        <Card className="rounded-3xl p-6 lg:p-8">
          <AnimatedRoadmap
            topics={roadmap.topics || []}
            currentProgress={completedCount}
            onTopicClick={handleTopicSelect}
          />
        </Card>
      )}

      {/* Loading State */}
      {!roadmap && (
        <Card className="rounded-3xl p-8">
          <div className="h-64 animate-pulse rounded-lg bg-slate-300 dark:bg-slate-700" />
        </Card>
      )}

      {/* Topic Details Modal */}
      <AnimatePresence>
        {showModal && selectedTopic && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white dark:bg-slate-900 shadow-2xl border border-slate-200 dark:border-slate-800"
            >
              <div className="p-6 lg:p-8">
                {/* Header */}
                <div className="mb-6 flex items-start justify-between">
                  <div>
                    <h2 className="text-3xl font-bold text-slate-900 dark:text-white">{selectedTopic.topicName}</h2>
                    <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                      {selectedTopic.progressPercent || 0}% complete • {selectedTopic.concepts?.length || 0} concepts
                    </p>
                  </div>
                  <button
                    onClick={() => setShowModal(false)}
                    className="rounded-full border border-slate-300 dark:border-slate-700 p-2 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
                  >
                    ✕
                  </button>
                </div>

                {/* Status Badge */}
                <div className="mb-6">
                  <span className={`inline-block rounded-full px-4 py-2 text-sm font-semibold ${getStatusColor(selectedTopic.status)}`}>
                    {selectedTopic.status || "LOCKED"}
                  </span>
                </div>

                {/* Progress Bar */}
                <div className="mb-6">
                  <div className="mb-2 flex justify-between text-xs font-semibold text-slate-700 dark:text-slate-300">
                    <span>Progress</span>
                    <span>{selectedTopic.progressPercent || 0}%</span>
                  </div>
                  <div className="h-3 rounded-full bg-slate-200 dark:bg-slate-800">
                    <motion.div
                      className="h-3 rounded-full bg-gradient-to-r from-sky-500 to-emerald-500"
                      initial={{ width: 0 }}
                      animate={{ width: `${selectedTopic.progressPercent || 0}%` }}
                      transition={{ duration: 0.6, ease: "easeOut" }}
                    />
                  </div>
                </div>

                {/* Concepts */}
                <div className="mb-6">
                  <h3 className="mb-3 text-lg font-semibold text-slate-900 dark:text-white">Concepts to Master</h3>
                  <div className="space-y-2">
                    {(selectedTopic.concepts || []).map((concept, idx) => {
                      const done = (selectedTopic.completedConcepts || []).includes(concept);
                      return (
                        <motion.div
                          key={idx}
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: idx * 0.05 }}
                          className={`rounded-lg border-2 p-3 flex items-center gap-3 ${
                            done
                              ? "border-emerald-300 bg-emerald-50 dark:border-emerald-700 dark:bg-emerald-950/30"
                              : "border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-900/30"
                          }`}
                        >
                          <span className="text-xl">{done ? "✅" : "⭕"}</span>
                          <span className={`font-medium ${done ? "text-emerald-700 dark:text-emerald-300" : "text-slate-700 dark:text-slate-300"}`}>
                            {concept}
                          </span>
                        </motion.div>
                      );
                    })}
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-3">
                  {selectedTopic.status !== "LOCKED" && (
                    <Link to={`/learn?concept=${encodeURIComponent(selectedTopic.nextConcept || "")}&topic=${encodeURIComponent(selectedTopic.topicName)}`} className="flex-1">
                      <Button className="w-full">Learn This Topic</Button>
                    </Link>
                  )}
                  {selectedTopic.status === "IN_PROGRESS" && (
                    <Link to="/roadmap/current" className="flex-1">
                      <Button variant="outline" className="w-full">Continue Current</Button>
                    </Link>
                  )}
                  <Button
                    variant="ghost"
                    onClick={() => setShowModal(false)}
                    className="w-full"
                  >
                    Close
                  </Button>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
