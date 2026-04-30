import { useEffect, useState } from "react";
import { completeRevisionApi, getRevisionAllApi, getRevisionDueApi } from "../api/revision";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

export default function RevisionPage() {
  const [cards, setCards] = useState([]);
  const [allCards, setAllCards] = useState([]);
  const [tab, setTab] = useState("due");
  useEffect(() => {
    getRevisionDueApi().then((d) => setCards(Array.isArray(d) ? d : d?.cards || []));
    getRevisionAllApi().then((d) => setAllCards(Array.isArray(d) ? d : d?.cards || [])).catch(() => setAllCards([]));
  }, []);
  return (
    <div className="space-y-3">
      <div className="flex gap-2">
        <Button variant={tab === "due" ? "primary" : "secondary"} onClick={() => setTab("due")}>Due Cards</Button>
        <Button variant={tab === "all" ? "primary" : "secondary"} onClick={() => setTab("all")}>All Cards</Button>
      </div>
      {tab === "due" ? (
        cards.length === 0 ? (
          <Card>
            <p className="text-lg">🎉 No revisions due today!</p>
            <p className="text-sm text-[var(--text-muted)]">You are up to date. Come back tomorrow.</p>
          </Card>
        ) : (
          cards.map((card) => (
            <Card key={card.conceptName}>
              <h3 className="font-semibold">{card.conceptName}</h3>
              <p className="mb-2 text-sm text-[var(--text-muted)]">{card.topicGoal}</p>
              <p className="mb-2 text-xs text-[var(--text-muted)]">Retention: {card.retentionScore ?? 0}%</p>
              <div className="mb-3 h-2 rounded bg-[var(--surface-2)]">
                <div
                  className="h-2 rounded"
                  style={{
                    width: `${card.retentionScore ?? 0}%`,
                    background:
                      (card.retentionScore ?? 0) < 50
                        ? "var(--error)"
                        : (card.retentionScore ?? 0) < 75
                          ? "var(--warning)"
                          : "var(--success)",
                  }}
                />
              </div>
              <div className="flex flex-wrap gap-2">
                {[
                  { quality: 0, label: "😵 Blank" },
                  { quality: 2, label: "😕 Very Hard" },
                  { quality: 3, label: "😐 Hard" },
                  { quality: 4, label: "🙂 Good" },
                  { quality: 5, label: "😄 Perfect" },
                ].map((q) => (
                  <Button key={q.quality} variant="secondary" onClick={() => completeRevisionApi({ conceptName: card.conceptName, quality: q.quality })}>
                    {q.label}
                  </Button>
                ))}
              </div>
            </Card>
          ))
        )
      ) : (
        <Card>
          <p className="mb-2 font-semibold">All Revision Cards</p>
          <div className="space-y-2">
            {allCards.map((card) => (
              <div key={card.conceptName} className="grid grid-cols-4 gap-2 rounded-lg bg-[var(--surface-2)] p-2 text-xs">
                <span>{card.conceptName}</span>
                <span>{card.retentionScore ?? 0}%</span>
                <span>{card.nextReviewAt ? new Date(card.nextReviewAt).toLocaleDateString() : "-"}</span>
                <span>{card.status || "-"}</span>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
