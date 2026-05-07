import { create } from "zustand";

export const useAuthStore = create((set) => ({
  user: null,
  accessToken: localStorage.getItem("accessToken"),
  refreshToken: localStorage.getItem("refreshToken"),
  isAuthenticated: Boolean(localStorage.getItem("accessToken")),
  login: (authResponse) => {
    localStorage.setItem("accessToken", authResponse.accessToken);
    localStorage.setItem("refreshToken", authResponse.refreshToken);
    set({
      user: authResponse.user ?? null,
      accessToken: authResponse.accessToken,
      refreshToken: authResponse.refreshToken,
      isAuthenticated: true,
    });
  },
  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
  },
}));
