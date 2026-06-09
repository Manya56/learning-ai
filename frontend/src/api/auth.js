import api, { unwrap } from "./axios";

export const registerApi = (payload) => api.post("/api/auth/register", payload).then(unwrap);
export const loginApi = (payload) => api.post("/api/auth/login", payload).then(unwrap);
export const refreshApi = (payload) => api.post("/api/auth/refresh", payload).then(unwrap);
export const logoutApi = () => api.post("/api/auth/logout").then(unwrap);

// Password reset
// Request a reset link/code for the given email.
export const forgotPasswordApi = (payload) => api.post("/api/auth/forgot-password", payload).then(unwrap);
// Set a new password using the token received by email.
export const resetPasswordApi = (payload) => api.post("/api/auth/reset-password", payload).then(unwrap);
