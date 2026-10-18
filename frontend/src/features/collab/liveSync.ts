import { useBranch } from "@/features/workspace/VersionPanel";
import { useEffect } from "react";
import { create } from "zustand";
import { toast } from "sonner";
import { applyOperations, liveSocketUrl } from "@/api/collab";
import { loadArchitecture } from "@/api/architectureRepository";
import { auth } from "@/api/auth";
import { useEditor } from "@/store/editorStore";
import type { ArchitectureDocument } from "@/features/architecture/model/types";
import { applyOps, diffDocs, type Operation } from "./ops";

export interface Collaborator { connectionId: string; userId: string; name: string; email?: string; agent?: boolean }
export interface RemoteCursor { x: number; y: number; name: string; agent?: boolean; at: number }

interface CollabState {
  connected: boolean;
  self: Collaborator | null;
  users: Collaborator[];
  cursors: Record<string, RemoteCursor>;
  selections: Record<string, string | null>;
  activity: { id: number; text: string; agent?: boolean | undefined; at: number }[];
}

export const useCollab = create<CollabState>(() => ({ connected: false, self: null, users: [], cursors: {}, selections: {}, activity: [] }));

const PALETTE = ["#f97316", "#22c55e", "#3b82f6", "#e11d48", "#a855f7", "#14b8a6", "#eab308", "#ec4899"];
export function colorFor(id: string) {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) | 0;
  return PALETTE[Math.abs(h) % PALETTE.length];
}

/** Module-level handle so other parts of the editor can talk to the live session. */
let session: {
  sendCursor: (x: number, y: number) => void;
  sendSelection: (id: string | null) => void;
  sendNodeMove: (id: string, x: number, y: number) => void;
  flushNow: () => void;
  adopt: (doc: ArchitectureDocument, revision: number) => void;
} | null = null;

export const liveSession = {
  sendCursor: (x: number, y: number) => session?.sendCursor(x, y),
  sendSelection: (id: string | null) => session?.sendSelection(id),
  sendNodeMove: (id: string, x: number, y: number) => session?.sendNodeMove(id, x, y),
  flushNow: () => session?.flushNow(),
  /** Replace local state with a document that the server already has (reset, restore...). */
  adopt: (doc: ArchitectureDocument, revision: number) => session?.adopt(doc, revision),
};

let activityId = 0;
function pushActivity(text: string, agent?: boolean | undefined) {
  useCollab.setState((s) => ({ activity: [{ id: ++activityId, text, agent, at: Date.now() }, ...s.activity].slice(0, 30) }));
}

function describe(ops: Operation[]) {
  const counts: Record<string, number> = {};
  for (const o of ops) counts[o.op] = (counts[o.op] ?? 0) + 1;
  const parts: string[] = [];
  const add = (k: string, word: string) => counts[k] && parts.push(`${word} ${counts[k]} ${k.startsWith("node") ? "component" : "connection"}${counts[k] > 1 ? "s" : ""}`);
  add("node.add", "added"); add("node.remove", "removed"); add("edge.add", "added"); add("edge.remove", "removed");
  if (!parts.length && (counts["node.update"] || counts["edge.update"])) parts.push("edited the diagram");
  return parts.join(", ");
}

/**
 * Figma-style live editing: every local change is diffed against the last server-confirmed
 * document ("shadow") and streamed as operations ~every 120ms; remote operations from other
 * people and AI agents are merged in as they arrive. There is no Save button.
 */
