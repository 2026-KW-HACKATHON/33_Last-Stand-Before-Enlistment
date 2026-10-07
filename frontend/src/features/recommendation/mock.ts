import type { AgendaPostDisplay, PostDisplayModel } from "../post/model";
import type { RecommendationInput, RecommendationResult } from "./model";
import type { RecommendationService } from "./service";

export type RecommendationMockMode = "success" | "empty" | "error" | "no-alternative";

function agendas(posts: readonly PostDisplayModel[]): readonly AgendaPostDisplay[] {
  return posts.filter((post): post is AgendaPostDisplay => post.type === "LOCAL_AGENDA");
}

/**
 * This fixture follows the product's ordered inputs only. It deliberately has no
 * behavior-history, view, click, reaction, or permanent exclusion signal.
 */
function candidates(posts: readonly AgendaPostDisplay[], input: RecommendationInput): readonly AgendaPostDisplay[] {
  const activityMatches = posts.filter((post) => post.metadata.regionName === input.activityRegion.name);
  const interestRegionNames = new Set(input.interestRegions.map((region) => region.name));
  const interestRegionMatches = posts.filter((post) => interestRegionNames.has(post.metadata.regionName));
  const keywordMatches = posts.filter((post) => input.interestKeywords.includes(post.metadata.topic as RecommendationInput["interestKeywords"][number]));
  return [...activityMatches, ...interestRegionMatches, ...keywordMatches].filter((post, index, list) => list.findIndex((candidate) => candidate.id === post.id) === index);
}

function asResult(post: AgendaPostDisplay, input: RecommendationInput): RecommendationResult {
  return {
    post,
    input,
    reason: `Activity region, interest regions, and interest keywords were considered in that order.`,
  };
}

/** Mock adapter only; it owns no duplicate post card or backend DTO. */
export function createRecommendationMockService(posts: readonly PostDisplayModel[], mode: RecommendationMockMode = "success"): RecommendationService {
  const source = agendas(posts);
  function available(input: RecommendationInput, exceptPostId?: string) {
    return candidates(source, input).filter((post) => post.id !== exceptPostId);
  }
  return {
    async get(input, signal) {
      signal?.throwIfAborted();
      if (mode === "error") throw new Error("Mock recommendation failed.");
      if (mode === "empty") return null;
      return available(input)[0] ? asResult(available(input)[0], input) : null;
    },
    async refresh(input, currentPostId, signal) {
      signal?.throwIfAborted();
      if (mode === "error") throw new Error("Mock recommendation refresh failed.");
      if (mode === "empty" || mode === "no-alternative") return null;
      const next = available(input, currentPostId)[0];
      return next ? asResult(next, input) : null;
    },
  };
}
