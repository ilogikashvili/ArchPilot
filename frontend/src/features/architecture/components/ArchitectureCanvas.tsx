import { useCallback, useMemo, type DragEvent } from "react";
import {
  Background,
  BackgroundVariant,
  Controls,
  MarkerType,
  MiniMap,
  ReactFlow,
  useReactFlow,
  type Connection,
  type Edge,
  type NodeChange,
  type EdgeChange,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { useEditor } from "@/store/editorStore";
import type { ComponentKind } from "../model/types";
import { ArchNode, type ArchFlowNode } from "./ArchNode";
import { RELATIONSHIP_META } from "./kinds";
import { RemoteCursors } from "@/features/collab/RemoteCursors";
import { liveSession, useCollab, colorFor } from "@/features/collab/liveSync";
import { PacketLayer } from "@/features/simulation/PacketLayer";

const nodeTypes = { arch: ArchNode };
export const DRAG_MIME = "application/archpilot-kind";

export function ArchitectureCanvas() {
  const doc = useEditor((s) => s.doc);
  const selection = useEditor((s) => s.selection);
  const highlight = useEditor((s) => s.highlight);
  const { select, updateSilently, update, addRelationship, addComponent } = useEditor.getState();
  const rf = useReactFlow();
  const remoteSel = useCollab((s) => s.selections);
  const collabUsers = useCollab((s) => s.users);
  const selectedBy = useMemo(() => {
    const m: Record<string, { color: string; name: string }> = {};
    for (const [conn, id] of Object.entries(remoteSel)) if (id) m[id] = { color: colorFor(conn) ?? "#3b82f6", name: collabUsers.find((u) => u.connectionId === conn)?.name ?? "Someone" };
    return m;
  }, [remoteSel, collabUsers]);
  const hasHighlight = highlight.components.length + highlight.relationships.length > 0;

  const nodes: ArchFlowNode[] = useMemo(
    () =>
      doc.nodes.map((c) => ({
        id: c.id,
        type: "arch",
        position: { x: c.x, y: c.y },
        selected: selection?.type === "component" && selection.id === c.id,
        style: selectedBy[c.id] ? { boxShadow: `0 0 0 2px ${selectedBy[c.id]!.color}`, borderRadius: 10 } : {},
        data: {
          component: c,
          highlighted: highlight.components.includes(c.id),
          dimmed: hasHighlight && !highlight.components.includes(c.id),
        },
      })),
    [doc.nodes, selection, highlight, hasHighlight, selectedBy],
  );

  const edges: Edge[] = useMemo(
    () =>
      doc.edges.map((r) => {
        const meta = (RELATIONSHIP_META[r.type as keyof typeof RELATIONSHIP_META] ?? RELATIONSHIP_META.sync);
        const hl = highlight.relationships.includes(r.id);
        const color = hl ? "var(--warning)" : meta.color;
        return {
          id: r.id,
          source: r.sourceId,
          target: r.targetId,
          label: r.label ?? "",
          type: "smoothstep",
          animated: !!meta.animated,
          selected: selection?.type === "relationship" && selection.id === r.id,
          style: { stroke: color, strokeWidth: hl ? 2.5 : 1.5, strokeDasharray: meta.dash, opacity: hasHighlight && !hl ? 0.35 : 1 },
          markerEnd: { type: MarkerType.ArrowClosed, color, width: 16, height: 16 },
          labelBgPadding: [6, 3] as [number, number],
          labelBgBorderRadius: 4,
          labelStyle: { fontSize: 10, fill: "var(--muted-foreground)" },
          labelBgStyle: { fill: "var(--card)" },
        };
      }),
    [doc.edges, selection, highlight, hasHighlight],
  );

  const onNodesChange = useCallback(
    (changes: NodeChange<ArchFlowNode>[]) => {
      for (const ch of changes) {
        if (ch.type === "position" && ch.position) {
          const pos = ch.position;
          liveSession.sendNodeMove(ch.id, Math.round(pos.x), Math.round(pos.y));
          updateSilently((d) => ({ ...d, nodes: d.nodes.map((c) => (c.id === ch.id ? { ...c, x: pos.x, y: pos.y } : c)) }));
        }
      }
    },
    [updateSilently],
  );

  const onEdgesChange = useCallback((_: EdgeChange[]) => {}, []);
  const onConnect = useCallback((c: Connection) => c.source && c.target && addRelationship(c.source, c.target), [addRelationship]);

  const onDrop = useCallback(
    (e: DragEvent) => {
      e.preventDefault();
      const kind = e.dataTransfer.getData(DRAG_MIME) as ComponentKind;
      if (!kind) return;
      addComponent(kind, rf.screenToFlowPosition({ x: e.clientX - 80, y: e.clientY - 24 }));
    },
    [rf, addComponent],
  );

  return (
    <div className="h-full w-full" onMouseMove={(e) => { const p = rf.screenToFlowPosition({ x: e.clientX, y: e.clientY }); liveSession.sendCursor(Math.round(p.x), Math.round(p.y)); }} onDragOver={(e) => { e.preventDefault(); e.dataTransfer.dropEffect = "move"; }} onDrop={onDrop}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeDragStart={() => update((d) => d)}
        onNodeClick={(_, n) => { select({ type: "component", id: n.id }); liveSession.sendSelection(n.id); }}
        onEdgeClick={(_, e) => select({ type: "relationship", id: e.id })}
        onPaneClick={() => { select(null); liveSession.sendSelection(null); }}
        deleteKeyCode={null}
        defaultViewport={doc.viewport}
        onMoveEnd={(_, viewport) => updateSilently((d) => ({ ...d, viewport }))}
        fitViewOptions={{ padding: 0.2 }}
        minZoom={0.2}
        maxZoom={2}
        proOptions={{ hideAttribution: true }}
        colorMode="dark"
      >
        <Background variant={BackgroundVariant.Dots} gap={20} size={1.2} color="var(--canvas-dot)" />
        <Controls showInteractive={false} position="bottom-left" />
        <RemoteCursors />
        <PacketLayer />
        <MiniMap pannable zoomable position="bottom-right" nodeColor="var(--muted-foreground)" maskColor="color-mix(in oklab, var(--background) 70%, transparent)" />
      </ReactFlow>
    </div>
  );
}
