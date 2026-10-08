import { postFixtures } from "../post/mock";
import type { PostDisplayModel } from "../post/model";
import type { BookmarkService } from "./model";

export const bookmarkMockSubject = "bookmark-demo";
export type BookmarkScenario = "normal" | "empty" | "error" | "mutation-error";
/** One original-post registry and relationship set shared by list and detail. Development only. */
export function createBookmarksMock(scenario: BookmarkScenario = "normal", delay = 0) {
  const posts = new Map<string, PostDisplayModel>(Object.values(postFixtures).map(post => [post.id, post]));
  const relations = new Map<string, Set<string>>([[bookmarkMockSubject, new Set(scenario === "empty" ? [] : ["agenda-photo", "activity", "vote", "vote-ended"])]]);
  let current = scenario;
  async function wait(signal: AbortSignal) { signal.throwIfAborted(); if (delay) await new Promise(resolve => setTimeout(resolve, delay)); signal.throwIfAborted(); }
  const snapshot = (subject: string, id: string) => ({ postId: id, available: posts.has(id), isBookmarked: posts.has(id) && !!relations.get(subject)?.has(id) });
  const service: BookmarkService = {
    source: "mock",
    async list(subject, signal) { await wait(signal); if (current === "error") throw new Error("Mock read failure"); return [...(relations.get(subject) ?? [])].flatMap(id => posts.has(id) ? [posts.get(id)!] : []); },
    async get(subject, id, signal) { await wait(signal); if (current === "error") throw new Error("Mock read failure"); return snapshot(subject, id); },
    async set(subject, id, selected, signal) {
      await wait(signal); if (current === "mutation-error") throw new Error("Mock mutation failure");
      if (!posts.has(id)) return snapshot(subject, id);
      const saved = relations.get(subject) ?? new Set<string>(); relations.set(subject, saved);
      if (selected) saved.add(id); else saved.delete(id);
      return snapshot(subject, id);
    },
  };
  return { service, post: (id: string) => posts.get(id), scenario: (value: BookmarkScenario) => { current = value; if (value === "empty") relations.get(bookmarkMockSubject)?.clear(); },
    removeOriginal: (id: string) => { posts.delete(id); relations.forEach(saved => saved.delete(id)); } };
}
