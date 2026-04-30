import api, { unwrap } from "./axios";

export const getProfileApi = () => api.get("/api/profile").then(unwrap);
export const updateProfileApi = (payload) => api.patch("/api/profile", payload).then(unwrap);
