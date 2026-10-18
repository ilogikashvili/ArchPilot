import { describe, expect, it } from "vitest";
import type { ArchitectureDocument } from "@/features/architecture/model/types";
import { simulate } from "./engine";

const n = (id: string, type: string) => ({ id, type, label: id, description: "", x: 0, y: 0, properties: {} });
const doc: ArchitectureDocument = {
  schemaVersion: "1.0",
  nodes: [n("web", "web"), n("gw", "api"), n("orders", "service"), n("db", "database"), n("q", "queue"), n("pay", "service")],
  edges: [
    { id: "e1", type: "sync", sourceId: "web", targetId: "gw", properties: {} },
    { id: "e2", type: "sync", sourceId: "gw", targetId: "orders", properties: {} },
    { id: "e3", type: "sync", sourceId: "orders", targetId: "db", properties: {} },
    { id: "e4", type: "async", sourceId: "orders", targetId: "q", properties: {} },
    { id: "e5", type: "event", sourceId: "q", targetId: "pay", properties: {} },
  ],
  viewport: { x: 0, y: 0, zoom: 1 },
  metadata: {},
};

describe("request flow simulation", () => {
  it("sends requests down and responses back up synchronous connections", () => {
    const { events } = simulate(doc, { sourceId: "web", method: "POST", path: "/orders", body: { item: "book" } });
    const sync = events.filter((e) => e.kind !== "async").map((e) => `${e.kind}:${e.fromId}>${e.toId}`);
    expect(sync).toEqual([
      "request:web>gw", "request:gw>orders", "request:orders>db", "response:db>orders",
      "response:orders>gw", "response:gw>web",
    ]);
  });

  it("delivers async messages after the synchronous reply", () => {
    const { events } = simulate(doc, { sourceId: "web", method: "POST", path: "/orders", body: {} });
    const lastSync = events.map((e) => e.kind).lastIndexOf("response");
    const delivery = events.findIndex((e) => e.fromId === "q" && e.toId === "pay");
    expect(delivery).toBeGreaterThan(lastSync);
  });

  it("stops on loops instead of running forever", () => {
    const loop = { ...doc, edges: [...doc.edges, { id: "back", type: "sync", sourceId: "orders", targetId: "gw", properties: {} }] };
    const result = simulate(loop, { sourceId: "web", method: "GET", path: "/", body: {} });
    expect(result.warnings.some((w) => w.includes("Loop"))).toBe(true);
  });
});
