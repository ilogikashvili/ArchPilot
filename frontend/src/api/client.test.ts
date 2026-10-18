import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { api } from "./client";
beforeEach(() => { localStorage.clear(); localStorage.setItem("accessToken", "old"); localStorage.setItem("refreshToken", "refresh"); });
afterEach(() => vi.unstubAllGlobals());
it("refreshes once and retries with the new access token", async () => {
 const fetchMock = vi.fn().mockResolvedValueOnce(new Response("", { status: 401 })).mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: "new", refreshToken: "new-refresh" }))).mockResolvedValueOnce(new Response(JSON.stringify({ ok: true })));
 vi.stubGlobal("fetch", fetchMock);
 await expect(api("/projects")).resolves.toEqual({ ok: true });
 expect(fetchMock).toHaveBeenCalledTimes(3);
 expect(fetchMock.mock.calls[1]?.[0]).toBe("/api/v1/auth/refresh");
 expect(fetchMock.mock.calls[2]?.[1].headers.Authorization).toBe("Bearer new");
 expect(localStorage.getItem("refreshToken")).toBe("new-refresh");
});
it("clears the session when refresh fails", async () => {
 vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 401 })));
 await expect(api("/projects")).rejects.toThrow("session has expired");
 expect(localStorage.getItem("accessToken")).toBeNull();
});
