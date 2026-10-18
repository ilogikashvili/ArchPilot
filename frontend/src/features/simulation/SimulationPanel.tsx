import { useEffect, useMemo, useRef, useState } from "react";
import { Pause, Play, RotateCcw, SkipBack, SkipForward, X } from "lucide-react";
import { useEditor } from "@/store/editorStore";
import { fieldClass } from "@/app/ui";
import { simulate } from "./engine";
import { useSimulation } from "./simulationStore";

const STEP_MS = 1100;
const KIND_STYLE = { request: "text-sky-400", response: "text-emerald-400", async: "text-amber-400" } as const;

/** Bottom panel: build a sample request, then watch it travel through the diagram step by step. */
export function SimulationPanel() {
  const { open, result, current, playing, speed, set } = useSimulation();
  const nodes = useEditor((s) => s.doc.nodes);
  const selection = useEditor((s) => s.selection);
  const [collapsed, setCollapsed] = useState(false);
  const [sourceId, setSourceId] = useState("");
  const [method, setMethod] = useState("POST");
  const [path, setPath] = useState("/orders");
  const [body, setBody] = useState('{\n  "customerId": "c-42",\n  "items": [{ "sku": "book-1", "qty": 2 }]\n}');
  const [error, setError] = useState("");
  const listRef = useRef<HTMLOListElement>(null);

  useEffect(() => {
    if (!open) return;
    if (selection?.type === "component") setSourceId(selection.id);
    else if (!sourceId || !nodes.some((n) => n.id === sourceId)) {
      setSourceId((nodes.find((n) => n.type === "web") ?? nodes.find((n) => n.type === "api") ?? nodes[0])?.id ?? "");
    }
  }, [open, selection, nodes]); // eslint-disable-line react-hooks/exhaustive-deps

  // Animation loop
  useEffect(() => {
    if (!playing || !result) return;
    let raf = 0;
    let last = performance.now();
    const tick = (now: number) => {
      const s = useSimulation.getState();
      const dt = (now - last) / (STEP_MS / s.speed);
      last = now;
      let progress = s.progress + dt;
      let idx = s.current;
      if (progress >= 1) {
        if (idx >= (s.result?.events.length ?? 0) - 1) { s.set({ progress: 1, playing: false }); return; }
        idx += 1; progress = 0;
      }
      s.set({ progress, current: idx });
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [playing, result]);

  // Highlight the components/connection the packet is on.
  const event = result?.events[current];
  useEffect(() => {
    const { setHighlight } = useEditor.getState();
    if (event) setHighlight([event.fromId, event.toId], [event.edgeId]);
    else setHighlight([], []);
  }, [event]);
  useEffect(() => () => useEditor.getState().setHighlight([], []), []);
  useEffect(() => { listRef.current?.querySelector(`[data-index="${current}"]`)?.scrollIntoView({ block: "nearest" }); }, [current]);

  const nodeIO = useMemo(() => result?.io.find((x) => x.nodeId === event?.toId), [result, event]);

  if (!open) return null;

  const run = () => {
    setError("");
    let parsed: Record<string, unknown>;
    try { parsed = body.trim() ? JSON.parse(body) : {}; } catch { setError("Body must be valid JSON."); return; }
    const res = simulate(useEditor.getState().doc, { sourceId, method, path, body: parsed });
    set({ result: res, current: 0, progress: 0, playing: res.events.length > 0 });
    if (!res.events.length) setError(res.warnings[0] ?? "Nothing to simulate.");
  };
  const step = (d: number) => result && set({ playing: false, progress: d > 0 ? 1 : 0, current: Math.min(Math.max(current + d, 0), result.events.length - 1) });
  const close = () => { set({ open: false, playing: false, result: null }); useEditor.getState().setHighlight([], []); };

  return (
    <section aria-label="Request flow simulation" style={collapsed ? {} : {height: 300, resize: "vertical"}} className="@container relative z-20 flex max-h-[45vh] shrink-0 flex-col overflow-hidden border-t border-border bg-popover">
      <header className="flex items-center gap-2 border-b border-border px-3 py-2 text-xs">
        <button className="font-semibold" onClick={()=>setCollapsed(!collapsed)} aria-expanded={!collapsed}>Request flow simulation {collapsed ? "▴" : "▾"}</button>
        <span className="hidden text-muted-foreground @lg:inline">— send a sample request and watch it travel</span>
        <button onClick={close} aria-label="Close simulation" className="ml-auto rounded p-1 hover:bg-accent"><X className="h-3.5 w-3.5" /></button>
      </header>
      <div className={`${collapsed ? "hidden" : "grid"} min-h-0 flex-1 grid-cols-1 gap-3 overflow-y-auto p-3 @xl:grid-cols-[230px_1fr] @4xl:grid-cols-[260px_1fr_1fr] @4xl:overflow-hidden`}>
        <div className="space-y-2 text-xs @4xl:overflow-y-auto">
          <label className="block space-y-1"><span className="text-muted-foreground">Send from</span>
            <select value={sourceId} onChange={(e) => setSourceId(e.target.value)} className={fieldClass} aria-label="Source component">
              {nodes.map((n) => <option key={n.id} value={n.id}>{n.label} ({n.type})</option>)}
            </select>
          </label>
          <div className="flex gap-2">
            <select value={method} onChange={(e) => setMethod(e.target.value)} className={`${fieldClass} w-24`} aria-label="Method">
              {["GET", "POST", "PUT", "PATCH", "DELETE"].map((m) => <option key={m}>{m}</option>)}
            </select>
            <input value={path} onChange={(e) => setPath(e.target.value)} className={fieldClass} aria-label="Path" />
          </div>
          <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={5} className={`${fieldClass} font-mono`} aria-label="Request body (JSON)" />
          {error && <p className="text-destructive">{error}</p>}
          <button onClick={run} disabled={!sourceId} className="w-full rounded-md bg-primary px-3 py-1.5 font-medium text-primary-foreground disabled:opacity-50">Send request</button>
          {result && (
            <div className="flex items-center gap-1">
              <button aria-label="Previous step" onClick={() => step(-1)} className="rounded p-1.5 hover:bg-accent"><SkipBack className="h-3.5 w-3.5" /></button>
              <button aria-label={playing ? "Pause" : "Play"} onClick={() => set(current >= result.events.length - 1 && !playing ? { current: 0, progress: 0, playing: true } : { playing: !playing })} className="rounded p-1.5 hover:bg-accent">
                {playing ? <Pause className="h-3.5 w-3.5" /> : <Play className="h-3.5 w-3.5" />}
              </button>
              <button aria-label="Next step" onClick={() => step(1)} className="rounded p-1.5 hover:bg-accent"><SkipForward className="h-3.5 w-3.5" /></button>
              <button aria-label="Restart" onClick={() => set({ current: 0, progress: 0, playing: true })} className="rounded p-1.5 hover:bg-accent"><RotateCcw className="h-3.5 w-3.5" /></button>
              <select value={speed} onChange={(e) => set({ speed: Number(e.target.value) })} className="ml-auto rounded border border-border bg-background px-1 py-0.5" aria-label="Speed">
                {[0.5, 1, 2, 4].map((s) => <option key={s} value={s}>{s}×</option>)}
              </select>
            </div>
          )}
          {result?.warnings.slice(0, 3).map((w) => <p key={w} className="text-warning">{w}</p>)}
        </div>

        <ol ref={listRef} className="max-h-48 space-y-1 overflow-y-auto text-xs @4xl:max-h-none @4xl:min-h-0" aria-label="Packet timeline">
          {result?.events.map((e) => (
            <li key={e.index} data-index={e.index}>
              <button onClick={() => set({ current: e.index, progress: 1, playing: false })}
                className={`flex w-full items-center gap-2 rounded px-2 py-1 text-left ${e.index === current ? "bg-accent" : "hover:bg-accent/50"}`}>
                <span className="w-5 text-right text-muted-foreground">{e.index + 1}</span>
                <span className={`w-16 font-mono uppercase ${KIND_STYLE[e.kind]}`}>{e.kind === "async" ? "message" : e.kind}</span>
                <span className="truncate">{e.fromLabel} → {e.toLabel}</span>
                <span className="ml-auto text-muted-foreground">{e.protocol}</span>
              </button>
            </li>
          ))}
          {!result && <li className="text-muted-foreground">Pick a starting component and press “Send request”.</li>}
        </ol>

        <div className="space-y-2 text-xs @4xl:min-h-0 @4xl:overflow-y-auto @xl:col-span-2 @4xl:col-span-1">
          {event ? (
            <>
              <p className="font-semibold">Step {event.index + 1}: {event.note}</p>
              <p className="text-muted-foreground">Packet data</p>
              <pre className="rounded bg-background/70 p-2 font-mono text-[11px] leading-relaxed">{JSON.stringify(event.data, null, 2)}</pre>
              {nodeIO && event.kind !== "response" && (
                <>
                  <p className="text-muted-foreground">{nodeIO.label} ({nodeIO.type}) — {nodeIO.note}</p>
                  <p className="text-muted-foreground">Output</p>
                  <pre className="rounded bg-background/70 p-2 font-mono text-[11px] leading-relaxed">{JSON.stringify(nodeIO.output, null, 2)}</pre>
                </>
              )}
            </>
          ) : <p className="text-muted-foreground">Input and output of each step appear here.</p>}
        </div>
      </div>
    </section>
  );
}
