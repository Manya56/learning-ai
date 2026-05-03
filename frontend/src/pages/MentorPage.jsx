import { useState } from "react";
import { chatMentorApi } from "../api/mentor";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";

export default function MentorPage() {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [sending, setSending] = useState(false);

  const send = async () => {
    if (!message.trim() || sending) return;
    setSending(true);
    const user = { role: "user", content: message };
    setMessages((prev) => [...prev, user]);
    setMessage("");
    try {
      const res = await chatMentorApi({ message: user.content, personality: "ENCOURAGING", newSession: false });
      setMessages((prev) => [...prev, { role: "assistant", content: res.reply || "..." }]);
    } catch {
      setMessages((prev) => [...prev, { role: "system", content: "Something went wrong. Try again." }]);
    } finally {
      setSending(false);
    }
  };

  return (
    <Card>
      <h3 className="mb-3 font-semibold">Aria - AI Learning Mentor</h3>
      <div className="mb-3 max-h-80 space-y-2 overflow-auto">
        {messages.map((m, i) => (
          <div key={i} className={`rounded-lg p-2 ${m.role === "user" ? "bg-[var(--accent-light)]" : "bg-[var(--surface-2)]"}`}>
            {m.content}
          </div>
        ))}
      </div>
      <textarea
        className="w-full rounded-md bg-[var(--surface-2)] p-2"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        disabled={sending}
      />
      <Button className="mt-2" disabled={sending || !message.trim()} onClick={send}>
        {sending ? "Sending..." : "Send"}
      </Button>
    </Card>
  );
}
