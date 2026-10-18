import { useReactFlow } from "@xyflow/react";
import { BranchSwitcher } from "@/features/workspace/VersionPanel";
import { Link } from "@tanstack/react-router";
import { toast } from "sonner";
import {
  ChevronDown, Code2, GitBranch, LayoutDashboard, Map, MessageSquareText, Plus, Settings, Workflow,
} from "lucide-react";
import { useEditor } from "@/store/editorStore";
import { KIND_META, PALETTE_KINDS, RELATIONSHIP_META } from "@/features/architecture/components/kinds";
import { DRAG_MIME } from "@/features/architecture/components/ArchitectureCanvas";
import type { RelationshipKind } from "@/features/architecture/model/types";
import { SectionLabel } from "./ui";
import { BrandLogo } from "@/components/BrandLogo";

const NAV = [
  { label: "Overview", icon: LayoutDashboard },
  { label: "Architecture", icon: Workflow },
  { label: "Branches", icon: GitBranch },
  { label: "Reviews", icon: MessageSquareText },
  { label: "Roadmap", icon: Map },
  { label: "Code Generation", icon: Code2 },
  { label: "Settings", icon: Settings },
];


export function NavSidebar() {
  const name = useEditor((s) => s.projectName);
  return (
    <aside className="flex w-52 shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground">
      <div className="flex h-12 items-center gap-2 border-b border-sidebar-border px-4">
        <BrandLogo compact className="h-7 w-7" />
      </div>
      <div className="p-3">
        <Link to="/" className="flex w-full items-center justify-between rounded-md border border-sidebar-border bg-secondary/50 px-2.5 py-1.5 text-xs hover:bg-secondary">
          <span className="truncate">{name}</span> <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
        </Link>
      </div>
      <nav className="flex-1 overflow-y-auto px-2" aria-label="Primary">
        <SectionLabel className="mb-1.5 mt-1">Explorer</SectionLabel>
        <ul className="space-y-0.5">
          {NAV.map(({ label, icon: Icon }) => {
            const active = label === "Architecture";
            return (
              <li key={label}>
                <button
                  aria-current={active ? "page" : undefined}
                  onClick={() => { if(label === "Branches") window.dispatchEvent(new Event("editor:versions")); else if(label === "Settings") window.location.href="/settings"; else if(label === "Overview") window.location.href="/"; else if(!active) toast("Use the editor panels and toolbar to access this feature"); }}
                  className={`flex w-full items-center gap-2.5 rounded-md px-2 py-1.5 text-xs transition ${active ? "bg-sidebar-accent text-sidebar-accent-foreground" : "text-muted-foreground hover:bg-sidebar-accent/60 hover:text-foreground"}`}
                >
                  <Icon className="h-3.5 w-3.5" /> {label}
                </button>
              </li>
            );
          })}
        </ul>
        <SectionLabel className="mb-1.5 mt-5">Branches</SectionLabel>
        <BranchSwitcher />
        <a href="/settings" className="block p-2 text-xs">Workspace settings ↗</a>
      </nav>

    </aside>
  );
}

export function PalettePanel() {
  const addComponent = useEditor((s) => s.addComponent);
  const rf = useReactFlow();
  return (
    <aside className="flex w-44 shrink-0 flex-col overflow-y-auto border-r border-border bg-sidebar/60 p-2" aria-label="Component palette">
      <SectionLabel className="mb-2 mt-1">Components</SectionLabel>
      <ul className="space-y-0.5">
        {PALETTE_KINDS.map((k) => {
          const m = KIND_META[k];
          const Icon = m.icon;
          return (
            <li key={k}>
              <button
                draggable
                onDragStart={(e) => { e.dataTransfer.setData(DRAG_MIME, k); e.dataTransfer.effectAllowed = "move"; }}
                onClick={() => { const rect = document.querySelector(".react-flow")?.getBoundingClientRect(); if(rect) addComponent(k, rf.screenToFlowPosition({x:rect.x+rect.width/2-80,y:rect.y+rect.height/2-24})); }}
                title={`${m.description} — drag onto canvas or click to add`}
                className="group flex w-full cursor-grab items-center gap-2.5 rounded-md px-2 py-1.5 text-xs text-foreground/90 hover:bg-accent active:cursor-grabbing"
              >
                <span className="grid h-6 w-6 place-items-center rounded border" style={{ color: m.color, borderColor: `color-mix(in oklab, ${m.color} 50%, transparent)`, background: `color-mix(in oklab, ${m.color} 14%, transparent)` }}>
                  <Icon className="h-3.5 w-3.5" />
                </span>
                <span className="flex-1 text-left">{m.label}</span>
                <Plus className="h-3 w-3 opacity-0 group-hover:opacity-60" />
              </button>
            </li>
          );
        })}
      </ul>
      <SectionLabel className="mb-2 mt-5">Relationships</SectionLabel>
      <ul className="space-y-1.5 px-2">
        {(Object.keys(RELATIONSHIP_META) as RelationshipKind[]).map((k) => {
          const m = RELATIONSHIP_META[k];
          return (
            <li key={k} className="flex items-center gap-2.5 text-xs text-muted-foreground">
              <svg width="22" height="8" aria-hidden><line x1="0" y1="4" x2="22" y2="4" stroke={m.color} strokeWidth="1.5" strokeDasharray={m.dash} /></svg>
              {m.label}
            </li>
          );
        })}
      </ul>
      <p className="mt-4 px-2 text-[11px] leading-relaxed text-muted-foreground">Drag a handle from one component to another to connect them.</p>
    </aside>
  );
}
