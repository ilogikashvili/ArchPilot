import type { ArchitectureComponent, ArchitectureDocument, ArchitectureRelationship } from "@/features/architecture/model/types";

/**
 * Packet-Tracer-style request simulation. Each component type behaves like a small mock
 * server: it receives a packet, transforms it, forwards it along its outgoing connections
 * and (for synchronous connections) sends a response packet back.
 */
export type Packet = Record<string, unknown>;

export interface SimEvent {
  index: number;
  kind: "request" | "response" | "async";
  edgeId: string;
  fromId: string;
  toId: string;
  fromLabel: string;
  toLabel: string;
  protocol: string;
  data: Packet;
  note: string;
}

export interface NodeIO { nodeId: string; label: string; type: string; input: Packet; output: Packet; note: string }

export interface SimResult { events: SimEvent[]; io: NodeIO[]; warnings: string[] }

export interface SimRequest { sourceId: string; method: string; path: string; body: Packet }

const MAX_EVENTS = 80;

let counter = 0;
const rid = () => `req-${Date.now().toString(36)}-${(++counter).toString(36)}`;
const slug = (s: string) => s.toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "") || "item";

function protocolFor(edge: ArchitectureRelationship, target: ArchitectureComponent) {
  if (edge.label) return edge.label;
  if (target.type === "database") return "SQL";
  if (target.type === "cache") return "RESP";
  if (target.type === "queue") return "AMQP";
  if (edge.type === "event") return "Event";
  return "HTTP";
}

export function simulate(doc: ArchitectureDocument, req: SimRequest): SimResult {
  const byId = new Map(doc.nodes.map((n) => [n.id, n]));
  const events: SimEvent[] = [];
  const io: NodeIO[] = [];
  const warnings: string[] = [];
  const deferred: (() => void)[] = [];
  const cacheStore = new Map<string, Packet>();
  const requestId = rid();

  const source = byId.get(req.sourceId);
  if (!source) return { events, io, warnings: ["Pick a component to send the request from."] };

  const out = (n: ArchitectureComponent) => doc.edges.filter((e) => e.sourceId === n.id && byId.has(e.targetId));

  const push = (kind: SimEvent["kind"], edge: ArchitectureRelationship, from: ArchitectureComponent, to: ArchitectureComponent, data: Packet, note: string) => {
    if (events.length >= MAX_EVENTS) return false;
    events.push({ index: events.length, kind, edgeId: edge.id, fromId: from.id, toId: to.id, fromLabel: from.label, toLabel: to.label, protocol: protocolFor(edge, byId.get(edge.targetId) ?? to), data: structuredClone(data), note });
    return true;
  };

  /** Run one component as a mock server and return its response. */
  const visit = (node: ArchitectureComponent, input: Packet, path: Set<string>, caller?: ArchitectureComponent): Packet => {
    if (path.has(node.id)) {
      warnings.push(`Loop detected at ${node.label}; stopped following it.`);
      return { status: 508, error: "Loop detected" };
    }
    const nextPath = new Set(path).add(node.id);
    const tech = typeof node.properties?.technology === "string" ? node.properties.technology : undefined;
    let output: Packet;
    let note = "";

    const forward = (payload: Packet) => {
      const results: Record<string, Packet> = {};
      for (const edge of out(node)) {
        const target = byId.get(edge.targetId)!;
        const isAsync = edge.type === "async" || edge.type === "event" || target.type === "queue" && edge.type !== "sync";
        if (isAsync) {
          if (!push("async", edge, node, target, payload, `${node.label} → ${target.label} (fire-and-forget)`)) break;
          deferred.push(() => { visit(target, payload, nextPath, node); });
          results[target.label] = { status: 202, accepted: true };
        } else {
          if (!push("request", edge, node, target, payload, `${node.label} calls ${target.label}`)) break;
          const response = visit(target, payload, nextPath, node);
          push("response", edge, target, node, response, `${target.label} replies to ${node.label}`);
          results[target.label] = response;
        }
      }
      return results;
    };

    switch (node.type) {
      case "web": {
        const httpReq = { method: req.method, path: req.path, headers: { "content-type": "application/json", "x-request-id": requestId }, body: input };
        const downstream = forward(httpReq);
        output = { status: 200, rendered: `${node.label} shows the result`, responses: downstream };
        note = "Browser/app builds an HTTP request and renders the reply.";
        break;
      }
      case "api": {
        const headers = { ...(input["headers"] as object ?? {}), "x-request-id": requestId, "x-authenticated-user": "user-123", "x-gateway": node.label };
        const routed = { ...input, headers, routedBy: node.label };
        const downstream = forward(routed);
        const statuses = Object.values(downstream).map((r) => Number(r["status"] ?? 200));
        output = { status: statuses.length ? Math.max(...statuses) : 404, headers: { "x-request-id": requestId }, body: Object.keys(downstream).length ? downstream : { error: "No route" } };
        note = "Gateway authenticates, adds tracing headers and routes to services.";
        break;
      }
      case "service": {
        const body = (input["body"] as Packet) ?? input;
        const processed = { ...body, processedBy: node.label, processedAt: new Date().toISOString(), ...(tech ? { runtime: tech } : {}) };
        const downstream = forward({ operation: `${req.method} ${req.path}`, requestId, data: processed });
        output = { status: 200, data: processed, ...(Object.keys(downstream).length ? { dependencies: downstream } : {}) };
        note = "Service validates input, applies business logic and calls its dependencies.";
        break;
      }
      case "database": {
        const table = slug(caller?.label?.replace(/service|api/gi, "") ?? "records") + "s";
        const write = req.method !== "GET";
        output = write
          ? { status: 200, query: `INSERT INTO ${table} (...) VALUES (...) RETURNING id`, rowsAffected: 1, id: Math.floor(Math.random() * 9000) + 1000 }
          : { status: 200, query: `SELECT * FROM ${table} WHERE ...`, rows: [{ id: 1042, ...((input["data"] as Packet) ?? {}) }] };
        note = `${tech ?? "Database"} executes the query.`;
        break;
      }
      case "cache": {
        const key = `${req.method}:${req.path}`;
        const hit = cacheStore.get(key);
        if (hit) { output = { status: 200, cache: "HIT", key, value: hit }; note = "Value served from cache."; }
        else { cacheStore.set(key, input); output = { status: 200, cache: "MISS", key, stored: true, ttlSeconds: 300 }; note = "Cache miss — value stored for next time."; }
        break;
      }
      case "queue": {
        const message = { messageId: rid(), topic: slug(node.label), payload: input, enqueuedAt: new Date().toISOString() };
        forward(message);
        output = { status: 202, messageId: message.messageId, topic: message.topic };
        note = "Broker stores the message and delivers it to consumers.";
        break;
      }
      case "external": {
        output = { status: 200, provider: node.label, reference: `${slug(node.label)}_${Math.random().toString(36).slice(2, 10)}` };
        note = "Third-party API replies.";
        break;
      }
      default:
        output = { status: 200, echo: input };
        note = "Generic component echoes the input.";
    }

    io.push({ nodeId: node.id, label: node.label, type: node.type, input: structuredClone(input), output: structuredClone(output), note });
    return output;
  };

  visit(source, req.body, new Set());
  while (deferred.length && events.length < MAX_EVENTS) deferred.shift()!();
  if (events.length >= MAX_EVENTS) warnings.push(`Stopped after ${MAX_EVENTS} hops.`);
  if (!out(source).length) warnings.push(`${source.label} has no outgoing connections — connect it to other components.`);
  return { events, io, warnings };
}
