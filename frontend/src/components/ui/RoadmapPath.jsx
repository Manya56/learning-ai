import { motion } from "framer-motion";
import Icon from "./Icon";

// A real, sequential learning path: connected circular nodes down a spine,
// each with a topic card. Minimalist — one accent color + gray.
const STATUS = {
  COMPLETED: { label: "Completed", pill: "bg-[var(--accent-light)] text-[var(--accent-hover)]" },
  IN_PROGRESS: { label: "In progress", pill: "bg-[var(--accent)] text-white" },
  UNLOCKED: { label: "Unlocked", pill: "bg-[var(--surface-2)] text-[var(--text)]" },
  LOCKED: { label: "Locked", pill: "bg-[var(--surface-2)] text-[var(--text-muted)]" },
};

function Node({ status, index }) {
  if (status === "COMPLETED") {
    return (
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--accent)] text-white shadow-[0_3px_0_0_var(--accent-hover)]">
        <Icon name="check_circle" size={24} fill={1} />
      </div>
    );
  }
  if (status === "IN_PROGRESS") {
    return (
      <div className="relative flex h-12 w-12 items-center justify-center rounded-full bg-[var(--accent)] font-extrabold text-white shadow-[0_3px_0_0_var(--accent-hover)]">
        <motion.span
          className="absolute inset-0 rounded-full ring-2 ring-[var(--accent)]"
          animate={{ scale: [1, 1.35], opacity: [0.6, 0] }}
          transition={{ duration: 1.6, repeat: Infinity, ease: "easeOut" }}
        />
        {index + 1}
      </div>
    );
  }
  if (status === "UNLOCKED") {
    return (
      <div className="flex h-12 w-12 items-center justify-center rounded-full border-2 border-[var(--accent)] bg-[var(--surface)] font-extrabold text-[var(--accent-hover)]">
        {index + 1}
      </div>
    );
  }
  return (
    <div className="flex h-12 w-12 items-center justify-center rounded-full border-2 border-[var(--border)] bg-[var(--surface-2)] text-[var(--text-muted)]">
      <Icon name="lock" size={20} />
    </div>
  );
}

export default function RoadmapPath({ topics = [], onTopicClick }) {
  return (
    <motion.div
      initial="hidden"
      animate="visible"
      variants={{ hidden: { opacity: 0 }, visible: { opacity: 1, transition: { staggerChildren: 0.06 } } }}
    >
      {topics.map((topic, idx) => {
        const status = topic.status || "LOCKED";
        const meta = STATUS[status] || STATUS.LOCKED;
        const isLast = idx === topics.length - 1;
        const isCurrent = status === "IN_PROGRESS";
        const isLocked = status === "LOCKED";
        const conceptCount = topic.concepts?.length || 0;

        return (
          <motion.div
            key={topic.topicName || idx}
            variants={{ hidden: { opacity: 0, y: 16 }, visible: { opacity: 1, y: 0 } }}
            className="flex gap-4 sm:gap-5"
          >
            {/* Spine: node + connector */}
            <div className="flex shrink-0 flex-col items-center">
              <Node status={status} index={idx} />
              {!isLast && (
                <div
                  className={`my-1 w-1 flex-1 rounded-full ${
                    status === "COMPLETED" ? "bg-[var(--accent)]" : "bg-[var(--border)]"
                  }`}
                />
              )}
            </div>

            {/* Topic card */}
            <button
              type="button"
              onClick={() => topic.topicName && onTopicClick?.(topic.topicName)}
              className={`mb-5 flex min-w-0 flex-1 items-center gap-4 rounded-2xl border-2 p-4 text-left transition-all duration-150 focus:outline-none ${
                isCurrent
                  ? "border-[var(--accent)] bg-[var(--accent-light)]"
                  : isLocked
                    ? "border-[var(--border)] bg-[var(--surface-2)]"
                    : "border-[var(--border)] bg-[var(--surface)] hover:-translate-y-0.5 hover:border-[var(--accent)]"
              }`}
            >
              <div className="min-w-0 flex-1">
                <div className="mb-1 flex flex-wrap items-center gap-2">
                  <span className="text-[10px] font-extrabold uppercase tracking-widest text-[var(--text-muted)]">
                    Topic {idx + 1}
                  </span>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${meta.pill}`}>{meta.label}</span>
                </div>
                <h3 className={`break-words text-lg font-extrabold leading-snug ${isLocked ? "text-[var(--text-muted)]" : "text-[var(--text)]"}`}>
                  {topic.topicName || "Untitled topic"}
                </h3>
                <p className="mt-0.5 text-xs font-medium text-[var(--text-muted)]">
                  {conceptCount} concept{conceptCount === 1 ? "" : "s"}
                  {!isLocked ? ` • ${topic.progressPercent || 0}% complete` : ""}
                </p>
                {!isLocked && (
                  <div className="mt-2 h-2 overflow-hidden rounded-full bg-[var(--surface-2)]">
                    <motion.div
                      className="h-full rounded-full bg-[var(--accent)]"
                      initial={{ width: 0 }}
                      animate={{ width: `${topic.progressPercent || 0}%` }}
                      transition={{ duration: 0.8, ease: "easeOut" }}
                    />
                  </div>
                )}
              </div>
              <Icon
                name="chevron_right"
                size={22}
                className={`shrink-0 ${isLocked ? "text-[var(--text-muted)]" : "text-[var(--accent)]"}`}
              />
            </button>
          </motion.div>
        );
      })}
    </motion.div>
  );
}
