import { describe, expect, it } from "vitest";
import { mergeMessage, type ChatMessage } from "./TeamChatPanel";
const m = (id: string, t: string): ChatMessage => ({ id, authorType: "USER", authorName: "a", content: id, createdAt: t });
describe("team chat messages", () => {
  it("ignores a message that arrives twice (REST reply + live push)", () => {
    expect(mergeMessage([m("1", "2026-01-01T00:00:01Z")], m("1", "2026-01-01T00:00:01Z"))).toHaveLength(1);
  });
  it("keeps messages in time order", () => {
    const out = mergeMessage([m("2", "2026-01-01T00:00:02Z")], m("1", "2026-01-01T00:00:01Z"));
    expect(out.map((x) => x.id)).toEqual(["1", "2"]);
  });
});
