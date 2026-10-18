import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { projects, type Project } from "@/api/projects";
import { AccountMenu } from "@/features/auth/AuthGuard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
export function ProjectsPage() {
 const [items, setItems] = useState<Project[]>([]); const [name, setName] = useState(""); const [error, setError] = useState(""); const [busy, setBusy] = useState(false); const [loaded, setLoaded] = useState(false);
 async function load() { setItems(await projects.list()); setLoaded(true); }
 async function action(fn: () => Promise<unknown>) { setBusy(true); setError(""); try { await fn(); await load(); } catch(e) { setError(e instanceof Error ? e.message : "Request failed"); } finally { setBusy(false); } }
 useEffect(() => { void action(async () => {}); }, []);
 return <main className="mx-auto max-w-5xl space-y-6 p-8"><header className="flex items-center justify-between"><h1 className="text-2xl font-semibold">Projects</h1><AccountMenu /></header>
 <form className="flex gap-2" onSubmit={e => { e.preventDefault(); void action(async () => { await projects.create(name.trim()); setName(""); }); }}><Input aria-label="Project name" placeholder="New project name" required maxLength={200} value={name} onChange={e => setName(e.target.value)} /><Button disabled={busy || !name.trim()}>Create project</Button></form>
 {error && <div role="alert" className="text-destructive">{error} <Button variant="outline" onClick={() => void action(async () => {})}>Retry</Button></div>}
 {!loaded && !error && <p>Loading projects…</p>}{loaded && !items.length && <p>No projects yet. Create your first architecture.</p>}
 <div className="grid gap-4">{items.map(p => <Card key={p.id}><CardContent className="flex flex-wrap items-center gap-3 p-4"><Link className="mr-auto font-semibold" to="/projects/$projectId" params={{ projectId: p.id }}>{p.name}{p.archived ? " (Archived)" : ""}</Link><Button variant="outline" disabled={busy} onClick={() => { const name = prompt("Project name", p.name); if (name?.trim()) void action(() => projects.rename(p.id, name.trim())); }}>Rename</Button><Button variant="outline" disabled={busy} onClick={() => void action(() => projects.archive(p.id, !p.archived))}>{p.archived ? "Restore" : "Archive"}</Button><Button variant="destructive" disabled={busy} onClick={() => { if(confirm(`Delete ${p.name}?`)) void action(() => projects.remove(p.id)); }}>Delete</Button></CardContent></Card>)}</div></main>;
}
