import { ViewportPortal, useReactFlow, useViewport } from "@xyflow/react";
import { useSimulation } from "./simulationStore";

const COLORS = { request: "#38bdf8", response: "#22c55e", async: "#f59e0b" } as const;

/** The animated "packet" travelling between two components (drawn in diagram coordinates). */
export function PacketLayer() {
  const result = useSimulation((s) => s.result);
  const current = useSimulation((s) => s.current);
  const progress = useSimulation((s) => s.progress);
  const rf = useReactFlow();
  const { zoom } = useViewport();
  const event = result?.events[current];
  if (!event) return null;

  const center = (id: string) => {
    const n = rf.getInternalNode(id);
    if (!n) return null;
    const w = n.measured?.width ?? 180, h = n.measured?.height ?? 60;
    return { x: n.internals.positionAbsolute.x + w / 2, y: n.internals.positionAbsolute.y + h / 2 };
  };
  const a = center(event.fromId), b = center(event.toId);
  if (!a || !b) return null;
  const t = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;
  const x = a.x + (b.x - a.x) * t, y = a.y + (b.y - a.y) * t;
  const color = COLORS[event.kind];

  return (
    <ViewportPortal>
      <svg className="pointer-events-none absolute left-0 top-0 overflow-visible" style={{ zIndex: 999 }}>
        <line x1={a.x} y1={a.y} x2={b.x} y2={b.y} stroke={color} strokeWidth={2} strokeDasharray="4 6" opacity={0.5} />
      </svg>
      <div className="pointer-events-none absolute left-0 top-0" style={{ transform: `translate(${x}px, ${y}px) scale(${Math.max(1, 0.8 / zoom)})`, transformOrigin: "0 0", zIndex: 1000 }}>
        <div className="-translate-x-1/2 -translate-y-1/2">
          <div className="grid h-7 w-9 place-items-center rounded-md border-2 border-white/80 text-[9px] font-bold text-black shadow-lg"
            style={{ background: color, boxShadow: `0 0 18px ${color}` }}>
            {event.kind === "response" ? "RES" : event.kind === "async" ? "MSG" : "REQ"}
          </div>
          <div className="mt-1 -translate-x-1/4 whitespace-nowrap rounded bg-black/80 px-1.5 py-0.5 text-[10px] text-white">{event.protocol}</div>
        </div>
      </div>
    </ViewportPortal>
  );
}
