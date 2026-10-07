import type { PostDisplayModel } from "../post/model";

/** FE display contracts only. Region geometry and provider coordinates remain unagreed. */
export type MapViewport = { centerRegionId: string; zoom: number };
export type MapDong = {
  region: { id: string; name: string };
  representativePost: PostDisplayModel | null;
};
export type MapResult = { centerRegion: { id: string; name: string }; dongs: readonly MapDong[] };
export type MapLoadState =
  | { kind: "loading" }
  | { kind: "success"; data: MapResult }
  | { kind: "error"; message: string };

export type MapSnapshot = { viewport: MapViewport; selectedDongId: string | null };

export function mapSnapshotReference(snapshot: MapSnapshot): string {
  return `map:${snapshot.viewport.centerRegionId}:${snapshot.viewport.zoom}:${snapshot.selectedDongId ?? "none"}`;
}
