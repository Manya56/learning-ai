import api, { unwrap } from "./axios";

export const getWeeklyLeaderboardApi = () => api.get("/api/leaderboard/weekly").then(unwrap);
export const getAllTimeLeaderboardApi = () => api.get("/api/leaderboard/all-time").then(unwrap);
export const getMyXpApi = () => api.get("/api/leaderboard/my-xp").then(unwrap);