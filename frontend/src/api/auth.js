import api, { unwrap } from "./axios";

export const registerApi = (payload) => api.post("/api/auth/register", payload).then(unwrap);
export const loginApi = (payload) => api.post("/api/auth/login", payload).then(unwrap);
export const refreshApi = (payload) => api.post("/api/auth/refresh", payload).then(unwrap);
export const logoutApi = () => api.post("/api/auth/logout").then(unwrap);
