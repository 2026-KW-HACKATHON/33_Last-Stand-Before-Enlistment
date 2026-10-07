import type { BoardFilters } from "../explore/model";
import type { PostDisplayModel } from "../post/model";

/** FE-only search contract. It is not a Backend DTO or a URL query format. */
export type SearchFilters = BoardFilters & { query: string };
export type SearchResult = { posts: readonly PostDisplayModel[] };
export type SearchStatus = "idle" | "loading" | "success" | "empty" | "error";
export type SearchSnapshot = { filters: SearchFilters; result: SearchResult | null; scrollTop: number };

export function emptySearchFilters(regionId: string): SearchFilters {
  return { query: "", regionId, type: "ALL", topic: "ALL" };
}

export function searchSnapshotReference(snapshot: SearchSnapshot): string {
  return `search:${snapshot.filters.regionId}:${snapshot.filters.type}:${snapshot.filters.topic}:${snapshot.filters.query}:${snapshot.scrollTop}`;
}
