import type { ArchitectureDocument } from "@/features/architecture/model/types";

/**
 * Fine-grained, conflict-free edit operations. The same format is accepted by
 * POST /projects/{id}/architecture/operations and broadcast to every live client,
 * so people and AI agents can edit the same diagram at the same time.
 */
export type Operation =
  | { op: "node.add"; node: Record<string, unknown> & { id: string } }
  | { op: "node.update"; id: string; patch: Record<string, unknown> }
  | { op: "node.remove"; id: string }
  | { op: "edge.add"; edge: Record<string, unknown> & { id: string } }
  | { op: "edge.update"; id: string; patch: Record<string, unknown> }
  | { op: "edge.remove"; id: string };

const NODE_FIELDS = ["type", "label", "description", "x", "y", "properties"] as const;
const EDGE_FIELDS = ["type", "label", "sourceId", "targetId", "properties"] as const;

const same = (a: unknown, b: unknown) => a === b || JSON.stringify(a) === JSON.stringify(b);

function patchOf(before: Record<string, unknown>, after: Record<string, unknown>, fields: readonly string[]) {
  const patch: Record<string, unknown> = {};
  for (const f of fields) if (!same(before[f], after[f])) patch[f] = after[f];
  return patch;
}

/** Compute the operations that turn `before` into `after` (viewport/metadata are local-only). */
export function diffDocs(before: ArchitectureDocument, after: ArchitectureDocument): Operation[] {
  if (before === after) return [];
  const ops: Operation[] = [];
  const bNodes = new Map(before.nodes.map((n) => [n.id, n]));
  const aNodes = new Map(after.nodes.map((n) => [n.id, n]));
  const bEdges = new Map(before.edges.map((e) => [e.id, e]));
  const aEdges = new Map(after.edges.map((e) => [e.id, e]));

  for (const n of after.nodes) if (!bNodes.has(n.id)) ops.push({ op: "node.add", node: { ...n } });
  for (const n of after.nodes) {
    const prev = bNodes.get(n.id);
    if (!prev || prev === n) continue;
    const patch = patchOf(prev as never, n as never, NODE_FIELDS);
    if (Object.keys(patch).length) ops.push({ op: "node.update", id: n.id, patch });
  }
  for (const e of after.edges) if (!bEdges.has(e.id)) ops.push({ op: "edge.add", edge: { ...e } });
  for (const e of after.edges) {
    const prev = bEdges.get(e.id);
    if (!prev || prev === e) continue;
    const patch = patchOf(prev as never, e as never, EDGE_FIELDS);
    if (Object.keys(patch).length) ops.push({ op: "edge.update", id: e.id, patch });
  }
  for (const e of before.edges) if (!aEdges.has(e.id)) ops.push({ op: "edge.remove", id: e.id });
  for (const n of before.nodes) if (!aNodes.has(n.id)) ops.push({ op: "node.remove", id: n.id });
  return ops;
}

/** Apply operations locally. Idempotent: re-adding an existing id or touching a missing one is a no-op. */
export function applyOps(doc: ArchitectureDocument, ops: Operation[]): ArchitectureDocument {
  let nodes = doc.nodes;
  let edges = doc.edges;
  for (const o of ops) {
    switch (o.op) {
      case "node.add":
        if (!nodes.some((n) => n.id === o.node.id))
          nodes = [...nodes, { description: "", x: 0, y: 0, properties: {}, ...o.node } as never];
        break;
      case "node.update":
        nodes = nodes.map((n) => (n.id === o.id ? { ...n, ...pick(o.patch, NODE_FIELDS) } : n));
        break;
      case "node.remove":
        nodes = nodes.filter((n) => n.id !== o.id);
        edges = edges.filter((e) => e.sourceId !== o.id && e.targetId !== o.id);
        break;
      case "edge.add":
        if (!edges.some((e) => e.id === o.edge.id)) edges = [...edges, { type: "sync", properties: {}, ...o.edge } as never];
        break;
      case "edge.update":
        edges = edges.map((e) => (e.id === o.id ? { ...e, ...pick(o.patch, EDGE_FIELDS) } : e));
        break;
      case "edge.remove":
        edges = edges.filter((e) => e.id !== o.id);
        break;
    }
  }
  return nodes === doc.nodes && edges === doc.edges ? doc : { ...doc, nodes, edges };
}

function pick(patch: Record<string, unknown>, fields: readonly string[]) {
  const out: Record<string, unknown> = {};
  for (const f of fields) if (f in patch) out[f] = patch[f];
  return out;
}
