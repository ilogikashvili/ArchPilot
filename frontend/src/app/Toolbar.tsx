import { PatternMenu } from "@/features/patterns/PatternMenu";
import { ShareDialog } from "@/features/workspace/ShareDialog";
import { useEffect, useState } from "react";
import { useReactFlow, useViewport } from "@xyflow/react";
import { toast } from "sonner";
import {
  Bell, Check, ChevronLeft, ChevronRight, CircleAlert, GitBranch, Loader2, Maximize, Minus, MoreHorizontal,
  PanelLeft, PanelRight, Plus, Redo2, RotateCcw, Settings, Trash2, Undo2, Bot, Download, Radio,
} from "lucide-react";
import { useEditor } from "@/store/editorStore";
import { exportArchitecture, resetArchitecture } from "@/api/architectureRepository";
import { liveSession } from "@/features/collab/liveSync";
import { PresenceAvatars } from "@/features/collab/PresenceAvatars";
import { useSimulation } from "@/features/simulation/simulationStore";
import { DownloadStructureDialog } from "@/features/codegen/DownloadStructureDialog";
import { AgentsDialog } from "@/features/agents/AgentsDialog";
import { AccountMenu } from "@/features/auth/AuthGuard";
import { IconButton, ToolbarButton } from "./ui";

interface Props { onToggleLeft: () => void; onToggleRight: () => void }

