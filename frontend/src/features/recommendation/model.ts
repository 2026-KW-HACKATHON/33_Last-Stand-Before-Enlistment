import type { BoardTopic, ExploreRegion } from "../explore/model";
import type { AgendaPostDisplay } from "../post/model";

/** FE display contract only. It is not a Backend DTO or an API query format. */
export type RecommendationInput = {
  activityRegion: ExploreRegion;
  interestRegions: readonly ExploreRegion[];
  interestKeywords: readonly BoardTopic[];
};

export type RecommendationResult = {
  post: AgendaPostDisplay;
  reason: string;
  input: RecommendationInput;
};

export type RecommendationStatus = "loading" | "success" | "empty" | "error" | "refreshing";

export function recommendationSnapshotReference(result: RecommendationResult): string {
  return `recommendation:${result.post.id}:${result.input.activityRegion.id}:${result.input.interestRegions.map((region) => region.id).join(",")}:${result.input.interestKeywords.join(",")}`;
}
