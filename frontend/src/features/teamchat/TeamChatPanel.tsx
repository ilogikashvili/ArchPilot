import { useEffect, useMemo, useRef, useState } from "react";
import { api, json } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useEditor } from "@/store/editorStore";
import { useCollab, colorFor } from "@/features/collab/liveSync";
import type { AIKey } from "@/features/workspace/AIPanel";

export interface TeamAgent { id: string; name: string; handle: string; role?: string; persona?: string; providerLabel?: string; ownerName?: string; autoReply: boolean; color?: string }
export interface ChatMessage { id: string; authorType: "USER" | "API_AGENT" | "AI_AGENT" | "SYSTEM"; authorId?: string; authorName: string; content: string; discussionId?: string; createdAt: string }
interface ChatState { messages: ChatMessage[]; agents: TeamAgent[]; discussionRunning: boolean }

export const ROLE_PRESETS: { name: string; role: string; persona: string }[] = [
  { name: "Architect", role: "Principal software architect", persona: "Think in trade-offs: scalability, coupling, cost. Propose concrete component changes." },
  { name: "Security", role: "Security reviewer", persona: "Hunt for auth gaps, exposed data stores, missing encryption, injection and trust-boundary problems." },
  { name: "DBA", role: "Database engineer", persona: "Focus on data ownership, consistency, indexing, migrations, caching and transactions." },
  { name: "SRE", role: "Site reliability engineer", persona: "Focus on failure modes, observability, retries, timeouts, deployments and capacity." },
  { name: "Critic", role: "Devil's advocate", persona: "Challenge every assumption politely but firmly. Point out what will break first in production." },
  { name: "Product", role: "Product owner", persona: "Keep the team focused on user value, scope and delivery risk. Ask what can be cut." },
];

