import api, { unwrap } from "./axios";

export const getRevisionDueApi = () => api.get("/api/revision/due").then(unwrap);
export const completeRevisionApi = (payload) => api.post("/api/revision/complete", payload).then(unwrap);
export const getRevisionAllApi = () => api.get("/api/revision/all").then(unwrap);
