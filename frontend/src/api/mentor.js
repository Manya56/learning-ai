import api, { unwrap } from "./axios";

export const chatMentorApi = (payload) => api.post("/api/mentor/chat", payload).then(unwrap);
export const mentorHistoryApi = () => api.get("/api/mentor/history").then(unwrap);
