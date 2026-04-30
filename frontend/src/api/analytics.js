import api, { unwrap } from "./axios";

export const analyticsOverviewApi = () => api.get("/api/analytics/overview").then(unwrap);
export const analyticsHeatmapApi = () => api.get("/api/analytics/heatmap").then(unwrap);
export const analyticsWeakConceptsApi = () => api.get("/api/analytics/weak-concepts").then(unwrap);
export const analyticsWeeklyApi = () => api.get("/api/analytics/weekly").then(unwrap);
