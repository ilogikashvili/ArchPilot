import { create } from "zustand";
import type {
  ArchitectureComponent,
  ArchitectureDocument,
  ArchitectureRelationship,
  ComponentKind,
  Selection,
} from "@/features/architecture/model/types";
type PersistenceSource = "backend";
import { KIND_META } from "@/features/architecture/components/kinds";

export type SaveStatus = "saved" | "unsaved" | "saving" | "error";

interface EditorState {
  projectName: string;
  projectId: string;
  revision: number;
  doc: ArchitectureDocument;
  past: ArchitectureDocument[];
  future: ArchitectureDocument[];
  selection: Selection;
  highlight: { components: string[]; relationships: string[] };
  saveStatus: SaveStatus;
  persistenceSource: PersistenceSource;
  dismissedFindings: string[];
  acceptedFindings: string[];

  loadDoc: (doc: ArchitectureDocument, source?: PersistenceSource) => void;
  /** Apply a change and record it in undo history. */
  update: (fn: (d: ArchitectureDocument) => ArchitectureDocument) => void;
  /** Change without history (e.g. drag positions). */
  updateSilently: (fn: (d: ArchitectureDocument) => ArchitectureDocument) => void;
  undo: () => void;
  redo: () => void;
  select: (s: Selection) => void;
  setHighlight: (components: string[], relationships: string[]) => void;
  setSaveStatus: (s: SaveStatus) => void;
  setPersistenceSource: (source: PersistenceSource) => void;

  addComponent: (kind: ComponentKind, position?: { x: number; y: number }) => void;
  updateComponent: (id: string, patch: Partial<ArchitectureComponent>) => void;
  addRelationship: (sourceId: string, targetId: string) => void;
  updateRelationship: (id: string, patch: Partial<ArchitectureRelationship>) => void;
  deleteSelection: () => void;
  markFinding: (id: string, action: "accepted" | "dismissed") => void;
}

const uid = (p: string) => `${p}-${Math.random().toString(36).slice(2, 9)}`;

export const useEditor = create<EditorState>((set, get) => ({
  projectName: "Architecture", projectId: "", revision: 0,
  doc: { schemaVersion: "1.0", nodes: [], edges: [], viewport: { x: 0, y: 0, zoom: 1 }, metadata: {} },
  past: [],
  future: [],
  selection: null,
  highlight: { components: [], relationships: [] },
  saveStatus: "saved",
  persistenceSource: "backend",
  dismissedFindings: [],
  acceptedFindings: [],

  loadDoc: (doc, persistenceSource = "backend") => set({ doc, past: [], future: [], selection: null, saveStatus: "saved", persistenceSource, dismissedFindings: [], acceptedFindings: [] }),
  update: (fn) =>
    set((s) => ({ past: [...s.past.slice(-50), s.doc], future: [], doc: fn(s.doc), saveStatus: "unsaved" })),
  updateSilently: (fn) => set((s) => ({ doc: fn(s.doc), saveStatus: "unsaved" })),
  undo: () =>
    set((s) => {
      const prev = s.past[s.past.length - 1];
      if (!prev) return s;
      return { doc: prev, past: s.past.slice(0, -1), future: [s.doc, ...s.future], saveStatus: "unsaved" };
    }),
  redo: () =>
    set((s) => {
      const next = s.future[0];
      if (!next) return s;
      return { doc: next, future: s.future.slice(1), past: [...s.past, s.doc], saveStatus: "unsaved" };
    }),
  select: (selection) => set({ selection }),
  setHighlight: (components, relationships) => set({ highlight: { components, relationships } }),
  setSaveStatus: (saveStatus) => set({ saveStatus }),
  setPersistenceSource: (persistenceSource) => set({ persistenceSource }),

  addComponent: (kind, position) => {
    const id = uid(kind);
    const meta = KIND_META[kind];
    get().update((d) => ({
      ...d,
      nodes: [
        ...d.nodes,
        {
          id,
          type: kind,
          label: `New ${meta.label}`,
          description: "",
          properties: { technology: meta.defaultTech ?? "", responsibilities: [] },
          ...(position ?? { x: 760, y: 80 }),
        },
      ],
    }));
    set({ selection: { type: "component", id } });
  },
  updateComponent: (id, patch) =>
    get().update((d) => ({ ...d, nodes: d.nodes.map((c) => (c.id === id ? { ...c, ...patch } : c)) })),
  addRelationship: (sourceId, targetId) => {
    if (sourceId === targetId) return;
    get().update((d) => ({
      ...d,
      edges: [...d.edges, { id: uid("rel"), sourceId, targetId, type: "sync", label: "REST", properties: {} }],
    }));
  },
  updateRelationship: (id, patch) =>
    get().update((d) => ({ ...d, edges: d.edges.map((r) => (r.id === id ? { ...r, ...patch } : r)) })),
  deleteSelection: () => {
    const sel = get().selection;
    if (!sel) return;
    get().update((d) =>
      sel.type === "component"
        ? {
            ...d,
            nodes: d.nodes.filter((c) => c.id !== sel.id),
            edges: d.edges.filter((r) => r.sourceId !== sel.id && r.targetId !== sel.id),
          }
        : { ...d, edges: d.edges.filter((r) => r.id !== sel.id) },
    );
    set({ selection: null });
  },
  markFinding: (id, action) =>
    set((s) =>
      action === "accepted"
        ? { acceptedFindings: [...s.acceptedFindings, id] }
        : { dismissedFindings: [...s.dismissedFindings, id] },
    ),
}));
