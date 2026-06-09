import { lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import RootLayout from "../components/layout/RootLayout";
import ProtectedRoute from "./ProtectedRoute";
import AppShell from "../components/layout/AppShell";
import SpaceShell from "../components/layout/SpaceShell";

// Route-level code splitting: each page is loaded on demand.
const LandingPage = lazy(() => import("../pages/LandingPage"));
const LoginPage = lazy(() => import("../pages/LoginPage"));
const RegisterPage = lazy(() => import("../pages/RegisterPage"));
const VerifyOtpPage = lazy(() => import("../pages/VerifyOtpPage"));
const ResetPasswordPage = lazy(() => import("../pages/ResetPasswordPage"));
const OnboardingPage = lazy(() => import("../pages/OnboardingPage"));
const DashboardPage = lazy(() => import("../pages/DashboardPage"));
const RoadmapPage = lazy(() => import("../pages/RoadmapPage"));
const SpaceOverviewPage = lazy(() => import("../pages/SpaceOverviewPage"));
const LearnPage = lazy(() => import("../pages/LearnPage"));
const QuizPage = lazy(() => import("../pages/QuizPage"));
const PracticePage = lazy(() => import("../pages/PracticePage"));
const MentorPage = lazy(() => import("../pages/MentorPage"));
const RevisionPage = lazy(() => import("../pages/RevisionPage"));
const AnalyticsPage = lazy(() => import("../pages/AnalyticsPage"));
const ProfilePage = lazy(() => import("../pages/ProfilePage"));
const LeaderboardPage = lazy(() => import("../pages/LeaderboardPage"));

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { path: "/", element: <LandingPage /> },
      { path: "/login", element: <LoginPage /> },
      { path: "/register", element: <RegisterPage /> },
      { path: "/register/verify-otp", element: <VerifyOtpPage /> },
      { path: "/reset-password", element: <ResetPasswordPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          { path: "/onboarding", element: <OnboardingPage /> },
          {
            element: <AppShell />,
            children: [
              { path: "/dashboard", element: <DashboardPage /> },
              { path: "/roadmap", element: <RoadmapPage /> },
              { path: "/analytics", element: <AnalyticsPage /> },
              { path: "/profile", element: <ProfilePage /> },
              { path: "/leaderboard", element: <LeaderboardPage /> },
            ],
          },
          {
            path: "/space",
            element: <SpaceShell />,
            children: [
              { index: true, element: <SpaceOverviewPage /> },
              { path: "learn", element: <LearnPage /> },
              { path: "quiz", element: <QuizPage /> },
              { path: "practice", element: <PracticePage /> },
              { path: "mentor", element: <MentorPage /> },
              { path: "revision", element: <RevisionPage /> },
            ],
          },
        ],
      },
    ],
  },
]);
