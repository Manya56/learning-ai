import axios from "axios";
import toast from "react-hot-toast";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => {
    // Temporary debug logging to inspect backend payloads during integration testing.
    console.log(`[API OK] ${String(response?.config?.method || "GET").toUpperCase()} ${response?.config?.url}`, response?.data);
    return response;
  },
  (error) => {
    const status = error?.response?.status;
    console.log(
      `[API ERR] ${String(error?.config?.method || "GET").toUpperCase()} ${error?.config?.url}`,
      error?.response?.data || error?.message
    );
    if (status === 401) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      window.location.href = "/login";
    } else if (status === 429) {
      toast.error("Too many requests — wait a moment");
    } else if (status >= 500) {
      toast.error("Server error — try again");
    }
    return Promise.reject(error);
  }
);

export const unwrap = (response) => response?.data?.data;
export default api;
