export const pickConceptName = (item = {}) => {
  if (typeof item === "string") return item.trim();
  return item?.conceptName || item?.concept || item?.concept_name || item?.name || "";
};

export const pickTopicName = (item = {}) =>
  item?.topicGoal || item?.topicName || item?.topic || item?.topic_name || "";

export const parseApiBody = (apiBody) => {
  if (!apiBody) return {};
  if (typeof apiBody === "object") return apiBody;
  if (typeof apiBody !== "string") return {};
  try {
    return JSON.parse(apiBody);
  } catch {
    return {};
  }
};

const normalStatus = (value) => String(value || "").toUpperCase();

export const isSelectableConcept = (concept = {}) => {
  if (typeof concept === "string") return Boolean(concept.trim());
  const status = normalStatus(concept.status);
  if (status === "LOCKED") return false;
  if (concept.completed === true) return false;
  if (!status) return true;
  return status === "CURRENT" || status === "IN_PROGRESS" || status === "UNLOCKED";
};

export const getSelectableConcepts = (concepts = []) => concepts.filter(isSelectableConcept);

export const getSelectableConceptNames = (concepts = []) =>
  Array.from(
    new Set(
      getSelectableConcepts(concepts)
        .map((item) => pickConceptName(item))
        .filter(Boolean)
    )
  );

export const getPreferredConcept = (concepts = []) => {
  const selectable = getSelectableConcepts(concepts);
  const byPriority = ["CURRENT", "IN_PROGRESS", "UNLOCKED"];
  for (const status of byPriority) {
    const match = selectable.find((c) => normalStatus(c.status) === status);
    if (match) return pickConceptName(match);
  }
  return getSelectableConceptNames(concepts)[0] || "";
};

export const getConceptPoolFromTopic = (topic = {}) => {
  if (Array.isArray(topic?.remainingConcepts) && topic.remainingConcepts.length) {
    return topic.remainingConcepts;
  }
  return topic?.concepts || [];
};

export const getSelectableTopics = (topics = []) =>
  topics.filter((topic) => ["UNLOCKED", "IN_PROGRESS", "COMPLETED"].includes(String(topic?.status || "").toUpperCase()));
