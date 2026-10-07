import type { PostDisplayModel } from "../post/model";
import type { SearchResult } from "./model";
import type { SearchService } from "./service";

export type SearchMockMode = "success" | "error";

/** Fixture source stays in the existing post feature; this adapter owns no second post model. */
export function createSearchMockService(posts: readonly PostDisplayModel[], regionNames: Readonly<Record<string, string>>, mode: SearchMockMode = "success"): SearchService {
  return {
    async search(filters, signal) {
      signal?.throwIfAborted();
      if (mode === "error") throw new Error("Mock search failed.");
      const query = filters.query.trim().toLocaleLowerCase();
      return { posts: posts.filter((post) =>
        (regionNames[filters.regionId] === undefined || post.metadata.regionName === regionNames[filters.regionId]) &&
        (filters.type === "ALL" || post.type === filters.type) &&
        (filters.topic === "ALL" || post.metadata.topic === filters.topic) &&
        (!query || `${post.title} ${post.content}`.toLocaleLowerCase().includes(query)),
      ) } satisfies SearchResult;
    },
  };
}
