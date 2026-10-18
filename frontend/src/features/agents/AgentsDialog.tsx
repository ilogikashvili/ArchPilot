import { useEffect, useState } from "react";
import { Bot, Copy, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { agents, type Agent, type AgentRole } from "@/api/agents";
import { API_BASE } from "@/api/client";
import { fieldClass } from "@/app/ui";

const PROVIDERS = ["OpenAI GPT", "Anthropic Claude", "GitHub Copilot", "Kimi", "Local model", "Other"];
const ROLES: { id: AgentRole; text: string }[] = [
  { id: "EDITOR", text: "Can edit the diagram" },
  { id: "REVIEWER", text: "Can read and run reviews" },
  { id: "VIEWER", text: "Read only" },
];

/** Project owners connect outside AI models with a project-scoped token. */
export function AgentsDialog({ projectId, open, onOpenChange }: { projectId: string; open: boolean; onOpenChange: (o: boolean) => void }) {
  const [list, setList] = useState<Agent[]>([]);
  const [name, setName] = useState("");
  const [provider, setProvider] = useState<string>(PROVIDERS[1]!);
  const [role, setRole] = useState<AgentRole>("EDITOR");
  const [created, setCreated] = useState<Agent | null>(null);
  const [error, setError] = useState("");

  const refresh = () => agents.list(projectId).then(setList).catch((e) => setError(e.message));
  useEffect(() => { if (open) { setCreated(null); setError(""); void refresh(); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const create = async () => {
    setError("");
    try {
      const a = await agents.create(projectId, { name: name.trim() || provider, provider, role });
      setCreated(a); setName(""); void refresh();
    } catch (e) { setError(e instanceof Error ? e.message : "Could not connect the agent"); }
  };
  const revoke = async (a: Agent) => {
    if (!confirm(`Disconnect ${a.name}? Its token stops working immediately.`)) return;
    try { await agents.revoke(projectId, a.id); void refresh(); } catch (e) { toast.error(e instanceof Error ? e.message : "Could not revoke"); }
  };

  const apiBase = new URL(API_BASE, window.location.href).toString().replace(/\/$/, "");
  const instructions = created?.token ? [
    `You can edit an architecture diagram through this API.`,
    `Base URL: ${apiBase}`,
    `Header: Authorization: Bearer ${created.token}`,
    `1. GET /agent/me — your project, role and the list of operations`,
    `2. GET /projects/${projectId}/architecture — the current diagram (nodes + edges)`,
    `3. POST /projects/${projectId}/architecture/operations with {"operations":[...]} — e.g.`,
    `   {"op":"node.add","node":{"id":"orders","type":"service","label":"Order Service","x":300,"y":120}}`,
    `   {"op":"edge.add","edge":{"id":"e1","sourceId":"gateway","targetId":"orders","type":"sync","label":"REST"}}`,
    `Component types: service, api, database, queue, cache, external, web. Connection types: sync, async, event, dependency.`,
  ].join("\n") : "";
  const copy = (text: string) => navigator.clipboard.writeText(text).then(() => toast.success("Copied"));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>AI agents</DialogTitle>
          <DialogDescription>Let outside models (GPT, Claude, Copilot, Kimi or a local model) edit this project live, like a teammate. Only owners and admins can connect or disconnect them.</DialogDescription>
        </DialogHeader>

        {created?.token ? (
          <div className="space-y-2 rounded-lg border border-warning/40 bg-warning/10 p-3 text-xs">
            <p className="font-semibold">Token for {created.name} — copy it now, it won't be shown again.</p>
            <div className="flex gap-2">
              <code className="flex-1 truncate rounded bg-background px-2 py-1.5 font-mono">{created.token}</code>
              <button onClick={() => copy(created.token!)} className="rounded border border-border px-2" aria-label="Copy token"><Copy className="h-3.5 w-3.5" /></button>
            </div>
            <p className="text-muted-foreground">Paste these instructions into the model's system prompt or tool config:</p>
            <pre className="max-h-40 overflow-auto rounded bg-background p-2 font-mono text-[11px]">{instructions}</pre>
            <button onClick={() => copy(instructions)} className="rounded-md border border-border px-3 py-1">Copy instructions</button>
          </div>
        ) : (
          <div className="grid gap-2 text-xs md:grid-cols-[1fr_1fr_1fr_auto]">
            <select value={provider} onChange={(e) => setProvider(e.target.value)} className={fieldClass} aria-label="Model provider">
              {PROVIDERS.map((p) => <option key={p}>{p}</option>)}
            </select>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Name (e.g. Claude reviewer)" className={fieldClass} aria-label="Agent name" />
            <select value={role} onChange={(e) => setRole(e.target.value as AgentRole)} className={fieldClass} aria-label="Permission">
              {ROLES.map((r) => <option key={r.id} value={r.id}>{r.text}</option>)}
            </select>
            <button onClick={create} className="rounded-md bg-primary px-3 py-1.5 font-medium text-primary-foreground">Connect</button>
          </div>
        )}
        {error && <p className="text-xs text-destructive">{error}</p>}

        <ul className="divide-y divide-border rounded-lg border border-border text-xs">
          {list.map((a) => (
            <li key={a.id} className="flex items-center gap-3 p-2.5">
              <Bot className="h-4 w-4 text-primary" />
              <div className="min-w-0 flex-1">
                <p className="font-medium">{a.name} <span className="text-muted-foreground">· {a.provider} · {a.role.toLowerCase()}</span></p>
                <p className="text-muted-foreground">{a.tokenPrefix}… · {a.revoked ? "disconnected" : a.lastUsedAt ? `last active ${new Date(a.lastUsedAt).toLocaleString()}` : "never used"}</p>
              </div>
              {!a.revoked && <button onClick={() => revoke(a)} aria-label={`Disconnect ${a.name}`} className="rounded p-1.5 text-destructive hover:bg-accent"><Trash2 className="h-3.5 w-3.5" /></button>}
            </li>
          ))}
          {!list.length && <li className="p-3 text-muted-foreground">No agents connected yet.</li>}
        </ul>
      </DialogContent>
    </Dialog>
  );
}
