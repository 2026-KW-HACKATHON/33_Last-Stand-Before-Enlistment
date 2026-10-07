import type { RecommendationInput, RecommendationResult } from "./model";

/** A future adapter may consume an agreed API contract without changing recommendation UI. */
export type RecommendationService = {
  get(input: RecommendationInput, signal?: AbortSignal): Promise<RecommendationResult | null>;
  refresh(input: RecommendationInput, currentPostId: string, signal?: AbortSignal): Promise<RecommendationResult | null>;
};
