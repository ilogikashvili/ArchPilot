import { useEffect, useState } from "react";
import { reviews, type Review, type Finding, type Decision, type DecisionValue } from "@/api/reviews";
import { useEditor } from "@/store/editorStore";
import { Button } from "@/components/ui/button";
import { api } from "@/api/client";
const SEV: Record<string, string> = { CRITICAL: "border-destructive/60 bg-destructive/10", WARNING: "border-warning/60 bg-warning/10", INFO: "border-border" };
const SEV_TEXT: Record<string, string> = { CRITICAL: "text-destructive", WARNING: "text-warning", INFO: "text-muted-foreground" };
const RANK: Record<string, number> = { CRITICAL: 0, WARNING: 1, INFO: 2 };
export function ReviewPanel() {
 const pid = useEditor(s => s.projectId);
 const status = useEditor(s => s.saveStatus);
 const [review, setReview] = useState<Review | null>(null);
 const [findings, setFindings] = useState<Finding[]>([]);
 const [decisions, setDecisions] = useState<Decision[]>([]);
 const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
 const [keys, setKeys] = useState<{ id: string; provider: string; model: string }[]>([]); const [keyId, setKeyId] = useState("");
 useEffect(() => { api<{ id: string; provider: string; model: string }[]>("/users/me/ai-keys").then(setKeys).catch(() => {}); }, []);
 const running = review?.status === "RUNNING" || review?.status === "QUEUED";
 useEffect(() => { let active = true; reviews.decisions(pid).then(d => { if(active) setDecisions(d); }).catch(e => { if(active) setError(e.message); }); return () => { active = false; }; }, [pid]);
 useEffect(() => {
  if (!review) return;
  let active = true; let timer: ReturnType<typeof setTimeout>;
  async function poll() {
   try {
    const next = await reviews.get(pid, review!.id);
    if (!active) return;
    setReview(next);
    if (next.status === "QUEUED" || next.status === "RUNNING") timer = setTimeout(poll, 1500);
    else if (next.status === "COMPLETED") { const data = await reviews.findings(pid, next.id); if(active) setFindings([...data].sort((a, b) => (RANK[a.severity] ?? 3) - (RANK[b.severity] ?? 3))); }
    else setError(`Review ${next.status.toLowerCase()}. Try starting another review.`);
   } catch(e) { if(active) setError(e instanceof Error ? e.message : "Review polling failed"); }
  }
  void poll(); return () => { active = false; clearTimeout(timer); };
 }, [pid, review?.id]);
 async function start() {
  setBusy(true); setError(""); setFindings([]);
  try { setReview(await reviews.start(pid, keyId)); } catch(e) { setError(e instanceof Error ? e.message : "Could not start review"); } finally { setBusy(false); }
 }
 async function decide(fid: string, decision: DecisionValue) {
  setBusy(true); setError("");
  try { const result = await reviews.decide(pid, fid, decision); setDecisions(d => [...d.filter(x => x.findingId !== fid), result]); } catch(e) { setError(e instanceof Error ? e.message : "Decision failed"); } finally { setBusy(false); }
 }
 return <div className="h-full space-y-4 overflow-y-auto p-4"><h2 className="text-sm font-semibold">Senior Architecture Review</h2><p className="text-xs text-muted-foreground">Reviews your design like a senior engineer checking a junior's work: security holes, shared data, cycles, cascading failures, missing pieces.</p><label className="block text-xs">Reviewer<select aria-label="Reviewer" className="mt-1 block w-full rounded-md border border-border bg-background p-1.5 text-xs" value={keyId} onChange={e => setKeyId(e.target.value)}><option value="">Built-in senior checks</option>{keys.map(k => <option key={k.id} value={k.id}>Built-in + {k.provider} {k.model} as senior architect</option>)}</select></label><Button size="sm" onClick={start} disabled={busy || (!!running && !error) || status !== "saved"}>{running && !error ? "Review running…" : "Start review"}</Button>{status !== "saved" && <p className="text-xs text-muted-foreground">Save architecture before requesting a review.</p>}
 {error && <p role="alert" className="text-xs text-destructive">{error}</p>}{review && <p className={"rounded-md border p-2 text-xs font-medium " + (review.summary?.startsWith("Changes requested") ? "border-destructive/60 text-destructive" : review.summary?.startsWith("Approve with") ? "border-warning/60 text-warning" : "border-border")}>{review.status === "COMPLETED" ? review.summary : review.status}</p>}
 {review?.status === "COMPLETED" && !findings.length && !error && <p className="text-xs">No findings.</p>}
 {findings.map(f => <article key={f.id} className={"space-y-2 rounded-md border p-3 text-xs " + (SEV[f.severity] ?? "border-border")} onMouseEnter={() => { const doc = useEditor.getState().doc; useEditor.getState().setHighlight(doc.nodes.some(n => n.id === f.affectedElementId) ? [f.affectedElementId!] : [], doc.edges.some(e => e.id === f.affectedElementId) ? [f.affectedElementId!] : []); }} onMouseLeave={() => useEditor.getState().setHighlight([], [])}><div><span className={"font-semibold " + (SEV_TEXT[f.severity] ?? "")}>{f.severity}</span> <span className="text-muted-foreground">· {f.category}</span></div><h3 className="font-semibold">{f.title}</h3><p>{f.description}</p><p><span className="font-semibold">Fix: </span>{f.recommendation}</p><p>{decisions.find(d => d.findingId === f.id)?.decision}</p><div className="flex flex-wrap gap-1">{(["ACCEPTED", "DISMISSED", "POSTPONED"] as const).map(d => <Button key={d} size="sm" variant="outline" disabled={busy} onClick={() => void decide(f.id, d)}>{d.toLowerCase()}</Button>)}</div></article>)}</div>;
}
