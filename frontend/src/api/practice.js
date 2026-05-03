import api, { unwrap } from "./axios";

export const generatePracticeApi = (payload) => api.post("/api/practice/generate", payload).then(unwrap);
export const evaluatePracticeApi = (payload) => api.post("/api/practice/evaluate", payload).then(unwrap);
export const getCodingHistoryApi = () => api.get("/api/practice/history").then(unwrap);
export const getCodingConceptHistoryApi = (conceptName) => api.get(`/api/practice/history/${conceptName}`).then(unwrap);
