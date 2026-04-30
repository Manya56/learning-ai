import { createBrowserRouter } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import AppShell from "../components/layout/AppShell";
import LandingPage from "../pages/LandingPage";
import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import OnboardingPage from "../pages/OnboardingPage";
import DashboardPage from "../pages/DashboardPage";
import RoadmapPage from "../pages/RoadmapPage";
import CurrentTopicPage from "../pages/CurrentTopicPage";
import LearnPage from "../pages/LearnPage";
import QuizPage from "../pages/QuizPage";
import PracticePage from "../pages/PracticePage";
import MentorPage from "../pages/MentorPage";
import RevisionPage from "../pages/RevisionPage";
import AnalyticsPage from "../pages/AnalyticsPage";
import ProfilePage from "../pages/ProfilePage";

export const router = createBrowserRouter([
  { path: "/", element: <LandingPage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      { path: "/onboarding", element: <OnboardingPage /> },
      {
        element: <AppShell />,
        children: [
          { path: "/dashboard", element: <DashboardPage /> },
          { path: "/roadmap", element: <RoadmapPage /> },
          { path: "/roadmap/current", element: <CurrentTopicPage /> },
          { path: "/learn", element: <LearnPage /> },
          { path: "/quiz", element: <QuizPage /> },
          { path: "/practice", element: <PracticePage /> },
          { path: "/mentor", element: <MentorPage /> },
          { path: "/revision", element: <RevisionPage /> },
          { path: "/analytics", element: <AnalyticsPage /> },
          { path: "/profile", element: <ProfilePage /> },
        ],
      },
    ],
  },
]);
