import { api, request } from "./client";

export type CodegenLayout = "multi-module" | "per-service";
export interface CodegenFile { path: string; content: string }

const qs = (layout: CodegenLayout, groupId: string) =>
  `language=java&layout=${layout}&groupId=${encodeURIComponent(groupId)}`;

export const previewStructure = (projectId: string, layout: CodegenLayout, groupId: string) =>
  api<{ files: CodegenFile[] }>(`/projects/${encodeURIComponent(projectId)}/codegen/preview?${qs(layout, groupId)}`);

export async function downloadStructure(projectId: string, layout: CodegenLayout, groupId: string) {
  const res = await request(`/projects/${encodeURIComponent(projectId)}/codegen?${qs(layout, groupId)}`);
  const name = /filename="([^"]+)"/.exec(res.headers.get("content-disposition") ?? "")?.[1] ?? `project-${layout}.zip`;
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement("a");
  a.href = url; a.download = name;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 30_000);
}
