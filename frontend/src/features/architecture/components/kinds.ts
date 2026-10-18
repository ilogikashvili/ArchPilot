import { Box, Database, Globe, Layers, Monitor, Network, Zap, type LucideIcon } from "lucide-react";
import type { ComponentKind, RelationshipKind } from "../model/types";

/** Visual metadata per component kind. Colors map to --kind-* tokens. */
export const KIND_META: Record<
  ComponentKind,
  { label: string; icon: LucideIcon; color: string; description: string; defaultTech?: string | undefined }
> = {
  service: { label: "Service", icon: Box, color: "var(--kind-service)", description: "Backend service with business logic", defaultTech: "Spring Boot" },
  api: { label: "API", icon: Network, color: "var(--kind-api)", description: "Gateway or public API surface", defaultTech: "REST" },
  database: { label: "Database", icon: Database, color: "var(--kind-database)", description: "Persistent data store", defaultTech: "PostgreSQL" },
  queue: { label: "Queue", icon: Layers, color: "var(--kind-queue)", description: "Message broker or event stream", defaultTech: "Kafka" },
  cache: { label: "Cache", icon: Zap, color: "var(--kind-cache)", description: "In-memory cache", defaultTech: "Redis" },
  external: { label: "External API", icon: Globe, color: "var(--kind-external)", description: "Third-party system" },
  web: { label: "Web App", icon: Monitor, color: "var(--kind-web)", description: "Client application", defaultTech: "React" },
};

export const PALETTE_KINDS: ComponentKind[] = ["service", "database", "api", "queue", "cache", "external", "web"];

export const RELATIONSHIP_META: Record<RelationshipKind, { label: string; dash?: string; animated?: boolean; color: string }> = {
  sync: { label: "Sync Call", color: "var(--muted-foreground)" },
  async: { label: "Async Call", dash: "6 4", color: "var(--kind-queue)" },
  event: { label: "Event", dash: "2 4", animated: true, color: "var(--kind-api)" },
  dependency: { label: "Dependency", dash: "8 3 2 3", color: "var(--kind-database)" },
};
