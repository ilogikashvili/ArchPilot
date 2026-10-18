import { useState } from "react";
import { Bot } from "lucide-react";
import { colorFor, useCollab } from "./liveSync";

/** Who is in the project right now (people and AI agents) plus a short activity feed. */
export function PresenceAvatars() {
  const users = useCollab((s) => s.users);
  const self = useCollab((s) => s.self);
  const connected = useCollab((s) => s.connected);
  const activity = useCollab((s) => s.activity);
  const [open, setOpen] = useState(false);
  const unique = Array.from(new Map(users.map((u) => [u.userId, u])).values());

  return (
    <div className="relative">
      <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-2 rounded-md px-1.5 py-1 hover:bg-accent" aria-label="Collaborators">
        <span className={`h-2 w-2 rounded-full ${connected ? "bg-success" : "bg-muted-foreground"}`} title={connected ? "Live" : "Reconnecting…"} />
        <div className="flex -space-x-1.5">
          {unique.slice(0, 5).map((u) => (
            <span key={u.userId} title={`${u.name}${u.userId === self?.userId ? " (you)" : ""}${u.agent ? " — AI agent" : ""}`}
              className="grid h-6 w-6 place-items-center rounded-full border-2 border-sidebar text-[10px] font-semibold text-white"
              style={{ background: colorFor(u.userId) }}>
              {u.agent ? <Bot className="h-3 w-3" /> : (u.name || "?").slice(0, 1).toUpperCase()}
            </span>
          ))}
          {unique.length > 5 && <span className="grid h-6 w-6 place-items-center rounded-full bg-muted text-[10px]">+{unique.length - 5}</span>}
        </div>
      </button>
      {open && (
        <div className="absolute right-0 top-9 z-50 w-72 rounded-lg border border-border bg-popover p-3 text-xs shadow-xl">
          <p className="mb-2 font-semibold">In this project now</p>
          <ul className="mb-3 space-y-1.5">
            {unique.map((u) => (
              <li key={u.userId} className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full" style={{ background: colorFor(u.userId) }} />
                <span className="flex-1 truncate">{u.name}{u.userId === self?.userId ? " (you)" : ""}</span>
                {u.agent && <span className="rounded bg-primary/15 px-1.5 py-0.5 text-[10px] text-primary">AI agent</span>}
              </li>
            ))}
            {!unique.length && <li className="text-muted-foreground">Connecting…</li>}
          </ul>
          <p className="mb-1 font-semibold">Recent activity</p>
          <ul className="max-h-48 space-y-1 overflow-y-auto text-muted-foreground">
            {activity.map((a) => <li key={a.id}>{a.agent ? "🤖 " : ""}{a.text}</li>)}
            {!activity.length && <li>No activity yet.</li>}
          </ul>
        </div>
      )}
    </div>
  );
}
