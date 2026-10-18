import { useEffect, useMemo, useState } from "react";
import { FileCode2, Folder, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { downloadStructure, previewStructure, type CodegenFile, type CodegenLayout } from "@/api/codegen";
import { fieldClass } from "@/app/ui";
import { liveSession } from "@/features/collab/liveSync";

const LAYOUTS: { id: CodegenLayout; title: string; text: string }[] = [
  { id: "multi-module", title: "One multi-module project", text: "A single Gradle build with a module per component, shared code and one docker-compose." },
  { id: "per-service", title: "One project per service", text: "Each service is its own independent Spring Boot project with its own build and Dockerfile." },
];

/** Choose a layout, preview the generated Java file tree and download it as a zip. */
export function DownloadStructureDialog({ projectId, open, onOpenChange }: { projectId: string; open: boolean; onOpenChange: (o: boolean) => void }) {
  const [layout, setLayout] = useState<CodegenLayout>("multi-module");
  const [groupId, setGroupId] = useState("com.example");
  const [files, setFiles] = useState<CodegenFile[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState("");
  const validGroup = /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$/.test(groupId);

  useEffect(() => {
    if (!open || !validGroup) return;
    let active = true;
    setLoading(true); setError("");
    liveSession.flushNow();
    const t = setTimeout(() => {
      previewStructure(projectId, layout, groupId)
        .then((r) => { if (!active) return; setFiles(r.files); setSelected((s) => r.files.some((f) => f.path === s) ? s : r.files.find((f) => f.path.endsWith("README.md"))?.path ?? r.files[0]?.path ?? null); })
        .catch((e) => active && setError(e instanceof Error ? e.message : "Could not build the preview"))
        .finally(() => active && setLoading(false));
    }, 300);
    return () => { active = false; clearTimeout(t); };
  }, [open, layout, groupId, projectId, validGroup]);

  const tree = useMemo(() => files.map((f) => ({ ...f, depth: f.path.split("/").length - 1, name: f.path.split("/").pop()!, dir: f.path.split("/").slice(0, -1).join("/") })), [files]);
  const content = files.find((f) => f.path === selected)?.content ?? "";

  const download = async () => {
    setDownloading(true);
    try { await downloadStructure(projectId, layout, groupId); toast.success("Project structure downloaded"); }
    catch (e) { toast.error(e instanceof Error ? e.message : "Download failed"); }
    finally { setDownloading(false); }
  };

  let lastDir = "";
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl">
        <DialogHeader>
          <DialogTitle>Download structure</DialogTitle>
          <DialogDescription>Java (Spring Boot) starter code generated from this diagram. Every file has starter code you can build on.</DialogDescription>
        </DialogHeader>
        <div className="grid gap-3 md:grid-cols-2">
          {LAYOUTS.map((l) => (
            <button key={l.id} onClick={() => setLayout(l.id)} aria-pressed={layout === l.id}
              className={`rounded-lg border p-3 text-left text-xs ${layout === l.id ? "border-primary bg-primary/10" : "border-border hover:bg-accent"}`}>
              <p className="font-semibold">{l.title}</p>
              <p className="mt-1 text-muted-foreground">{l.text}</p>
            </button>
          ))}
        </div>
        <label className="flex items-center gap-2 text-xs">
          <span className="w-28 text-muted-foreground">Base package</span>
          <input value={groupId} onChange={(e) => setGroupId(e.target.value.trim())} className={fieldClass} aria-label="Base package" />
        </label>
        {!validGroup && <p className="text-xs text-destructive">Use a package name like com.example</p>}
        {error && <p className="text-xs text-destructive">{error}</p>}
        <div className="grid h-[380px] grid-cols-[280px_1fr] overflow-hidden rounded-lg border border-border text-xs">
          <ul className="overflow-y-auto border-r border-border p-2" aria-label="Generated files">
            {loading && !files.length && <li className="flex items-center gap-2 text-muted-foreground"><Loader2 className="h-3 w-3 animate-spin" /> Generating…</li>}
            {tree.map((f) => {
              const showDir = f.dir !== lastDir;
              lastDir = f.dir;
              return (
                <li key={f.path}>
                  {showDir && <div className="mt-1 flex items-center gap-1 truncate text-muted-foreground"><Folder className="h-3 w-3 shrink-0" />{f.dir}/</div>}
                  <button onClick={() => setSelected(f.path)} className={`flex w-full items-center gap-1 truncate rounded py-0.5 pl-4 text-left ${selected === f.path ? "bg-accent" : "hover:bg-accent/50"}`}>
                    <FileCode2 className="h-3 w-3 shrink-0" />{f.name}
                  </button>
                </li>
              );
            })}
          </ul>
          <pre className="overflow-auto p-3 font-mono text-[11px] leading-relaxed">{content}</pre>
        </div>
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>{files.length} files</span>
          <button onClick={download} disabled={downloading || !validGroup || !files.length}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 font-medium text-primary-foreground disabled:opacity-50">
            {downloading && <Loader2 className="h-3.5 w-3.5 animate-spin" />} Download .zip
          </button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
