import { postFixtures } from "../post/mock";
import type { BoardFilters, BoardPage, ExploreContext, HomeContent } from "./model";
import type { ExploreService } from "./service";

const posts = Object.values(postFixtures);

export const explorePreviewContext: ExploreContext = {
  defaultActivityRegion: { id: "gongneung-2", name: "공릉2동", neighborVerified: true },
  currentExploreRegion: { id: "gongneung-2", name: "공릉2동", neighborVerified: true },
  exploreRegionCandidates: [
    { id: "gongneung-2", name: "공릉2동", neighborVerified: true },
    { id: "hagye-2", name: "하계2동", neighborVerified: false },
  ],
  neighborVerifiedRegions: ["gongneung-2"],
  institutionRegions: [],
};

export function createExploreMockService(): ExploreService {
  return {
    async getHome() {
      return { regionPosts: posts.slice(0, 3), openVotes: posts.filter((post) => post.type === "VOTE" && post.vote.status === "OPEN") } satisfies HomeContent;
    },
    async getBoard(filters: BoardFilters, cursor?: string) {
      const filtered = posts.filter((post) =>
        (filters.type === "ALL" || post.type === filters.type) &&
        (filters.topic === "ALL" || post.metadata.topic === filters.topic),
      );
      const start = cursor ? Number(cursor) : 0;
      return { posts: filtered.slice(start, start + 2), pageInfo: start + 2 < filtered.length ? { nextCursor: String(start + 2) } : {} } satisfies BoardPage;
    },
  };
}
