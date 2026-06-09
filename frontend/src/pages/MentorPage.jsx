import { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { chatMentorApi } from "../api/mentor";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Icon from "../components/ui/Icon";
import WizardStepHeader from "../components/ui/WizardStepHeader";
import Chip from "../components/ui/Chip";

export default function MentorPage() {
  const navigate = useNavigate();
  const { concept } = useOutletContext() || {};
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [sending, setSending] = useState(false);

  const suggestions = concept
    ? [`Explain ${concept} simply`, `Give me an example of ${concept}`, `Quiz me on ${concept}`, `Why does ${concept} matter?`]
    : ["Explain this simply", "Give me an example", "Quiz me on this", "Why does this matter?"];

  const send = async (text) => {
    const content = (text ?? message).trim();
    if (!content || sending) return;
    setSending(true);
    setMessages((prev) => [...prev, { role: "user", content }]);
    setMessage("");
    try {
      const res = await chatMentorApi({ message: content, personality: "ENCOURAGING", newSession: false });
      setMessages((prev) => [...prev, { role: "assistant", content: res.reply || "..." }]);
    } catch {
      setMessages((prev) => [...prev, { role: "system", content: "Something went wrong. Try again." }]);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-4">
      <WizardStepHeader concept={concept} showSteps={false} />

      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[var(--accent-light)]">
          <Icon name="psychology" size={24} className="text-[var(--accent)]" />
        </div>
        <div>
          <h2 className="text-lg font-extrabold tracking-tight">Aria — your AI mentor</h2>
          <p className="text-sm font-medium text-[var(--text-muted)]">
            {concept ? `Ask anything about ${concept}.` : "Ask anything about what you're learning."}
          </p>
        </div>
      </div>

      <Card>
        {messages.length === 0 ? (
          <div className="py-6 text-center">
            <p className="text-sm font-bold text-[var(--text)]">How can I help you today?</p>
            <div className="mt-4 flex flex-wrap justify-center gap-2">
              {suggestions.map((s) => (
                <Chip key={s} onClick={() => send(s)}>
                  {s}
                </Chip>
              ))}
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {messages.map((m, i) =>
              m.role === "user" ? (
                <div key={i} className="ml-auto max-w-[85%] rounded-2xl rounded-br-md bg-[var(--accent)] px-4 py-2.5 text-sm font-medium text-white">
                  {m.content}
                </div>
              ) : m.role === "assistant" ? (
                <div key={i} className="flex max-w-[90%] items-end gap-2">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--accent-light)]">
                    <Icon name="psychology" size={18} className="text-[var(--accent)]" />
                  </div>
                  <div className="whitespace-pre-wrap rounded-2xl rounded-bl-md bg-[var(--surface-2)] px-4 py-2.5 text-sm font-medium text-[var(--text)]">
                    {m.content}
                  </div>
                </div>
              ) : (
                <p key={i} className="text-center text-sm font-bold text-[var(--error)]">{m.content}</p>
              )
            )}
            {sending && <p className="text-sm font-medium text-[var(--text-muted)]">Aria is typing…</p>}
          </div>
        )}
      </Card>

      {/* Input */}
      <div className="flex items-end gap-2 rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-2">
        <textarea
          className="max-h-32 flex-1 resize-none bg-transparent px-3 py-2 text-sm font-medium text-[var(--text)] outline-none placeholder:text-[var(--text-muted)]"
          rows={1}
          placeholder="Ask Aria anything…"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              send();
            }
          }}
          disabled={sending}
        />
        <Button className="gap-1.5" disabled={sending || !message.trim()} onClick={() => send()}>
          <Icon name="send" size={18} /> Send
        </Button>
      </div>
    </div>
  );
}
