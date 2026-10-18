import { VersionPanel } from "@/features/workspace/VersionPanel";
import { TeamChatPanel } from "@/features/teamchat/TeamChatPanel";
import { AIPanel } from "@/features/workspace/AIPanel";
import { useEffect, useState } from "react";
import { ReactFlowProvider } from "@xyflow/react";
import { ArchitectureCanvas } from "@/features/architecture/components/ArchitectureCanvas";
import { Inspector } from "@/features/inspector/Inspector";
import { ReviewPanel } from "@/features/ai-review/ReviewPanel";
import { loadArchitecture } from "@/api/architectureRepository";
import { projects } from "@/api/projects";
import { useEditor } from "@/store/editorStore";
import { Toaster } from "@/components/ui/sonner";
import { NavSidebar, PalettePanel } from "./Sidebar";
import { useLiveCollaboration } from "@/features/collab/liveSync";
import { SimulationPanel } from "@/features/simulation/SimulationPanel";
import { Toolbar } from "./Toolbar";

export function EditorApp({ projectId }: { projectId: string }) {
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [leftOpen, setLeftOpen] = useState(true);
  const [rightOpen, setRightOpen] = useState(true);
  const [tab, setTab] = useState<"inspector" | "review" | "versions" | "assistant" | "team">("inspector");
  useLiveCollaboration(projectId, loaded);
  useEffect(()=>{const show=()=>{setTab("versions");setRightOpen(true);};window.addEventListener("editor:versions",show);return()=>window.removeEventListener("editor:versions",show);},[]);

  useEffect(() => {
    let active = true;
    Promise.all([loadArchitecture(projectId), projects.get(projectId)]).then(([{ document, revision }, project]) => {
      if (!active) return;
      useEditor.setState({ projectId, revision, projectName: project.name });
      useEditor.getState().loadDoc(document, "backend");
      setLoaded(true);
    }).catch((error: unknown) => {
      if (!active) return;
      setLoadError(error instanceof Error ? error.message : "Could not load the architecture.");
    });
    if (window.innerWidth < 1100) setLeftOpen(false);
    if (window.innerWidth < 800) setRightOpen(false);
    return () => { active = false; };
  }, [projectId]);

  return (
    <ReactFlowProvider>
      <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
        {leftOpen && <NavSidebar />}
        <div className="flex min-w-0 flex-1 flex-col">
          {loaded && <Toolbar onToggleLeft={() => setLeftOpen((o) => !o)} onToggleRight={() => setRightOpen((o) => !o)} />}
          <div className="flex min-h-0 flex-1">
            {loaded && <div className="hidden md:flex"><PalettePanel /></div>}
            <main className="relative flex min-w-0 flex-1 flex-col bg-canvas" aria-label="Architecture canvas">
              {loaded ? <><div className="min-h-0 flex-1"><ArchitectureCanvas /></div><SimulationPanel /></> : loadError ? (
                <div className="grid h-full place-items-center p-6 text-center">
                  <div className="max-w-sm space-y-2">
                    <p className="text-sm font-medium text-destructive">Could not load architecture</p>
                    <p className="text-xs text-muted-foreground">{loadError}</p>
                    <button onClick={() => window.location.reload()} className="rounded-md border border-border px-3 py-1.5 text-xs hover:bg-accent">Try again</button>
                  </div>
                </div>
              ) : <div className="grid h-full place-items-center text-xs text-muted-foreground">Loading architecture…</div>}
            </main>
            {loaded && (
              <div className={`${rightOpen ? "flex" : "hidden"} absolute inset-y-12 right-0 z-30 border-l border-border bg-sidebar shadow-2xl lg:static lg:shadow-none`}>
                <section className="flex w-[min(360px,90vw)] flex-col border-r border-border" aria-label="Properties">
                  <div className="flex h-11 shrink-0 items-center gap-3 border-b border-border px-3 text-[10px] font-semibold uppercase tracking-[0.12em]">
                    <button onClick={() => setTab("inspector")} className={`h-full border-b-2 ${tab === "inspector" ? "border-primary text-foreground" : "border-transparent text-muted-foreground"}`}>Inspector</button>
                    <button onClick={() => setTab("review")} className={`h-full border-b-2 ${tab === "review" ? "border-primary text-foreground" : "border-transparent text-muted-foreground"}`}>AI Review</button>
                    <button onClick={()=>setTab("versions")} className={tab === "versions" ? "text-primary" : "text-muted-foreground"}>Versions</button>
                    <button onClick={()=>setTab("assistant")} className={tab === "assistant" ? "text-primary" : "text-muted-foreground"}>Assistant</button>
                    <button onClick={()=>setTab("team")} className={tab === "team" ? "text-primary" : "text-muted-foreground"}>Team chat</button>
                  </div>
                  <div className="relative flex-1 overflow-y-auto p-4">
                    {/* All panels stay mounted so chats, reviews and assistant runs survive switching tabs. */}
                    <div className={tab === "inspector" ? "" : "hidden"}><Inspector /></div>
                    <div className={tab === "review" ? "-m-4" : "hidden"}><ReviewPanel /></div>
                    <div className={tab === "versions" ? "-m-4" : "hidden"}><VersionPanel /></div>
                    <div className={tab === "assistant" ? "-m-4" : "hidden"}><AIPanel /></div>
                    <div className={tab === "team" ? "-m-4 h-[calc(100%+2rem)]" : "hidden"}><TeamChatPanel /></div>
                  </div>
                </section>

              </div>
            )}
          </div>
        </div>
      </div>
      <Toaster theme="dark" position="bottom-center" />
    </ReactFlowProvider>
  );
}
