import { useEffect, useRef, useState } from "react";
import { useReactFlow } from "@xyflow/react";
import { toast } from "sonner";
import { ChevronDown, Shapes, Loader2 } from "lucide-react";
import { patterns, type DesignPattern, type PatternTransformation } from "@/api/patterns";
import { applyOps } from "@/features/collab/ops";
import { useEditor } from "@/store/editorStore";
import { Button } from "@/components/ui/button";

/** Toolbar dropdown: pick a design pattern, preview what changes, then transform the diagram (undoable). */
export function PatternMenu() {
  const [open, setOpen] = useState(false);
  const [list, setList] = useState<DesignPattern[]>([]);
  const [chosen, setChosen] = useState<DesignPattern | null>(null);
  const [preview, setPreview] = useState<PatternTransformation | null>(null);
  const [busy, setBusy] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const rf = useReactFlow();
  const projectId = useEditor((s) => s.projectId);

  useEffect(() => {
    if (!open || list.length) return;
    patterns.list().then(setList).catch((e) => toast.error(e instanceof Error ? e.message : "Could not load patterns"));
  }, [open, list.length]);

  useEffect(() => {
    const close = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) { setOpen(false); } };
    window.addEventListener("mousedown", close);
    return () => window.removeEventListener("mousedown", close);
  }, []);

  async function choose(p: DesignPattern) {
    setChosen(p); setPreview(null); setBusy(true);
    try {
      const t = await patterns.preview(projectId, p.id, useEditor.getState().doc);
      setPreview(t);
      useEditor.getState().setHighlight(t.highlightNodeIds, t.highlightEdgeIds);
    } catch (e) { toast.error(e instanceof Error ? e.message : "Preview failed"); }
    finally { setBusy(false); }
  }

  async function apply() {
    if (!chosen) return;
    setBusy(true);
    try {
      // Re-run against the latest diagram so concurrent edits are respected.
      const t = await patterns.preview(projectId, chosen.id, useEditor.getState().doc);
      if (!t.applicable) { toast(t.summary); return; }
      useEditor.getState().update((d) => applyOps(d, t.operations));
      toast.success(`${chosen.name} applied`, { description: `${t.changes.length} changes · Ctrl+Z to undo` });
      setOpen(false); setChosen(null); setPreview(null);
      setTimeout(() => {
        useEditor.getState().setHighlight(t.highlightNodeIds, t.highlightEdgeIds);
        rf.fitView({ padding: 0.2, duration: 500 });
      }, 60);
      setTimeout(() => useEditor.getState().setHighlight([], []), 4000);
    } catch (e) { toast.error(e instanceof Error ? e.message : "Could not apply pattern"); }
    finally { setBusy(false); }
  }

  function back() { setChosen(null); setPreview(null); useEditor.getState().setHighlight([], []); }

  const groups = list.reduce<Record<string, DesignPattern[]>>((acc, p) => { (acc[p.category] ??= []).push(p); return acc; }, {});

  return (
    <div ref={ref} className="relative">
      <button
        aria-label="Design patterns" aria-expanded={open}
        onClick={() => { setOpen((o) => !o); if (open) back(); }}
        className="inline-flex h-8 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-md border border-border px-2.5 text-xs font-medium hover:bg-accent [&_svg]:h-3.5 [&_svg]:w-3.5"
      >
        <Shapes /> <span className="hidden md:inline">Design patterns</span> <ChevronDown />
      </button>
      {open && (
        <div className="absolute right-0 top-9 z-50 max-h-[70vh] w-80 overflow-y-auto rounded-lg border border-border bg-popover p-1 shadow-xl">
          {!chosen && !list.length && <p className="p-3 text-xs text-muted-foreground">Loading…</p>}
          {!chosen && Object.entries(groups).map(([cat, ps]) => (
            <div key={cat} className="py-1">
              <div className="px-2 py-1 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">{cat}</div>
              {ps.map((p) => (
                <button key={p.id} onClick={() => void choose(p)} className="block w-full rounded-md px-2 py-1.5 text-left hover:bg-accent">
                  <div className="text-xs font-medium">{p.name}</div>
                  <div className="text-[11px] text-muted-foreground">{p.whenToUse}</div>
                </button>
              ))}
            </div>
          ))}
          {chosen && (
            <div className="space-y-2 p-2 text-xs">
              <button onClick={back} className="text-muted-foreground hover:text-foreground">← All patterns</button>
              <h3 className="text-sm font-semibold">{chosen.name}</h3>
              <p className="text-muted-foreground">{chosen.description}</p>
              {busy && !preview && <p className="flex items-center gap-1"><Loader2 className="h-3 w-3 animate-spin" /> Analysing your design…</p>}
              {preview && (
                <>
                  <p className={preview.applicable ? "font-medium" : "text-muted-foreground"}>{preview.summary}</p>
                  {preview.changes.length > 0 && (
                    <ul className="list-disc space-y-0.5 pl-4">{preview.changes.map((c, i) => <li key={i}>{c}</li>)}</ul>
                  )}
                  <div className="flex gap-2 pt-1">
                    <Button size="sm" disabled={!preview.applicable || busy} onClick={() => void apply()}>Apply to diagram</Button>
                    <Button size="sm" variant="outline" onClick={back}>Cancel</Button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
