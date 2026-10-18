import { useEffect, useState } from "react";
import { ArrowDownLeft, ArrowUpRight, Database, MousePointerClick, Trash2 } from "lucide-react";
import { useEditor } from "@/store/editorStore";
import { KIND_META, RELATIONSHIP_META } from "@/features/architecture/components/kinds";
import type { ArchitectureComponent, ArchitectureRelationship, RelationshipKind } from "@/features/architecture/model/types";
import { fieldClass } from "@/app/ui";

export function Inspector() {
  const selection = useEditor((s) => s.selection);
  const doc = useEditor((s) => s.doc);
  if (selection?.type === "component") {
    const c = doc.nodes.find((x) => x.id === selection.id);
    if (c) return <ComponentInspector key={c.id} c={c} />;
  }
  if (selection?.type === "relationship") {
    const r = doc.edges.find((x) => x.id === selection.id);
    if (r) return <RelationshipInspector key={r.id} r={r} />;
  }
  return <Summary />;
}

function Summary() {
  const doc = useEditor((s) => s.doc);
  const counts = Object.entries(
    doc.nodes.reduce<Record<string, number>>((a, c) => ({ ...a, [c.type]: (a[c.type] ?? 0) + 1 }), {}),
  );
  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-sm font-semibold">{String(doc.metadata["name"] ?? "Architecture")}</h2>
        <p className="mt-0.5 text-xs text-muted-foreground">{doc.nodes.length} components · {doc.edges.length} relationships</p>
      </div>
      <div className="grid grid-cols-2 gap-2">
        {counts.map(([k, n]) => (
          <div key={k} className="rounded-md border border-border bg-background/40 px-2.5 py-2">
            <div className="text-lg font-semibold" style={{ color: (KIND_META[k as keyof typeof KIND_META] ?? KIND_META.service).color }}>{n}</div>
            <div className="text-[11px] text-muted-foreground">{(KIND_META[k as keyof typeof KIND_META] ?? KIND_META.service).label}</div>
          </div>
        ))}
      </div>
      <div className="flex gap-2.5 rounded-md border border-dashed border-border p-3 text-xs text-muted-foreground">
        <MousePointerClick className="h-4 w-4 shrink-0" />
        Select a component or relationship on the canvas to inspect and edit it.
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-medium text-foreground/90">{label}</span>
      {children}
    </label>
  );
}

/** Text input that commits on blur so each edit is one undo step. */
function useDraft(value: string, commit: (v: string) => void) {
  const [v, setV] = useState(value);
  useEffect(() => setV(value), [value]);
  return { value: v, onChange: (e: { target: { value: string } }) => setV(e.target.value), onBlur: () => v !== value && commit(v) };
}

function ComponentInspector({ c }: { c: ArchitectureComponent }) {
  const { updateComponent, deleteSelection, select } = useEditor.getState();
  const doc = useEditor((s) => s.doc);
  const name = useDraft(c.label, (name) => updateComponent(c.id, { label: name }));
  const tech = useDraft(c.properties.technology ?? "", (technology) => updateComponent(c.id, { properties: { ...c.properties, technology } }));
  const desc = useDraft(c.description, (description) => updateComponent(c.id, { description }));
  const resp = useDraft((c.properties.responsibilities ?? []).join("\n"), (v) =>
    updateComponent(c.id, { properties: { ...c.properties, responsibilities: v.split("\n").map((s) => s.trim()).filter(Boolean) } }),
  );
  const byId = (id: string) => doc.nodes.find((x) => x.id === id);
  const outgoing = doc.edges.filter((r) => r.sourceId === c.id);
  const incoming = doc.edges.filter((r) => r.targetId === c.id);
  const dataStores = outgoing.map((r) => byId(r.targetId)).filter((x) => x?.type === "database");

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-base font-semibold">{c.label}</h2>
        <p className="text-xs" style={{ color: (KIND_META[c.type as keyof typeof KIND_META] ?? KIND_META.service).color }}>{(KIND_META[c.type as keyof typeof KIND_META] ?? KIND_META.service).label}</p>
      </div>
      <Field label="Name"><input className={fieldClass} {...name} /></Field>
      <Field label="Technology"><input className={fieldClass} {...tech} placeholder="e.g. Spring Boot" /></Field>
      <Field label="Description"><textarea rows={3} className={fieldClass} {...desc} /></Field>
      <Field label="Responsibilities (one per line)"><textarea rows={3} className={`${fieldClass} font-mono`} {...resp} /></Field>
      {(c.properties.responsibilities ?? []).length > 0 && (
        <ul className="space-y-1 pl-1 text-xs text-muted-foreground">
          {(c.properties.responsibilities ?? []).map((r) => <li key={r} className="flex gap-2"><span className="text-primary">•</span>{r}</li>)}
        </ul>
      )}
      <RelList title="Depends On" icon={<ArrowUpRight className="h-3 w-3" />} items={outgoing.map((r) => ({ r, other: byId(r.targetId) }))} onPick={(id) => select({ type: "relationship", id })} />
      <RelList title="Used By" icon={<ArrowDownLeft className="h-3 w-3" />} items={incoming.map((r) => ({ r, other: byId(r.sourceId) }))} onPick={(id) => select({ type: "relationship", id })} />
      {dataStores.length > 0 && (
        <div>
          <div className="mb-1.5 text-xs font-medium">Data Stores</div>
          {dataStores.map((d) => (
            <span key={d!.id} className="mr-1 inline-flex items-center gap-1.5 rounded-md border border-border bg-background/40 px-2 py-1 text-xs"><Database className="h-3 w-3" />{d!.label}</span>
          ))}
        </div>
      )}
      <DeleteButton onClick={() => confirm(`Delete “${c.label}” and its relationships?`) && deleteSelection()} />
    </div>
  );
}