export function Toolbar({ onToggleLeft, onToggleRight }: Props) {
  const { undo, redo, deleteSelection, loadDoc } = useEditor.getState();
  const canUndo = useEditor((s) => s.past.length > 0);
  const canRedo = useEditor((s) => s.future.length > 0);
  const hasSelection = useEditor((s) => s.selection !== null);
  const status = useEditor((s) => s.saveStatus);
  const persistenceSource = useEditor((s) => s.persistenceSource);
  const rf = useReactFlow();
  const { zoom } = useViewport();
  const [menuOpen, setMenuOpen] = useState(false);

  const save = () => liveSession.flushNow();
  const [codegenOpen, setCodegenOpen] = useState(false);
  const [shareOpen,setShareOpen] = useState(false);
  const [agentsOpen, setAgentsOpen] = useState(false);
  const projectId = useEditor((s) => s.projectId);
  const simOpen = useSimulation((s) => s.open);

  const reset = async () => {
    setMenuOpen(false);
    if (!confirm("Reset architecture for everyone in this project?")) return;
    try {
      const { document, revision } = await resetArchitecture(useEditor.getState().projectId);
      loadDoc(document, "backend");
      liveSession.adopt(document, revision);
      toast.success("Architecture reset");
      setTimeout(() => rf.fitView({ padding: 0.2, duration: 300 }), 50);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not reset the architecture");
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement;
      const typing = ["INPUT", "TEXTAREA", "SELECT"].includes(t.tagName);
      const mod = e.metaKey || e.ctrlKey;
      if (mod && e.key.toLowerCase() === "s") { e.preventDefault(); save(); }
      else if (typing) return;
      else if (mod && e.key.toLowerCase() === "z") { e.preventDefault(); e.shiftKey ? redo() : undo(); }
      else if (mod && e.key.toLowerCase() === "y") { e.preventDefault(); redo(); }
      else if (e.key === "Delete" || e.key === "Backspace") deleteSelection();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <header className="flex h-12 shrink-0 items-center gap-1 border-b border-border bg-sidebar px-2">
      <IconButton label="Toggle sidebar" onClick={onToggleLeft}><PanelLeft /></IconButton>
      <IconButton label="Back (placeholder)" disabled><ChevronLeft /></IconButton>
      <IconButton label="Forward (placeholder)" disabled><ChevronRight /></IconButton>
      <Sep />
      <ToolbarButton label="Undo (Ctrl+Z)" onClick={undo} disabled={!canUndo}><Undo2 /> <span className="hidden md:inline">Undo</span></ToolbarButton>
      <ToolbarButton label="Redo (Ctrl+Shift+Z)" onClick={redo} disabled={!canRedo}><Redo2 /> <span className="hidden md:inline">Redo</span></ToolbarButton>
      <Sep />
      <ToolbarButton label="Fit to view" onClick={() => rf.fitView({ padding: 0.2, duration: 300 })}><Maximize /> <span className="hidden md:inline">Fit</span></ToolbarButton>
      <div className="flex items-center rounded-md border border-border">
        <IconButton label="Zoom out" onClick={() => rf.zoomOut({ duration: 150 })}><Minus /></IconButton>
        <span className="w-11 text-center font-mono text-xs text-muted-foreground">{Math.round(zoom * 100)}%</span>
        <IconButton label="Zoom in" onClick={() => rf.zoomIn({ duration: 150 })}><Plus /></IconButton>
      </div>
      <Sep />
      <IconButton label="Delete selected (Del)" onClick={deleteSelection} disabled={!hasSelection}><Trash2 /></IconButton>

      <div className="ml-auto flex items-center gap-1.5">
        <PatternMenu />
        <ToolbarButton label="Share project and manage members" onClick={()=>setShareOpen(true)}>Share</ToolbarButton>
        <ToolbarButton label="Version control" onClick={()=>window.dispatchEvent(new Event("editor:versions"))}><GitBranch /></ToolbarButton>
        <ToolbarButton label="Simulate a request flowing through the diagram" onClick={() => useSimulation.getState().set({ open: !simOpen })} className={simOpen ? "bg-accent text-foreground" : ""}><Radio /> <span className="hidden lg:inline">Simulate</span></ToolbarButton>
        <ToolbarButton label="External agent access tokens" onClick={() => setAgentsOpen(true)}><Bot /> <span className="hidden lg:inline">External agents</span></ToolbarButton>
        <button
          onClick={() => setCodegenOpen(true)}
          className="inline-flex h-8 items-center gap-1.5 rounded-md bg-primary px-3 text-xs font-medium text-primary-foreground shadow-[0_0_20px_-6px_var(--primary)] transition hover:brightness-110 focus-visible:outline-2 focus-visible:outline-ring [&_svg]:h-3.5 [&_svg]:w-3.5"
        >
          <Download /> Download structure
        </button>
        <Sep />
        <PresenceAvatars /><SaveIndicator status={status} />
        <AccountMenu />
        <IconButton label="Settings (placeholder)" onClick={() => toast("Settings are a placeholder")}><Settings /></IconButton>
        <div className="relative">
          <IconButton label="More actions" onClick={() => setMenuOpen((o) => !o)}><MoreHorizontal /></IconButton>
          {menuOpen && (
            <div className="absolute right-0 top-9 z-50 w-52 rounded-lg border border-border bg-popover p-1 shadow-xl">
              <button onClick={async () => { try { await exportArchitecture(useEditor.getState().projectId); } catch(e) { toast.error(e instanceof Error ? e.message : "Export failed"); } }} className="w-full p-2 text-left text-xs">Export JSON</button>
              <button onClick={reset} className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-xs text-destructive hover:bg-accent">
                <RotateCcw className="h-3.5 w-3.5" /> Reset architecture
              </button>
            </div>
          )}
        </div>
        <IconButton label="Notifications (placeholder)"><Bell /></IconButton>
        <IconButton label="Toggle right panel" onClick={onToggleRight}><PanelRight /></IconButton>
      </div>
      <DownloadStructureDialog projectId={projectId} open={codegenOpen} onOpenChange={setCodegenOpen} />
      <ShareDialog projectId={projectId} open={shareOpen} onOpenChange={setShareOpen}/>
      <AgentsDialog projectId={projectId} open={agentsOpen} onOpenChange={setAgentsOpen} />
    </header>
  );
}

function Sep() { return <span className="mx-1 h-5 w-px bg-border" />; }

function SaveIndicator({ status }: { status: string }) {
  const map = {
    saved: { icon: <Check className="h-3.5 w-3.5 text-success" />, text: "Synced" },
    unsaved: { icon: <span className="h-2 w-2 rounded-full bg-warning" />, text: "Syncing…" },
    saving: { icon: <Loader2 className="h-3.5 w-3.5 animate-spin" />, text: "Syncing…" },
    error: { icon: <CircleAlert className="h-3.5 w-3.5 text-destructive" />, text: "Sync failed" },
  } as const;
  const m = map[status as keyof typeof map];
  return (
    <span className="hidden items-center gap-1.5 px-2 text-xs text-muted-foreground sm:flex" aria-live="polite">
      {m.icon} {m.text}
    </span>
  );
}

export { GitBranch };
