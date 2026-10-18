import type { ArchitectureDocument } from "@/features/architecture/model/types";
import { api, json, request } from "./client";
export interface ArchitectureResponse { projectId: string; document: ArchitectureDocument; revision: number; updatedAt: string }
const path = (id: string) => `/projects/${encodeURIComponent(id)}/architecture`;
export const loadArchitecture = (id: string) => api<ArchitectureResponse>(path(id));
export const saveArchitecture = (id: string, document: ArchitectureDocument, expectedRevision: number) => api<ArchitectureResponse>(path(id), json("PUT", { document, expectedRevision }));
export const resetArchitecture = (id: string, templateId?: string) => api<ArchitectureResponse>(`${path(id)}/reset`, json("POST", templateId ? { templateId } : {}));
export async function exportArchitecture(id: string) {
  const response = await request(`${path(id)}/export?format=json`);
  const url = URL.createObjectURL(await response.blob());
  const a = document.createElement("a"); a.href = url; a.download = `architecture-${id}.json`; a.click(); URL.revokeObjectURL(url);
}
