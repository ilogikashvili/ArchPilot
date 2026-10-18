import { ViewportPortal, useViewport } from "@xyflow/react";
import { colorFor, useCollab } from "./liveSync";

/** Other collaborators' cursors, drawn in diagram coordinates so they stay put while panning/zooming. */
export function RemoteCursors() {
  const cursors = useCollab((s) => s.cursors);
  const { zoom } = useViewport();
  return (
    <ViewportPortal>
      {Object.entries(cursors).map(([id, c]) => {
        const color = colorFor(id);
        return (
          <div key={id} className="pointer-events-none absolute left-0 top-0 z-[1000] transition-transform duration-75"
            style={{ transform: `translate(${c.x}px, ${c.y}px) scale(${1 / zoom})`, transformOrigin: "0 0" }}>
            <svg width="16" height="16" viewBox="0 0 16 16" style={{ color }}>
              <path d="M1 1l5 14 2-6 6-2z" fill="currentColor" stroke="white" strokeWidth="1" />
            </svg>
            <span className="ml-3 whitespace-nowrap rounded px-1.5 py-0.5 text-[10px] font-medium text-white" style={{ background: color }}>
              {c.agent ? "🤖 " : ""}{c.name}
            </span>
          </div>
        );
      })}
    </ViewportPortal>
  );
}
