import api, { unwrap } from "./axios";

export const explainApi = (payload) => api.post("/api/learn/explain", payload).then(unwrap);
