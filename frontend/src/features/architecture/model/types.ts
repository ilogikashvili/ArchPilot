export type ComponentKind = "service" | "api" | "database" | "queue" | "cache" | "external" | "web";
export type RelationshipKind = "sync" | "async" | "event" | "dependency";
export interface ArchitectureComponent {
  id: string; type: string; label: string; description: string; x: number; y: number;
  properties: Record<string, unknown> & { technology?: string; responsibilities?: string[] };
}
export interface ArchitectureRelationship {
  id: string; type: string; sourceId: string; targetId: string; label?: string;
  properties: Record<string, unknown>;
}
export interface ArchitectureDocument {
  schemaVersion: string; nodes: ArchitectureComponent[]; edges: ArchitectureRelationship[];
  viewport: { x: number; y: number; zoom: number }; metadata: Record<string, unknown>;
}
export type Selection = { type: "component" | "relationship"; id: string } | null;
