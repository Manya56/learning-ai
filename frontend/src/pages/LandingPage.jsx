import { Link } from "react-router-dom";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

export default function LandingPage() {
  return (
    <div className="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center px-4">
      <h1 className="text-4xl font-semibold tracking-tight">LearnAI</h1>
      <p className="mt-3 text-xl">Learn anything. Adapted to you.</p>
      <p className="mt-2 max-w-2xl text-center text-[var(--text-muted)]">
        AI-powered roadmaps, quizzes, and practice problems for any topic.
      </p>
      <div className="mt-6 flex gap-3">
        <Link to="/register">
          <Button>Get Started</Button>
        </Link>
        <Link to="/login">
          <Button variant="ghost">Sign In</Button>
        </Link>
      </div>
      <div className="mt-12 grid w-full gap-4 md:grid-cols-3">
        {["Personalized Roadmap", "Adaptive Quizzes", "AI Mentor Aria"].map((x) => (
          <Card key={x}>
            <h3 className="font-semibold">{x}</h3>
          </Card>
        ))}
      </div>
    </div>
  );
}
