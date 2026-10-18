const configuredApiBase = import.meta.env["VITE_API_BASE_URL"]?.trim();
export const API_BASE = configuredApiBase
  ? configuredApiBase.replace(/\/+$/, "")
  : "/api/v1";
const OFFLINE =
  "Can't reach the ArchMain server. Check that the backend is running and that this site is configured to reach it.";
export interface Tokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}
export function storeTokens(tokens: Tokens) {
  localStorage.setItem("accessToken", tokens.accessToken);
  localStorage.setItem("refreshToken", tokens.refreshToken);
}
export function clearTokens() {
  if (typeof window !== "undefined") {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.dispatchEvent(new Event("auth:logout"));
  }
}
const token = (key: string) =>
  typeof window === "undefined" ? null : localStorage.getItem(key);
let refreshing: Promise<void> | null = null;
async function fetchOrExplain(
  url: string,
  init: RequestInit,
): Promise<Response> {
  try {
    return await fetch(url, init);
  } catch {
    throw new Error(OFFLINE);
  }
}
/** Turns any server error into one readable sentence (field errors, proxy 503, HTML pages from the wrong server). */
export async function errorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  let body: Record<string, unknown> | null = null;
  try {
    const parsed: unknown = text ? JSON.parse(text) : null;
    if (isRecord(parsed)) body = parsed;
  } catch {
    body = null;
  }
  if (!body && (text.trimStart().startsWith("<") || response.status >= 502))
    return OFFLINE;
  const fieldErrors = body?.["fieldErrors"];
  const fields = isRecord(fieldErrors)
    ? Object.values(fieldErrors)
        .filter((value): value is string => typeof value === "string")
        .join(". ")
    : "";
  if (fields) return fields;
  const title = body?.["title"];
  if (response.status === 503 && typeof title === "string") return title;
  const message = [body?.["message"], body?.["detail"], title].find(
    (value): value is string => typeof value === "string" && value.length > 0,
  );
  return (
    message ??
    `Request failed (${response.status})${response.status === 409 ? ": Architecture changed on the server. Reload before saving." : ""}`
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
export async function request(
  path: string,
  init: RequestInit = {},
  authenticated = true,
): Promise<Response> {
  const send = () =>
    fetchOrExplain(`${API_BASE}${path}`, {
      ...init,
      headers: {
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...init.headers,
        ...(authenticated && token("accessToken")
          ? { Authorization: `Bearer ${token("accessToken")}` }
          : {}),
      },
    });
  let response = await send();
  if (response.status === 401 && authenticated) {
    try {
      if (!refreshing)
        refreshing = (async () => {
          if (!token("refreshToken")) throw new Error("Please log in again.");
          const res = await fetch(`${API_BASE}/auth/refresh`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ refreshToken: token("refreshToken") }),
          });
          if (!res.ok)
            throw new Error("Your session has expired. Please log in again.");
          storeTokens(await res.json());
        })().finally(() => {
          refreshing = null;
        });
      await refreshing;
      response = await send();
      if (response.status === 401) throw new Error("Please log in again.");
    } catch (error) {
      clearTokens();
      throw error;
    }
  }
  if (!response.ok) throw new Error(await errorMessage(response));
  return response;
}
export async function api<T>(
  path: string,
  init?: RequestInit,
  authenticated = true,
): Promise<T> {
  const res = await request(path, init, authenticated);
  if (res.status === 204 || res.status === 202) return undefined as T;
  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new Error(OFFLINE);
  }
}
export const json = (method: string, body: unknown = {}) => ({
  method,
  body: JSON.stringify(body),
});
