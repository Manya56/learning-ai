import { create } from "zustand";
import { getProfileApi } from "../api/profile";
import { getCurrentTopicApi, getRoadmapApi } from "../api/roadmap";
import { getRevisionDueApi } from "../api/revision";

export const useProfileStore = create((set) => ({
  profile: null,
  roadmapState: null,
  currentTopic: null,
  revisionDueCount: 0,
  setProfile: (profile) => set({ profile }),
  loadProfile: async () => set({ profile: await getProfileApi() }),
  loadRoadmap: async () => set({ roadmapState: await getRoadmapApi() }),
  loadCurrentTopic: async () => set({ currentTopic: await getCurrentTopicApi() }),
  loadRevisionCount: async () => {
    const due = await getRevisionDueApi();
    set({ revisionDueCount: Array.isArray(due) ? due.length : due?.dueToday ?? 0 });
  },
}));