/** Inserts or replaces a message by id, keeping chronological order. */
export function mergeMessage(list: ChatMessage[], m: ChatMessage): ChatMessage[] {
  if (list.some((x) => x.id === m.id)) return list;
  return [...list, m].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

const handleOfName = (n: string) => n.replace(/[^A-Za-z0-9_-]/g, "") || "user";

export function TeamChatPanel() {
  const pid = useEditor((s) => s.projectId);
  const online = useCollab((s) => s.users);
  const self = useCollab((s) => s.self);
  const [state, setState] = useState<ChatState>({ messages: [], agents: [], discussionRunning: false });
  const [typing, setTyping] = useState<Record<string, string>>({});
  const [text, setText] = useState("");
  const [error, setError] = useState("");
  const [view, setView] = useState<"chat" | "team" | "discuss">("chat");
  const endRef = useRef<HTMLDivElement>(null);

  async function load() { try { setState(await api<ChatState>(`/projects/${pid}/chat`)); } catch (e) { setError((e as Error).message); } }
  useEffect(() => { if (pid) void load(); }, [pid]);
  useEffect(() => {
    const on = (e: Event) => {
      const msg = (e as CustomEvent).detail;
      if (msg.type === "chat.message") {
        setState((s) => ({ ...s, messages: mergeMessage(s.messages, msg.message) }));
        const c = String(msg.message.content);
        if (msg.message.authorType === "SYSTEM" && /joined the team chat|left the team chat|Discussion (finished|stopped)|started a discussion/.test(c)) void load();
      } else if (msg.type === "chat.typing") {
        setTyping((t) => { const n = { ...t }; if (msg.typing) n[msg.agentId] = msg.name; else delete n[msg.agentId]; return n; });
      }
    };
    window.addEventListener("collab:chat", on);
    return () => window.removeEventListener("collab:chat", on);
  }, [pid]);
  useEffect(() => { endRef.current?.scrollIntoView({ block: "end" }); }, [state.messages.length, Object.keys(typing).length]);

  const humans = useMemo(() => { const m = new Map<string, (typeof online)[number]>(); online.forEach((u) => m.set(u.userId, u)); return [...m.values()]; }, [online]);
  const mentionables = useMemo(() => ["all", ...state.agents.map((a) => a.handle), ...humans.filter((h) => h.userId !== self?.userId).map((h) => handleOfName(h.name))], [state.agents, humans, self]);
  const partial = /@([A-Za-z0-9_-]*)$/.exec(text)?.[1];
  const suggestions = partial === undefined ? [] : mentionables.filter((m) => m.toLowerCase().startsWith(partial.toLowerCase())).slice(0, 6);

  async function send(e: React.FormEvent) {
    e.preventDefault(); if (!text.trim()) return; setError("");
    const content = text; setText("");
    try { const m = await api<ChatMessage>(`/projects/${pid}/chat/messages`, json("POST", { content })); setState((s) => ({ ...s, messages: mergeMessage(s.messages, m) })); }
    catch (err) { setError((err as Error).message); setText(content); }
  }

  return (
    <div className="flex h-full flex-col text-sm">
      <div className="flex gap-1 border-b border-border p-2">
        {(["chat", "team", "discuss"] as const).map((v) => (
          <button key={v} onClick={() => setView(v)} className={`rounded px-2 py-1 text-xs ${view === v ? "bg-accent text-foreground" : "text-muted-foreground"}`}>
            {v === "chat" ? "Chat" : v === "team" ? `Team (${humans.length + state.agents.length})` : "Discussion"}
          </button>
        ))}
      </div>
      {error && <p role="alert" className="px-3 py-2 text-xs text-destructive">{error}</p>}
      {view === "team" && <TeamView pid={pid} agents={state.agents} humans={humans} onChange={load} />}
      {view === "discuss" && <DiscussView pid={pid} agents={state.agents} running={state.discussionRunning} onChange={load} onStarted={() => setView("chat")} />}
      {view === "chat" && (
        <>
          <div className="flex-1 space-y-2 overflow-y-auto p-3" aria-live="polite" aria-label="Team chat messages">
            {state.messages.length === 0 && <p className="text-xs text-muted-foreground">No messages yet. Type @all to ask every AI teammate, or @Name for one.</p>}
            {state.messages.map((m) => <Bubble key={m.id} m={m} mine={m.authorId === self?.userId} agents={state.agents} />)}
            {Object.values(typing).map((n) => <p key={n} className="text-xs italic text-muted-foreground">{n} is thinking…</p>)}
            <div ref={endRef} />
          </div>
          <form onSubmit={send} className="relative border-t border-border p-2">
            {suggestions.length > 0 && (
              <div className="absolute bottom-full left-2 mb-1 flex flex-wrap gap-1 rounded border border-border bg-popover p-1 shadow">
                {suggestions.map((s) => <button type="button" key={s} className="rounded px-2 py-0.5 text-xs hover:bg-accent" onClick={() => setText(text.replace(/@[A-Za-z0-9_-]*$/, `@${s} `))}>@{s}</button>)}
              </div>
            )}
            <div className="flex gap-2">
              <Input aria-label="Chat message" placeholder="Message the team… (@all, @Architect)" value={text} onChange={(e) => setText(e.target.value)} maxLength={8000} />
              <Button disabled={!text.trim()}>Send</Button>
            </div>
          </form>
        </>
      )}
    </div>
  );
}

function Bubble({ m, mine, agents }: { m: ChatMessage; mine: boolean; agents: TeamAgent[] }) {
  if (m.authorType === "SYSTEM") return <p className="text-center text-[11px] text-muted-foreground">{m.content}</p>;
  const agent = m.authorType === "AI_AGENT" ? agents.find((a) => a.id === m.authorId) : undefined;
  const color = colorFor(m.authorId ?? m.authorName);
  const badge = m.authorType === "AI_AGENT" ? "AI" : m.authorType === "API_AGENT" ? "API agent" : null;
  return (
    <div className={`rounded-lg border p-2 ${mine ? "ml-6 border-primary/40 bg-primary/10" : "mr-6 border-border bg-card"} ${m.discussionId ? "border-dashed" : ""}`}>
      <div className="mb-0.5 flex items-center gap-1.5 text-[11px]">
        <span className="h-2 w-2 rounded-full" style={{ background: color }} />
        <span className="font-semibold">{m.authorName}</span>
        {badge && <span className="rounded bg-accent px-1 text-[9px] uppercase">{badge}</span>}
        {agent?.role && <span className="truncate text-muted-foreground">{agent.role}</span>}
        <span className="ml-auto text-muted-foreground">{new Date(m.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
      </div>
      <p className="whitespace-pre-wrap break-words">{m.content.split(/(@[A-Za-z0-9_-]+)/g).map((part, i) => part.startsWith("@") ? <span key={i} className="font-medium text-primary">{part}</span> : part)}</p>
    </div>
  );
}

function TeamView({ pid, agents, humans, onChange }: { pid: string; agents: TeamAgent[]; humans: { userId: string; name: string; agent?: boolean }[]; onChange: () => void }) {
  const [keys, setKeys] = useState<AIKey[]>([]);
  const [form, setForm] = useState({ name: "", role: "", persona: "", keyId: "", autoReply: false });
  const [busy, setBusy] = useState(false); const [err, setErr] = useState("");
  useEffect(() => { void api<AIKey[]>("/users/me/ai-keys").then(setKeys).catch(() => setKeys([])); }, []);
  async function act(fn: () => Promise<unknown>) { setBusy(true); setErr(""); try { await fn(); onChange(); } catch (e) { setErr((e as Error).message); } finally { setBusy(false); } }
  return (
    <div className="flex-1 space-y-4 overflow-y-auto p-3">
      <section>
        <h3 className="mb-1 text-xs font-semibold uppercase text-muted-foreground">Online now</h3>
        {humans.length === 0 && <p className="text-xs text-muted-foreground">Nobody else is connected.</p>}
        {humans.map((h) => <p key={h.userId} className="flex items-center gap-2"><span className="h-2 w-2 rounded-full bg-green-500" />{h.name}{h.agent && <span className="rounded bg-accent px-1 text-[9px] uppercase">API agent</span>}</p>)}
      </section>
      <section className="space-y-2">
        <h3 className="text-xs font-semibold uppercase text-muted-foreground">AI teammates</h3>
        {agents.length === 0 && <p className="text-xs text-muted-foreground">Add two or more AI teammates so they can talk to each other.</p>}
        {agents.map((a) => (
          <div key={a.id} className="rounded border border-border p-2">
            <div className="flex items-center gap-2"><span className="h-2 w-2 rounded-full" style={{ background: colorFor(a.id) }} /><b>@{a.handle}</b><span className="truncate text-xs text-muted-foreground">{a.role}</span>
              <button className="ml-auto text-xs text-destructive" disabled={busy} onClick={() => { if (confirm(`Remove @${a.handle}?`)) void act(() => api(`/projects/${pid}/team-agents/${a.id}`, { method: "DELETE" })); }}>Remove</button></div>
            <p className="text-[11px] text-muted-foreground">{a.providerLabel} · key of {a.ownerName}</p>
            <label className="mt-1 flex items-center gap-2 text-xs"><input type="checkbox" checked={a.autoReply} onChange={(e) => void act(() => api(`/projects/${pid}/team-agents/${a.id}`, json("PATCH", { autoReply: e.target.checked })))} />Answer messages that mention nobody</label>
          </div>
        ))}
      </section>
      <section className="space-y-2 rounded border border-border p-2">
        <h3 className="text-xs font-semibold uppercase text-muted-foreground">Add AI teammate</h3>
        <div className="flex flex-wrap gap-1">{ROLE_PRESETS.map((p) => <button key={p.name} type="button" className="rounded border border-border px-2 py-0.5 text-xs hover:bg-accent" onClick={() => setForm((f) => ({ ...f, name: p.name, role: p.role, persona: p.persona }))}>{p.name}</button>)}</div>
        <form className="space-y-2" onSubmit={(e) => { e.preventDefault(); void act(async () => { await api(`/projects/${pid}/team-agents`, json("POST", form)); setForm({ name: "", role: "", persona: "", keyId: form.keyId, autoReply: false }); }); }}>
          <Input required aria-label="Teammate name" placeholder="Name (used as @mention)" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Input aria-label="Teammate role" placeholder="Role" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })} />
          <textarea aria-label="Teammate instructions" className="min-h-16 w-full rounded border border-border bg-background p-2 text-xs" placeholder="Personality and instructions" value={form.persona} onChange={(e) => setForm({ ...form, persona: e.target.value })} />
          <select required aria-label="Teammate AI model" className="w-full rounded border border-border bg-background p-2" value={form.keyId} onChange={(e) => setForm({ ...form, keyId: e.target.value })}>
            <option value="">Which AI model?</option>
            {keys.map((k) => <option key={k.id} value={k.id}>{k.provider} / {k.model}</option>)}
          </select>
          {keys.length === 0 && <a className="text-xs text-primary underline" href="/settings">Add an AI key or a local Ollama model first ↗</a>}
          <Button disabled={busy || !form.keyId}>Add teammate</Button>
        </form>
        {err && <p role="alert" className="text-xs text-destructive">{err}</p>}
      </section>
    </div>
  );
}

