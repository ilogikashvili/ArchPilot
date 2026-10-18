import { expect, it } from "vitest";
import { liveSocketUrl } from "./collab";

it("connects to the same-origin project WebSocket endpoint", () => {
  const url = new URL(liveSocketUrl("project/123", "access-token"));

  expect(url.protocol).toBe(
    window.location.protocol === "https:" ? "wss:" : "ws:",
  );
  expect(url.pathname).toBe("/ws/projects/project%2F123");
  expect(url.searchParams.get("token")).toBe("access-token");
});
