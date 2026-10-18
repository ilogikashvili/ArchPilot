import { api, json } from "./client";
export interface Project { id: string; name: string; description: string; ownerId: string; archived: boolean; createdAt: string; updatedAt: string }
const path = (id: string) => `/projects/${encodeURIComponent(id)}`;
export const projects = {
 list: () => api<Project[]>("/projects"),
 get: (id: string) => api<Project>(path(id)),
 create: (name: string) => api<Project>("/projects", json("POST", { name, description: "" })),
 rename: (id: string, name: string) => api<Project>(path(id), json("PATCH", { name })),
 remove: (id: string) => api<void>(path(id), { method: "DELETE" }),
 archive: (id: string, archived: boolean) => api<Project>(`${path(id)}/${archived ? "archive" : "restore"}`, json("POST")),
};