function DiscussView({ pid, agents, running, onChange, onStarted }: { pid: string; agents: TeamAgent[]; running: boolean; onChange: () => void; onStarted: () => void }) {
  const [topic, setTopic] = useState(""); const [rounds, setRounds] = useState(2);
  const [picked, setPicked] = useState<string[]>([]); const [err, setErr] = useState("");
  const chosen = picked.length ? picked : agents.map((a) => a.id);
  return (
    <div className="flex-1 space-y-3 overflow-y-auto p-3">
      <p className="text-xs text-muted-foreground">AI teammates take turns discussing a topic about this design. Everyone online sees it live and can jump in with @mentions.</p>
      {running && <div className="flex items-center gap-2 rounded border border-primary/40 p-2 text-xs">A discussion is running.<Button size="sm" variant="outline" className="ml-auto" onClick={() => void api(`/projects/${pid}/chat/discussions/stop`, json("POST")).then(onChange)}>Stop</Button></div>}
      <form className="space-y-2" onSubmit={async (e) => { e.preventDefault(); setErr(""); try { await api(`/projects/${pid}/chat/discussions`, json("POST", { topic, rounds, agentIds: chosen })); setTopic(""); onChange(); onStarted(); } catch (x) { setErr((x as Error).message); } }}>
        <textarea required aria-label="Discussion topic" className="min-h-20 w-full rounded border border-border bg-background p-2" placeholder="Should orders and payments share one database?" value={topic} onChange={(e) => setTopic(e.target.value)} />
        <div className="space-y-1">{agents.map((a) => <label key={a.id} className="flex items-center gap-2 text-xs"><input type="checkbox" checked={chosen.includes(a.id)} onChange={(e) => setPicked(e.target.checked ? [...new Set([...chosen, a.id])] : chosen.filter((x) => x !== a.id))} />@{a.handle} <span className="text-muted-foreground">{a.role}</span></label>)}</div>
        <label className="flex items-center gap-2 text-xs">Rounds <input type="number" min={1} max={5} value={rounds} onChange={(e) => setRounds(Number(e.target.value))} className="w-14 rounded border border-border bg-background p-1" /></label>
        <Button disabled={running || chosen.length < 2}>Start discussion</Button>
        {agents.length < 2 && <p className="text-xs text-muted-foreground">Add at least two AI teammates in the Team tab.</p>}
        {err && <p role="alert" className="text-xs text-destructive">{err}</p>}
      </form>
    </div>
  );
}
