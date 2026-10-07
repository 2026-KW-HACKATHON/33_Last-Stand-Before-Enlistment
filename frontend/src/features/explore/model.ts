import type { PostDisplayModel, PostType } from "../post/model";

/** FE display contracts only. These intentionally do not define server endpoints or DTOs. */
export const boardTopics = ["교통", "주거", "안전", "복지", "생활정보", "환경", "기타"] as const;
export type BoardTopic = (typeof boardTopics)[number];
export type BoardTypeFilter = "ALL" | PostType;
export type BoardTopicFilter = "ALL" | BoardTopic;

export type ExploreRegion = { id: string; name: string; neighborVerified: boolean };
export type ExploreContext = {
  defaultActivityRegion: ExploreRegion;
  currentExploreRegion: ExploreRegion;
  /** Candidate display data only; this does not alter the profile's default region. */
  exploreRegionCandidates: readonly ExploreRegion[];
  neighborVerifiedRegions: readonly string[];
  institutionRegions: readonly string[];
};
export type BoardFilters = { regionId: string; type: BoardTypeFilter; topic: BoardTopicFilter };
export type PageInfo = { nextCursor?: string };
export type BoardPage = { posts: readonly PostDisplayModel[]; pageInfo: PageInfo };
export type HomeContent = { regionPosts: readonly PostDisplayModel[]; openVotes: readonly PostDisplayModel[] };

export type LoadState<T> =
  | { kind: "loading" }
  | { kind: "success"; data: T }
  | { kind: "empty" }
  | { kind: "error"; message: string };

export function toBoardQuery(filters: BoardFilters) {
  return {
    regionId: filters.regionId,
    ...(filters.type === "ALL" ? {} : { type: filters.type }),
    ...(filters.topic === "ALL" ? {} : { topic: filters.topic }),
  };
}

export function canParticipateInExploreRegion(context: ExploreContext) {
  return context.neighborVerifiedRegions.includes(context.currentExploreRegion.id);
}

/** B02/B04 keeps the applied explore region until a candidate is explicitly confirmed. */
export function applyExploreRegion(context: ExploreContext, candidate: ExploreRegion): ExploreContext {
  return { ...context, currentExploreRegion: candidate };
}
