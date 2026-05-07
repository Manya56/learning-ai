import { motion } from "framer-motion";

const getStatusColor = (status) => {
  switch (status) {
    case "COMPLETED":
      return "bg-emerald-500 text-white";
    case "IN_PROGRESS":
      return "bg-sky-500 text-white";
    case "UNLOCKED":
      return "bg-violet-500 text-white";
    default:
      return "bg-slate-400 text-white";
  }
};

const getProgressGradient = (status) => {
  switch (status) {
    case "COMPLETED":
      return "from-emerald-500 to-emerald-400";
    case "IN_PROGRESS":
      return "from-sky-500 to-sky-400";
    case "UNLOCKED":
      return "from-violet-500 to-fuchsia-500";
    default:
      return "from-slate-400 to-slate-300";
  }
};

const floatMotion = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.55, ease: "easeOut" } },
};

export default function AnimatedRoadmap({
  topics = [],
  currentProgress = 0,
  compact = false,
  onTopicClick,
  showStatus = true,
  showProgress = true,
  onboardingMode = false,
}) {
  const cards = topics.map((topic, idx) => ({
    id: `topic-${idx}`,
    topicName: topic.topicName,
    status: topic.status,
    progressPercent: topic.progressPercent || 0,
    concepts: topic.concepts || [],
    completedConcepts: topic.completedConcepts || [],
    topicIdx: idx,
  }));

  const progressWidth = Math.min(100, Math.max(0, currentProgress));

  const progressLabel = (card) =>
    showProgress
      ? `${card.concepts.length} concept${card.concepts.length === 1 ? "" : "s"} • ${card.progressPercent}% complete.`
      : `${card.concepts.length} concept${card.concepts.length === 1 ? "" : "s"}`;

  if (compact) {
    return (
      <div className="relative overflow-hidden rounded-[2rem] border border-slate-200/70 bg-slate-50/90 p-5 shadow-xl shadow-slate-900/5 dark:border-slate-700/70 dark:bg-slate-900/80">
        <div className="pointer-events-none absolute -top-10 right-6 h-40 w-40 rounded-full bg-sky-500/10 blur-3xl" />
        <div className="pointer-events-none absolute left-4 top-8 h-28 w-28 rounded-full bg-violet-500/10 blur-3xl" />

        {showProgress && (
          <div className="relative mb-6 h-2 rounded-full bg-slate-200 dark:bg-slate-700">
            <motion.div
              className={`absolute inset-y-0 rounded-full bg-gradient-to-r ${getProgressGradient("IN_PROGRESS")}`}
              style={{ width: `${progressWidth}%` }}
              initial={{ width: 0 }}
              animate={{ width: `${progressWidth}%` }}
              transition={{ duration: 1.2, ease: "easeOut" }}
            />
          </div>
        )}

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
          {cards.map((card, idx) => (
            <motion.div
              key={card.id}
              variants={floatMotion}
              initial="hidden"
              animate="visible"
              transition={{ delay: idx * 0.08 }}
              className="relative rounded-[1.75rem] border border-slate-200/70 bg-white/90 p-4 text-left shadow-lg shadow-slate-900/5 backdrop-blur-sm dark:border-slate-700/70 dark:bg-slate-950/80"
            >
              <div className="absolute -top-4 right-4 h-10 w-10 rounded-full bg-sky-500/10 dark:bg-sky-500/15" />
              <div className="relative z-10 space-y-3">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400 dark:text-slate-500">
                    Topic
                  </p>
                  {showStatus && (
                    <span className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold ${getStatusColor(card.status)}`}>
                      {card.status || "LOCKED"}
                    </span>
                  )}
                </div>
                <h3 className="text-base font-semibold text-slate-900 dark:text-white">
                  {card.topicName}
                </h3>
                <p className="text-sm leading-6 text-slate-600 dark:text-slate-400">
                  {progressLabel(card)}
                </p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <motion.div
      className="relative overflow-hidden rounded-[2rem] border border-slate-200/70 bg-gradient-to-br from-slate-50 via-white to-slate-50 p-6 shadow-2xl shadow-slate-900/5 dark:border-slate-700/70 dark:from-slate-950 dark:via-slate-900 dark:to-slate-800"
      initial="hidden"
      animate="visible"
      variants={{ hidden: { opacity: 0 }, visible: { opacity: 1, transition: { staggerChildren: 0.08 } } }}
    >
      <div className="pointer-events-none absolute -top-10 right-10 h-72 w-72 rounded-full bg-violet-500/10 blur-3xl" />
      <div className="pointer-events-none absolute left-0 top-16 h-56 w-56 rounded-full bg-sky-500/10 blur-3xl" />

      {onboardingMode && (
        <div className="relative mb-10 rounded-[2rem] bg-white/70 p-4 shadow-lg shadow-slate-900/10 ring-1 ring-slate-200 dark:bg-slate-950/70 dark:ring-slate-700">
          <div className="relative overflow-hidden rounded-full bg-slate-200/70 dark:bg-slate-800/80 h-2">
            <motion.div
              className="absolute inset-y-0 rounded-full bg-gradient-to-r from-sky-500 via-violet-500 to-fuchsia-500"
              initial={{ x: "-100%" }}
              animate={{ x: ["-100%", "100%", "-100%"] }}
              transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
            />
          </div>
          <div className="mt-4 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-sky-500/10 text-lg text-sky-700 dark:bg-sky-500/15 dark:text-sky-200">
              🚀
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900 dark:text-white">Roadmap Stream</p>
              <p className="text-sm text-slate-500 dark:text-slate-400">This connects your learning path and shows where you’ll reach next.</p>
            </div>
          </div>
        </div>
      )}

      <div className="relative z-10 grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map((card, idx) => (
          <motion.button
            key={card.id}
            type="button"
            className="group relative overflow-hidden rounded-[2rem] border border-slate-200/80 bg-white/90 p-6 text-left shadow-2xl shadow-slate-900/5 transition transform hover:-translate-y-1 hover:shadow-2xl focus:outline-none dark:border-slate-700/80 dark:bg-slate-950/85"
            variants={floatMotion}
            initial="hidden"
            animate="visible"
            transition={{ delay: idx * 0.08 }}
            onClick={() => card.topicName && onTopicClick && onTopicClick(card.topicName)}
            style={{ cursor: onTopicClick ? "pointer" : "default" }}
          >
            <div className="absolute inset-x-6 top-0 h-16 rounded-b-[2rem] bg-gradient-to-br from-sky-100/80 to-violet-100/80 blur-2xl" />
            <div className="relative z-10 space-y-4">
              <div className="flex items-center justify-between gap-3">
                <span className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-700 dark:text-slate-200">
                  Topic {card.topicIdx + 1}
                </span>
                {showStatus && (
                  <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getStatusColor(card.status)}`}>
                    {card.status || "LOCKED"}
                  </span>
                )}
              </div>
              <h3 className="text-2xl font-semibold text-slate-900 dark:text-white">
                {card.topicName}
              </h3>
              <p className="text-sm leading-6 text-slate-600 dark:text-slate-400">
                {progressLabel(card)}
              </p>

              {showProgress && (
                <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                  <motion.div
                    className={`h-full rounded-full bg-gradient-to-r ${getProgressGradient(card.status)}`}
                    initial={{ width: 0 }}
                    animate={{ width: `${card.progressPercent}%` }}
                    transition={{ duration: 1, ease: "easeOut" }}
                  />
                </div>
              )}
            </div>
          </motion.button>
        ))}
      </div>

      {!onboardingMode && (
        <div className="relative mt-8 rounded-[2rem] border border-slate-200/70 bg-slate-100/80 p-5 text-slate-700 shadow-inner shadow-slate-900/5 dark:border-slate-700/70 dark:bg-slate-900/80 dark:text-slate-300">
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="flex items-center gap-3 rounded-3xl bg-white/90 p-3 shadow-sm dark:bg-slate-950/80">
              <span className="flex h-3 w-3 rounded-full bg-emerald-500" />
              <span className="text-sm">Completed topics</span>
            </div>
            <div className="flex items-center gap-3 rounded-3xl bg-white/90 p-3 shadow-sm dark:bg-slate-950/80">
              <span className="flex h-3 w-3 rounded-full bg-sky-500" />
              <span className="text-sm">In progress</span>
            </div>
            <div className="flex items-center gap-3 rounded-3xl bg-white/90 p-3 shadow-sm dark:bg-slate-950/80">
              <span className="flex h-3 w-3 rounded-full bg-violet-500" />
              <span className="text-sm">Unlocked next</span>
            </div>
          </div>
        </div>
      )}
    </motion.div>
  );
}
