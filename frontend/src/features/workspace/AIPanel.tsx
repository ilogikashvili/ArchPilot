import { useState } from "react";
import { api, json } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ResourceState, useResource } from "./shared";
import { useEditor } from "@/store/editorStore";
import { loadArchitecture } from "@/api/architectureRepository";
import { liveSession } from "@/features/collab/liveSync";
import { persistCurrent, useBranch } from "./VersionPanel";
export type AIKey = {
  id: string;
  provider: string;
  model: string;
  last4: string;
  baseUrl?: string;
  label?: string;
};
type Run = {
  id: string;
  prompt: string;
  summary?: string;
  status: string;
  operations?: unknown[];
  appliedOps?: unknown[];
  error?: string;
};
export function AIProviders() {
  const r = useResource<AIKey[]>("/users/me/ai-keys");
  const [provider, setProvider] = useState("OPENAI");
  const [model, setModel] = useState("");
  const [key, setKey] = useState("");
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("");
  const [baseUrl, setBaseUrl] = useState(
    import.meta.env.DEV ? "http://localhost:11434" : "",
  );
  const [models, setModels] = useState<string[]>([]);
  const ollama = provider === "OLLAMA";
  async function detect() {
    setBusy(true);
    setStatus("");
    try {
      const r = await api<{
        ok: boolean;
        models: { name: string }[];
        message?: string;
      }>(
        `/users/me/ai-keys/ollama/models?baseUrl=${encodeURIComponent(baseUrl)}`,
      );
      if (!r.ok) setStatus(r.message ?? "Could not reach Ollama");
      else {
        setModels(r.models.map((m) => m.name));
        if (!model && r.models[0]) setModel(r.models[0].name);
        setStatus(`Found ${r.models.length} model(s)`);
      }
    } catch (e) {
      setStatus((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function act(fn: () => Promise<unknown>) {
    setBusy(true);
    setStatus("");
    try {
      const result = await fn();
      setStatus(typeof result === "string" ? result : "Done");
      await r.reload();
    } catch (e) {
      setStatus((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="space-y-3">
      <p className="text-xs text-muted-foreground">
        Keys are stored by the server; only the last four characters are
        displayed. Ollama runs models on your own computer: the server must run
        on the same machine as Ollama (or reach its address).
      </p>
      <form
        className="flex flex-wrap gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          void act(async () => {
            await api(
              "/users/me/ai-keys",
              json("POST", {
                provider,
                label: ollama
                  ? `Ollama ${model || "qwen3:4b"}`
                  : `${provider} key`,
                model: ollama && !model ? "qwen3:4b" : model,
                apiKey: key,
                baseUrl: ollama ? baseUrl : undefined,
              }),
            );
            setKey("");
          });
        }}
      >
        <select
          aria-label="AI provider"
          value={provider}
          onChange={(e) => setProvider(e.target.value)}
        >
          {["OPENAI", "ANTHROPIC", "GEMINI", "OPENROUTER", "OLLAMA"].map(
            (p) => (
              <option key={p} value={p}>
                {p === "OLLAMA" ? "OLLAMA (local)" : p}
              </option>
            ),
          )}
        </select>
        {ollama && (
          <>
            <Input
              className="w-56"
              required={ollama}
              placeholder="http://ollama-host:11434"
              aria-label="Ollama URL"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
            />
            <Button
              type="button"
              variant="outline"
              disabled={busy || !baseUrl}
              onClick={() => void detect()}
            >
              Detect models
            </Button>
          </>
        )}
        <Input
          className="w-44"
          required={!ollama}
          list="ollama-models"
          placeholder={ollama ? "qwen3:4b" : "Model"}
          aria-label="AI model"
          value={model}
          onChange={(e) => setModel(e.target.value)}
        />
        <datalist id="ollama-models">
          {models.map((m) => (
            <option key={m} value={m} />
          ))}
        </datalist>
        <Input
          className="w-52"
          type="password"
          autoComplete="off"
          required={!ollama}
          placeholder={ollama ? "API key (optional)" : "API key"}
          aria-label="Provider API key"
          value={key}
          onChange={(e) => setKey(e.target.value)}
        />
        <Button disabled={busy}>Add key</Button>
      </form>
      {status && (
        <p role="status" className="text-sm">
          {status}
        </p>
      )}
      <ResourceState resource={r} empty={!r.data?.length} />
      {r.data?.map((k) => (
        <div
          key={k.id}
          className="flex flex-wrap items-center gap-3 rounded border border-border p-3 text-sm"
        >
          <span className="mr-auto">
            {k.provider} · {k.model} ·{" "}
            {k.provider === "OLLAMA"
              ? (k.baseUrl ?? "local")
              : `••••${k.last4}`}
          </span>
          <Button
            disabled={busy}
            variant="outline"
            size="sm"
            onClick={() =>
              void act(async () => {
                const res = await api<{ message?: string; ok?: boolean }>(
                  `/users/me/ai-keys/${k.id}/test`,
                  json("POST"),
                );
                return (
                  res.message ??
                  (res.ok === false ? "Test failed" : "Connection tested")
                );
              })
            }
          >
            Test
          </Button>
          <Button
            disabled={busy}
            variant="outline"
            size="sm"
            onClick={() => {
              if (confirm("Delete this provider key?"))
                void act(() =>
                  api(`/users/me/ai-keys/${k.id}`, { method: "DELETE" }),
                );
            }}
          >
            Delete
          </Button>
        </div>
      ))}
    </div>
  );
}
export function AIPanel() {
  const pid = useEditor((s) => s.projectId);
  const branch = useBranch((s) => s.id);
  const keys = useResource<AIKey[]>("/users/me/ai-keys");
  const runs = useResource<Run[]>(`/projects/${pid}/ai/agent-runs`);
  const [keyId, setKeyId] = useState("");
  const [prompt, setPrompt] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return (
    <div className="space-y-4 p-4 text-sm">
      <h2 className="font-semibold">AI assistant</h2>
      <p className="text-xs text-muted-foreground">
        Describe a change and apply it to your architecture.
      </p>
      <a className="text-primary underline" href="/settings">
        Manage AI provider keys ↗
      </a>
      <ResourceState resource={keys} empty={!keys.data?.length} />
      <form
        className="space-y-3"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError("");
          try {
            await persistCurrent();
            await api(
              `/projects/${pid}/ai/agent-runs`,
              json("POST", { keyId, prompt }),
            );
            await runs.reload();
            const res = await loadArchitecture(pid);
            useEditor.getState().loadDoc(res.document, "backend");
            liveSession.adopt(res.document, res.revision);
          } catch (e) {
            setError((e as Error).message);
          } finally {
            setBusy(false);
          }
        }}
      >
        <select
          className="w-full rounded border border-border bg-background p-2"
          required
          aria-label="Saved AI key"
          value={keyId}
          onChange={(e) => setKeyId(e.target.value)}
        >
          <option value="">Select saved key…</option>
          {keys.data?.map((k) => (
            <option key={k.id} value={k.id}>
              {k.provider} / {k.model} · {k.last4}
            </option>
          ))}
        </select>
        <textarea
          className="min-h-28 w-full rounded border border-border bg-background p-2"
          required
          maxLength={8000}
          aria-label="AI prompt"
          placeholder="Add a redis cache between api and db"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
        />
        <Button disabled={busy || !!branch || !keyId}>
          {busy ? "Running…" : "Run assistant"}
        </Button>
        {branch && <p>Switch to main before running AI.</p>}
      </form>
      {error && (
        <p role="alert" className="text-destructive">
          {error}
        </p>
      )}
      <h3 className="font-semibold">Run history</h3>
      <ResourceState resource={runs} empty={!runs.data?.length} />
      {runs.data?.map((run) => (
        <article
          className="space-y-2 rounded-lg border border-border p-3"
          key={run.id}
        >
          <p className="font-medium">{run.prompt}</p>
          <p className="text-xs text-muted-foreground">{run.status}</p>
          <p>{run.summary ?? run.error}</p>
          <details>
            <summary>Applied operations</summary>
            <pre className="overflow-auto whitespace-pre-wrap break-all text-xs">
              {JSON.stringify(run.appliedOps ?? run.operations ?? [], null, 2)}
            </pre>
          </details>
        </article>
      ))}
    </div>
  );
}