export function useLiveCollaboration(projectId: string, enabled: boolean) {
  useEffect(() => {
    if (!enabled) return;
    const clientId = crypto.randomUUID();
    let shadow = useEditor.getState().doc;
    let inflight = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let applyingRemote = false;
    let ws: WebSocket | null = null;
    let closed = false;
    let retry: ReturnType<typeof setTimeout> | undefined;
    let lastCursor = 0;
    let lastMove = 0;
    const setStatus = useEditor.getState().setSaveStatus;

    const replaceLocal = (doc: ArchitectureDocument) => {
      applyingRemote = true;
      useEditor.setState((s) => ({ doc: { ...doc, viewport: s.doc.viewport } }));
      applyingRemote = false;
    };

    const resync = async () => {
      if (useBranch.getState().id) return;
      try {
        const { document, revision } = await loadArchitecture(projectId);
        shadow = document;
        replaceLocal(document);
        useEditor.setState({ revision });
        setStatus("saved");
      } catch { /* the next event or edit retries */ }
    };

    const flush = async () => {
      timer = undefined;
      if (useBranch.getState().id) return;
      if (inflight) { schedule(); return; }
      const sentFrom = shadow;
      const ops = diffDocs(sentFrom, useEditor.getState().doc);
      if (!ops.length) { setStatus("saved"); return; }
      inflight = true;
      setStatus("saving");
      try {
        const res = await applyOperations(projectId, clientId, ops);
        // Converge on the server's document, then re-apply anything typed while the request was in flight.
        const pending = diffDocs(applyOps(sentFrom, ops), useEditor.getState().doc);
        shadow = res.document;
        replaceLocal(applyOps(res.document, pending));
        useEditor.setState({ revision: res.revision });
        setStatus(pending.length ? "unsaved" : "saved");
        if (pending.length) schedule();
      } catch (error) {
        setStatus("error");
        toast.error(error instanceof Error ? error.message : "Could not sync changes");
        await resync();
      } finally {
        inflight = false;
      }
    };
    const schedule = () => { if (!timer) timer = setTimeout(flush, 50); };

    const unsubscribe = useEditor.subscribe((s, prev) => {
      if (s.doc !== prev.doc && !applyingRemote) schedule();
    });

    const send = (msg: unknown) => { if (ws?.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg)); };

    const connect = async () => {
      if (closed) return;
      try { await auth.me(); } catch { /* refreshes the token when needed */ }
      const token = localStorage.getItem("accessToken");
      if (!token || closed) return;
      ws = new WebSocket(liveSocketUrl(projectId, token));
      ws.onopen = () => { useCollab.setState({ connected: true }); void resync(); };
      ws.onclose = () => {
        useCollab.setState({ connected: false, users: [], cursors: {} });
        if (!closed) retry = setTimeout(connect, 2000);
      };
      ws.onmessage = (event) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let msg: any;
        try { msg = JSON.parse(event.data); } catch { return; }
        const selfId = useCollab.getState().self?.connectionId;
        switch (msg.type) {
          case "presence":
            useCollab.setState({ self: msg.self, users: msg.users });
            break;
          case "user.joined":
            useCollab.setState((s) => ({ users: [...s.users.filter((u) => u.connectionId !== msg.user.connectionId), msg.user] }));
            pushActivity(`${msg.user.name} joined`, msg.user.agent);
            break;
          case "user.left":
            useCollab.setState((s) => {
              const cursors = { ...s.cursors }; delete cursors[msg.user.connectionId];
              const selections = { ...s.selections }; delete selections[msg.user.connectionId];
              return { users: s.users.filter((u) => u.connectionId !== msg.user.connectionId), cursors, selections };
            });
            pushActivity(`${msg.user.name} left`, msg.user.agent);
            break;
          case "cursor.moved":
            if (msg.user.connectionId === selfId) break;
            useCollab.setState((s) => ({ cursors: { ...s.cursors, [msg.user.connectionId]: { x: msg.payload.x, y: msg.payload.y, name: msg.user.name, agent: msg.user.agent, at: Date.now() } } }));
            break;
          case "node.updated": {
            // Transient drag preview from another collaborator: move the box live, without saving.
            if (msg.user?.connectionId === selfId) break;
            const p = msg.payload ?? {};
            if (typeof p.id !== "string" || typeof p.x !== "number" || typeof p.y !== "number") break;
            applyingRemote = true;
            useEditor.setState((s) => ({ doc: { ...s.doc, nodes: s.doc.nodes.map((n) => (n.id === p.id ? { ...n, x: p.x, y: p.y } : n)) } }));
            applyingRemote = false;
            useCollab.setState((s) => ({ cursors: s.cursors[msg.user.connectionId] ? { ...s.cursors, [msg.user.connectionId]: { ...s.cursors[msg.user.connectionId], at: Date.now() } } : s.cursors }));
            break;
          }
          case "selection.changed":
            useCollab.setState((s) => ({ selections: { ...s.selections, [msg.user.connectionId]: msg.payload?.id ?? null } }));
            break;
          case "operations.applied": {
            useEditor.setState({ revision: Math.max(useEditor.getState().revision, msg.revision) });
            if (msg.clientId === clientId) break;
            const ops = msg.operations as Operation[];
            shadow = applyOps(shadow, ops);
            replaceLocal(applyOps(useEditor.getState().doc, ops));
            const who = useCollab.getState().users.find((u) => u.userId === msg.userId);
            const text = describe(ops);
            if (who && !ops.every((o) => o.op === "node.update" && Object.keys(o.patch).every((k) => k === "x" || k === "y")))
              pushActivity(`${who.name} ${text}`, who.agent);
            break;
          }
          case "chat.message":
          case "chat.typing":
            window.dispatchEvent(new CustomEvent("collab:chat", { detail: msg }));
            break;
          case "architecture.updated":
            if (!msg.branchId && !inflight) void resync();
            break;
        }
      };
    };
    void connect();

    session = {
      sendCursor: (x, y) => {
        const now = Date.now();
        if (now - lastCursor < 25) return;
        lastCursor = now;
        send({ type: "cursor.moved", payload: { x, y } });
      },
      sendSelection: (id) => send({ type: "selection.changed", payload: { id } }),
      sendNodeMove: (id, x, y) => {
        const now = Date.now();
        if (now - lastMove < 30) return;
        lastMove = now;
        send({ type: "node.updated", payload: { id, x, y } });
      },
      flushNow: () => { if (timer) clearTimeout(timer); timer = undefined; void flush(); },
      adopt: (doc, revision) => { shadow = doc; replaceLocal(doc); useEditor.setState({ revision }); setStatus("saved"); },
    };

    const stale = setInterval(() => {
      const cutoff = Date.now() - 15_000;
      useCollab.setState((s) => ({ cursors: Object.fromEntries(Object.entries(s.cursors).filter(([, c]) => c.at > cutoff)) }));
    }, 5000);

    return () => {
      closed = true;
      session = null;
      unsubscribe();
      clearInterval(stale);
      if (timer) { clearTimeout(timer); void flush(); }
      if (retry) clearTimeout(retry);
      ws?.close();
      useCollab.setState({ connected: false, self: null, users: [], cursors: {}, selections: {} });
    };
  }, [projectId, enabled]);
}
