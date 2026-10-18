import { api, json } from "./client";
export interface Review { id: string; projectId: string; requestedBy: string; status: "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED"; startedAt: string | null; completedAt: string | null; summary: string; findingCount: number; createdAt: string }
export interface Finding { id: string; reviewId: string; projectId: string; severity: string; category: string; title: string; description: string; recommendation: string; affectedElementId: string | null; createdAt: string }
export type DecisionValue = "ACCEPTED" | "DISMISSED" | "POSTPONED";
export interface Decision { id: string; projectId: string; findingId: string; decidedBy: string; decision: DecisionValue; reason: string | null; decidedAt: string; createdAt: string }
const base = (id: string) => `/projects/${encodeURIComponent(id)}`;
export const reviews = {
 start: (pid: string, aiKeyId?: string) => api<Review>(`${base(pid)}/reviews`, json("POST", { engineType: "SENIOR", aiKeyId: aiKeyId || null })),
 get: (pid: string, rid: string) => api<Review>(`${base(pid)}/reviews/${rid}`),
 findings: (pid: string, rid: string) => api<Finding[]>(`${base(pid)}/reviews/${rid}/findings`),
 decisions: (pid: string) => api<Decision[]>(`${base(pid)}/findings/decisions`),
 decide: (pid: string, fid: string, decision: DecisionValue) => api<Decision>(`${base(pid)}/findings/${fid}/decision`, json("PUT", { decision, reason: null })),
};
