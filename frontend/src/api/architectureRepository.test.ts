import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createDemoArchitecture } from "@/data/demoArchitecture";
import { loadArchitecture, saveArchitecture, resetArchitecture } from "./architectureRepository";
const document = createDemoArchitecture();
const payload = { projectId: "project-1", document, revision: 7, updatedAt: "2026-01-01T00:00:00Z" };
const response = () => new Response(JSON.stringify(payload), { status: 200, headers: { "Content-Type": "application/json" } });
describe("architectureRepository", () => {
 beforeEach(() => localStorage.clear());
 afterEach(() => vi.unstubAllGlobals());
 it("loads the exact backend response wrapper", async () => {
  const fetchMock = vi.fn().mockResolvedValue(response()); vi.stubGlobal("fetch", fetchMock);
  await expect(loadArchitecture("project-1")).resolves.toEqual(payload);
  expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/v1/projects/project-1/architecture");
 });
 it("sends the document and expectedRevision on save", async () => {
  const fetchMock = vi.fn().mockResolvedValue(response()); vi.stubGlobal("fetch", fetchMock);
  await expect(saveArchitecture("project-1", document, 6)).resolves.toEqual(payload);
  expect(JSON.parse(fetchMock.mock.calls[0]?.[1].body)).toEqual({ document, expectedRevision: 6 });
  expect(fetchMock.mock.calls[0]?.[1].method).toBe("PUT");
 });
 it("does not fall back to browser data on API failure", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("Not found", { status: 404 })));
  await expect(loadArchitecture("project-1")).rejects.toThrow("404");
  expect(localStorage.length).toBe(0);
 });
 it("reports revision conflicts", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 409 })));
  await expect(saveArchitecture("project-1", document, 1)).rejects.toThrow("Reload before saving");
 });
 it("resets through the backend", async () => {
  const fetchMock = vi.fn().mockResolvedValue(response()); vi.stubGlobal("fetch", fetchMock);
  await resetArchitecture("project-1");
  expect(fetchMock.mock.calls[0]?.[0]).toContain("/architecture/reset");
  expect(fetchMock.mock.calls[0]?.[1].body).toBe("{}");
 });
});
