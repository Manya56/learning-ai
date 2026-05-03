import api, { unwrap } from "./axios";

export const startQuizApi = (payload) => api.post("/api/quiz/start", payload).then(unwrap);
export const submitQuizAnswerApi = (sessionId, payload) =>
  api.post(`/api/quiz/${sessionId}/answer`, payload).then(unwrap);
export const completeQuizApi = (sessionId) => api.post(`/api/quiz/${sessionId}/complete`).then(unwrap);
export const getQuizHistoryApi = () => api.get("/api/quiz/history").then(unwrap);
export const getQuizHintApi = (sessionId, questionIndex, hintNumber = 1) =>
  api.get(`/api/quiz/${sessionId}/hint`, { params: { questionIndex, hintNumber } }).then(unwrap);
