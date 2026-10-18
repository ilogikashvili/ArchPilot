import type { Operation } from "@/features/collab/ops";
import type { ArchitectureResponse } from "./architectureRepository";
import { api, json } from "./client";

const configuredWebSocketBase = import.meta.env["VITE_WS_BASE_URL"]?.trim();

export const applyOperations = (
  projectId: string,
  clientId: string,
  operations: Operation[],
) =>
  api<ArchitectureResponse>(
    `/projects/${encodeURIComponent(projectId)}/architecture/operations`,
    json("POST", { clientId, operations }),
  );

/** Builds a same-origin ws(s) URL by default, or uses VITE_WS_BASE_URL when configured. */
export function liveSocketUrl(projectId: string, token: string) {
  const base = new URL(configuredWebSocketBase || "/ws", window.location.href);
  base.protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  base.pathname = `${base.pathname.replace(/\/+$/, "")}/projects/${encodeURIComponent(projectId)}`;
  base.search = "";
  base.hash = "";
  base.searchParams.set("token", token);
  return base.toString();
}
