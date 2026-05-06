import { motion } from "framer-motion";

export default function AnimatedRoadmap({ topics = [], currentProgress = 0, compact = false, onTopicClick, showStatus = true }) {
  // Flatten topics and concepts into a single timeline
  const flattenItems = () => {
    const items = [];
    topics.forEach((topic, topicIdx) => {
      // Add topic header
      items.push({
        type: 'topic',
        id: `topic-${topicIdx}`,
        topicIdx,
        topicName: topic.topicName,
        status: topic.status,
        progressPercent: topic.progressPercent,
        concepts: topic.concepts || [],
        completedConcepts: topic.completedConcepts || []
      });

      // Add concepts within this topic
      (topic.concepts || []).forEach((concept, conceptIdx) => {
        const isCompleted = (topic.completedConcepts || []).includes(concept);
        items.push({
          type: 'concept',
          id: `concept-${topicIdx}-${conceptIdx}`,
          topicIdx,
          conceptIdx,
          conceptName: concept,
          isCompleted,
          topicName: topic.topicName,
          topicStatus: topic.status
        });
      });
    });
    return items;
  };

  const allItems = flattenItems();
  const totalItems = allItems.length;
  const completedItems = allItems.filter(item =>
    item.type === 'topic' ? item.status === 'COMPLETED' :
    item.type === 'concept' ? item.isCompleted : false
  ).length;
  const progressPercent = totalItems > 0 ? (completedItems / totalItems) * 100 : 0;

  const getStatusColor = (status) => {
    switch (status) {
      case "COMPLETED":
        return "bg-emerald-500";
      case "IN_PROGRESS":
        return "bg-sky-500";
      case "UNLOCKED":
        return "bg-violet-500";
      default:
        return "bg-slate-400";
    }
  };

  const getConceptColor = (isCompleted, topicStatus) => {
    if (isCompleted) return "bg-emerald-400";
    if (topicStatus === "IN_PROGRESS") return "bg-sky-400";
    if (topicStatus === "UNLOCKED") return "bg-violet-400";
    return "bg-slate-300";
  };

  const container = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.05,
        delayChildren: 0.2,
      },
    },
  };

  const item = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
  };

  if (compact) {
    return (
      <div className="w-full">
        {/* Horizontal Timeline */}
        <div className="relative mb-8">
          {/* Background Track */}
          <div className="absolute left-0 top-1/2 h-1 w-full -translate-y-1/2 rounded-full bg-slate-300 dark:bg-slate-700" />

          {/* Progress Track */}
          <motion.div
            className="absolute left-0 top-1/2 h-1 rounded-full bg-gradient-to-r from-sky-500 to-emerald-500 dark:from-sky-400 dark:to-emerald-400"
            style={{ width: `${progressPercent}%` }}
            initial={{ width: 0 }}
            animate={{ width: `${progressPercent}%` }}
            transition={{ duration: 1, ease: "easeOut" }}
          />

          {/* Items */}
          <div className="relative flex justify-between">
            {allItems.map((item, idx) => {
              const isTopic = item.type === 'topic';
              const isCompleted = isTopic ? item.status === "COMPLETED" : item.isCompleted;
              const isActive = isTopic ? item.status === "IN_PROGRESS" : false;

              return (
                <motion.div
                  key={item.id}
                  variants={item}
                  className="flex flex-col items-center"
                >
                  {/* Circle Node */}
                  <motion.div
                    className={`relative z-10 flex h-10 w-10 items-center justify-center rounded-full border-4 border-white dark:border-slate-800 shadow-lg ${isTopic ? getStatusColor(item.status) : getConceptColor(item.isCompleted, item.topicStatus)}`}
                    whileHover={{ scale: 1.2 }}
                    transition={{ duration: 0.3 }}
                  >
                    {isCompleted && <span className="text-lg text-white">✓</span>}
                    {isActive && (
                      <motion.div
                        className="h-4 w-4 rounded-full bg-white dark:bg-slate-800"
                        animate={{ scale: [1, 1.2, 1] }}
                        transition={{ duration: 1, repeat: Infinity }}
                      />
                    )}
                  </motion.div>

                  {/* Label */}
                  <p className="mt-2 text-center text-xs font-semibold text-slate-700 dark:text-slate-300">
                    {isTopic ? item.topicName : item.conceptName}
                  </p>
                  {isTopic && <p className="text-[10px] text-slate-500 dark:text-slate-400">
                    {item.progressPercent || 0}%
                  </p>}
                </motion.div>
              );
            })}
          </div>
        </div>

        {/* Car Animation */}
        <div className="relative -mt-2 mb-4 h-12">
          <motion.div
            className="absolute bottom-0 flex flex-col items-center"
            animate={{ x: `calc(${progressPercent}% - 20px)` }}
            transition={{ duration: 1.5, ease: "easeOut" }}
          >
            <motion.div
              className="text-2xl"
              animate={{ y: [0, -4, 0] }}
              transition={{ duration: 0.6, repeat: Infinity }}
            >
              🚗
            </motion.div>
          </motion.div>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      className="w-full space-y-4"
      variants={container}
      initial="hidden"
      animate="visible"
    >
      {/* Vertical Timeline */}
      <div className="relative">
        {/* Vertical Track */}
        <div className="absolute left-8 top-0 h-full w-1 rounded-full bg-slate-300 dark:bg-slate-700" />

        {/* Progress Track */}
        <motion.div
          className="absolute left-8 top-0 w-1 rounded-full bg-gradient-to-b from-sky-500 to-emerald-500 dark:from-sky-400 dark:to-emerald-400"
          style={{ height: `${progressPercent}%` }}
          initial={{ height: 0 }}
          animate={{ height: `${progressPercent}%` }}
          transition={{ duration: 1.5, ease: "easeOut" }}
        />

        {/* Items */}
        {allItems.map((item, idx) => {
          const isTopic = item.type === 'topic';
          const isCompleted = isTopic ? item.status === "COMPLETED" : item.isCompleted;
          const isActive = isTopic ? item.status === "IN_PROGRESS" : false;

          return (
            <motion.div
              key={item.id}
              variants={item}
              className={`relative mb-4 ml-24 flex items-center gap-4`}
            >
              {/* Timeline Circle */}
              <motion.div
                className={`absolute -left-16 flex h-16 w-16 items-center justify-center rounded-full border-4 border-white dark:border-slate-800 shadow-lg ${isTopic ? getStatusColor(item.status) : getConceptColor(item.isCompleted, item.topicStatus)}`}
                whileHover={{ scale: 1.15 }}
                transition={{ duration: 0.3 }}
              >
                {isCompleted && <span className={`text-2xl text-white`}>✓</span>}
                {isActive && (
                  <motion.div
                    className="h-6 w-6 rounded-full bg-white dark:bg-slate-800"
                    animate={{ scale: [1, 1.3, 1] }}
                    transition={{ duration: 1, repeat: Infinity }}
                  />
                )}
                {!isCompleted && !isActive && isTopic && (
                  <span className="text-xl font-bold text-white">{item.topicIdx + 1}</span>
                )}
              </motion.div>

              {/* Content Card */}
              <motion.div
                className={`flex-1 rounded-2xl border border-slate-200 dark:border-slate-700 bg-gradient-to-br ${isTopic ? 'from-slate-50 to-slate-100 dark:from-slate-900/50 dark:to-slate-800/50 p-4' : 'from-slate-100 to-slate-200 dark:from-slate-800/30 dark:to-slate-700/30 p-3'} shadow-sm`}
                whileHover={{ scale: 1.02, boxShadow: "0 20px 40px rgba(0,0,0,0.1)" }}
                onClick={() => isTopic && onTopicClick && onTopicClick(item.topicName)}
                style={{ cursor: isTopic && onTopicClick ? 'pointer' : 'default' }}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className={`font-semibold text-slate-900 dark:text-white`}>
                      {isTopic ? item.topicName : `• ${item.conceptName}`}
                    </h4>
                    {isTopic && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                      {item.concepts?.length || 0} concepts • {item.progressPercent || 0}% complete
                    </p>}
                    {!isTopic && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                      {item.topicName}
                    </p>}
                  </div>
                  <div className="text-right">
                    {isTopic && showStatus && <span
                      className={`inline-block rounded-full px-3 py-1 text-xs font-semibold text-white ${getStatusColor(item.status)}`}
                    >
                      {item.status || "LOCKED"}
                    </span>}
                    {!isTopic && showStatus && <span className={`inline-block rounded-full px-2 py-1 text-xs font-semibold text-white ${getConceptColor(item.isCompleted, item.topicStatus)}`}>
                      {item.isCompleted ? "DONE" : "PENDING"}
                    </span>}
                  </div>
                </div>

                {/* Progress Bar for Topics */}
                {isTopic && (
                  <div className="mt-3 h-2 rounded-full bg-slate-300 dark:bg-slate-700">
                    <motion.div
                      className="h-2 rounded-full bg-gradient-to-r from-sky-500 to-emerald-500 dark:from-sky-400 dark:to-emerald-400"
                      initial={{ width: 0 }}
                      animate={{ width: `${item.progressPercent || 0}%` }}
                      transition={{ duration: 1, ease: "easeOut", delay: 0.2 }}
                    />
                  </div>
                )}
              </motion.div>
            </motion.div>
          );
        })}

        {/* Finish Flag */}
        {allItems.length > 0 && (
          <motion.div
            variants={item}
            className="relative mb-4 ml-24 flex items-center gap-4"
          >
            <motion.div
              className="absolute -left-16 flex h-16 w-16 items-center justify-center rounded-full border-4 border-white dark:border-slate-800 bg-gradient-to-r from-emerald-400 to-teal-500 shadow-lg"
              animate={{ scale: progressPercent === 100 ? [1, 1.2, 1] : 1 }}
              transition={{ duration: 0.8, repeat: progressPercent === 100 ? Infinity : 0 }}
            >
              <span className="text-2xl">🏁</span>
            </motion.div>

            <div className="flex-1 rounded-2xl border-2 border-emerald-500 dark:border-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 p-4">
              <h4 className="font-semibold text-emerald-900 dark:text-emerald-300">
                Congratulations! 🎉
              </h4>
              <p className="mt-1 text-sm text-emerald-800 dark:text-emerald-200">
                Complete all topics and concepts to unlock mastery
              </p>
            </div>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}