function RelList({ title, icon, items, onPick }: { title: string; icon: React.ReactNode; items: { r: ArchitectureRelationship; other: ArchitectureComponent | undefined }[]; onPick: (id: string) => void }) {
  return (
    <div>
      <div className="mb-1.5 text-xs font-medium">{title}</div>
      {items.length === 0 ? (
        <p className="text-xs text-muted-foreground">None</p>
      ) : (
        <ul className="space-y-1">
          {items.map(({ r, other }) => (
            <li key={r.id}>
              <button onClick={() => onPick(r.id)} className="flex w-full items-center gap-2 rounded-md border border-border bg-background/40 px-2 py-1.5 text-left text-xs hover:border-ring">
                <span className="text-muted-foreground">{icon}</span>
                <span className="flex-1 truncate">{other?.label ?? "Unknown"}</span>
                {r.label && <span className="font-mono text-[10px] text-muted-foreground">{r.label}</span>}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function RelationshipInspector({ r }: { r: ArchitectureRelationship }) {
  const { updateRelationship, deleteSelection, select } = useEditor.getState();
  const doc = useEditor((s) => s.doc);
  const label = useDraft(r.label ?? "", (v) => updateRelationship(r.id, { label: v }));
  const src = doc.nodes.find((c) => c.id === r.sourceId);
  const tgt = doc.nodes.find((c) => c.id === r.targetId);
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-base font-semibold">Relationship</h2>
        <p className="text-xs text-muted-foreground">{(RELATIONSHIP_META[r.type as keyof typeof RELATIONSHIP_META] ?? RELATIONSHIP_META.sync).label}</p>
      </div>
      <div className="flex items-center gap-2 text-xs">
        <button onClick={() => select({ type: "component", id: r.sourceId })} className="flex-1 truncate rounded-md border border-border bg-background/40 px-2 py-1.5 text-left hover:border-ring">{src?.label}</button>
        <ArrowUpRight className="h-3.5 w-3.5 rotate-45 text-muted-foreground" />
        <button onClick={() => select({ type: "component", id: r.targetId })} className="flex-1 truncate rounded-md border border-border bg-background/40 px-2 py-1.5 text-left hover:border-ring">{tgt?.label}</button>
      </div>
      <Field label="Relationship type">
        <select className={fieldClass} value={r.type} onChange={(e) => updateRelationship(r.id, { type: e.target.value as RelationshipKind })}>
          {(Object.keys(RELATIONSHIP_META) as RelationshipKind[]).map((k) => <option key={k} value={k}>{RELATIONSHIP_META[k].label}</option>)}
        </select>
      </Field>
      <Field label="Label"><input className={fieldClass} {...label} placeholder="REST, SQL, Event…" /></Field>
      <DeleteButton onClick={deleteSelection} />
    </div>
  );
}

function DeleteButton({ onClick }: { onClick: () => void }) {
  return (
    <button onClick={onClick} className="flex w-full items-center justify-center gap-1.5 rounded-md border border-destructive/40 py-1.5 text-xs text-destructive hover:bg-destructive/10">
      <Trash2 className="h-3.5 w-3.5" /> Delete
    </button>
  );
}
