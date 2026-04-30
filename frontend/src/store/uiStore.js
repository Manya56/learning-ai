import { create } from "zustand";

export const useUiStore = create((set) => ({
  sidebarOpen: true,
  activeQuizSessionId: sessionStorage.getItem("activeQuizSessionId"),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),
  setActiveQuizSession: (id) => {
    sessionStorage.setItem("activeQuizSessionId", id);
    set({ activeQuizSessionId: id });
  },
  clearQuizSession: () => {
    sessionStorage.removeItem("activeQuizSessionId");
    set({ activeQuizSessionId: null });
  },
}));
