import { api, json } from "./client";

export type AgentRole = "EDITOR" | "REVIEWER" | "VIEWER";
export interface Agent {
  id: string; agentUserId: string; name: string; provider: string | null; role: AgentRole;
  tokenPrefix: string; createdAt: string; lastUsedAt: string | null; revoked: boolean; token: string | null;
}
const path = (projectId: string) => `/projects/${encodeURIComponent(projectId)}/agents`;

export const agents = {
  list: (projectId: string) => api<Agent[]>(path(projectId)),
  create: (projectId: string, body: { name: string; provider: string; role: AgentRole }) => api<Agent>(path(projectId), json("POST", body)),
  revoke: (projectId: string, id: string) => api<void>(`${path(projectId)}/${id}`, { method: "DELETE" }),
};
