import { describe, expect, it } from "vitest";
import type { ArchitectureDocument } from "@/features/architecture/model/types";
import { applyOps, diffDocs } from "./ops";

const base: ArchitectureDocument = {
  schemaVersion: "1.0",
  nodes: [
    { id: "a", type: "service", label: "A", description: "", x: 0, y: 0, properties: {} },
    { id: "b", type: "database", label: "B", description: "", x: 100, y: 0, properties: {} },
  ],
  edges: [{ id: "e", type: "sync", sourceId: "a", targetId: "b", label: "SQL", properties: {} }],
  viewport: { x: 0, y: 0, zoom: 1 },
  metadata: {},
};

describe("live collaboration operations", () => {
  it("diff then apply reproduces the edited document", () => {
    const edited: ArchitectureDocument = {
      ...base,
      nodes: [{ ...base.nodes[0]!, x: 50, label: "A2" }, { id: "c", type: "cache", label: "C", description: "", x: 1, y: 2, properties: {} }],
      edges: [],
    };
    const ops = diffDocs(base, edited);
    const result = applyOps(base, ops);
    expect(result.nodes).toEqual(edited.nodes);
    expect(result.edges).toEqual([]);
  });

  it("only sends changed fields for a moved node", () => {
    const moved = { ...base, nodes: [{ ...base.nodes[0]!, x: 10, y: 20 }, base.nodes[1]!] };
    expect(diffDocs(base, moved)).toEqual([{ op: "node.update", id: "a", patch: { x: 10, y: 20 } }]);
  });

  it("removing a node also removes its connections", () => {
    const result = applyOps(base, [{ op: "node.remove", id: "b" }]);
    expect(result.nodes.map((n) => n.id)).toEqual(["a"]);
    expect(result.edges).toEqual([]);
  });

  it("is idempotent when the same operations arrive twice", () => {
    const ops = [{ op: "node.add" as const, node: { id: "z", type: "queue", label: "Z" } }];
    const once = applyOps(base, ops);
    expect(applyOps(once, ops)).toBe(once);
  });
});
