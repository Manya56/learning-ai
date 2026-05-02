import api, { unwrap } from "./axios";

export const sendRevisionNotificationTestApi = () => api.post("/api/notifications/test/revision").then(unwrap);
export const sendStreakNotificationTestApi = () => api.post("/api/notifications/test/streak").then(unwrap);
export const sendMotivationNotificationTestApi = () => api.post("/api/notifications/test/motivation").then(unwrap);
