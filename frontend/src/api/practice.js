import api, { unwrap } from "./axios";

export const generatePracticeApi = (payload) => api.post("/api/practice/generate", payload).then(unwrap);
export const evaluatePracticeApi = (payload) => api.post("/api/practice/evaluate", payload).then(unwrap);
