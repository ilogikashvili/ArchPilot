import type { Operation } from "@/features/collab/ops";
import type { ArchitectureDocument } from "@/features/architecture/model/types";
import { api, json } from "./client";

export interface DesignPattern { id: string; name: string; category: string; description: string; whenToUse: string }
export interface PatternTransformation {
  patternId: string; applicable: boolean; summary: string; changes: string[];
  operations: Operation[]; highlightNodeIds: string[]; highlightEdgeIds: string[];
}

export const patterns = {
  list: () => api<DesignPattern[]>("/design-patterns"),
  preview: (projectId: string, patternId: string, document: ArchitectureDocument) =>
    api<PatternTransformation>(`/projects/${encodeURIComponent(projectId)}/architecture/patterns/${encodeURIComponent(patternId)}/preview`, json("POST", { document })),
};
