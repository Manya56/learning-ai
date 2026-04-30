import api, { unwrap } from "./axios";

export const getRoadmapApi = () => api.get("/api/roadmap").then(unwrap);
export const getCurrentTopicApi = () => api.get("/api/roadmap/current-topic").then(unwrap);
export const getDailyPlanApi = () => api.get("/api/roadmap/daily-plan").then(unwrap);
