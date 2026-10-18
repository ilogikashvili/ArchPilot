import { memo } from "react";
import { Handle, Position, type NodeProps, type Node } from "@xyflow/react";
import type { ArchitectureComponent } from "../model/types";
import { KIND_META } from "./kinds";

export type ArchNodeData = { component: ArchitectureComponent; highlighted: boolean; dimmed: boolean };
export type ArchFlowNode = Node<ArchNodeData, "arch">;

/** Services get one of several accent tones so sibling services are easy to tell apart. */
const SERVICE_TONES = ["var(--kind-service)", "var(--kind-web)", "var(--kind-queue)", "var(--kind-external)", "var(--kind-cache)"];
function accentFor(c: ArchitectureComponent) {
  if (c.type !== "service") return (KIND_META[c.type as keyof typeof KIND_META] ?? KIND_META.service).color;
  let h = 0;
  for (const ch of c.id) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return SERVICE_TONES[h % SERVICE_TONES.length];
}

function ArchNodeImpl({ data, selected }: NodeProps<ArchFlowNode>) {
  const { component: c, highlighted, dimmed } = data;
  const meta = (KIND_META[c.type as keyof typeof KIND_META] ?? KIND_META.service);
  const Icon = meta.icon;
  const color = accentFor(c);
  const isDb = c.type === "database";

  return (
    <div
      className={`relative flex min-w-[168px] items-center gap-2.5 border px-3 py-2.5 transition-all ${isDb ? "rounded-[50%/22%] px-6 py-4" : "rounded-lg"} ${dimmed ? "opacity-40" : ""}`}
      style={{
        borderColor: color,
        background: `color-mix(in oklab, ${color} 14%, var(--card))`,
        boxShadow: selected
          ? `0 0 0 2px var(--primary), 0 0 24px -4px ${color}`
          : highlighted
            ? `0 0 0 2px var(--warning), 0 0 28px -2px var(--warning)`
            : `0 0 18px -8px ${color}`,
      }}
    >
      <Handle type="target" position={Position.Top} />
      <span
        className="grid h-7 w-7 shrink-0 place-items-center rounded-md"
        style={{ background: `color-mix(in oklab, ${color} 28%, transparent)`, color }}
      >
        <Icon className="h-4 w-4" />
      </span>
      <div className="min-w-0">
        <div className="truncate text-[13px] font-semibold text-foreground">{c.label}</div>
        {c.properties.technology && (
          <div className="truncate text-[11px]" style={{ color }}>
            {c.properties.technology}
          </div>
        )}
      </div>
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}

export const ArchNode = memo(ArchNodeImpl);
