import type { PostDisplayModel } from "../post/model";

/** Registered server IDs; optional geographic center uses [latitude, longitude]. */
export type MapViewport = { centerRegionId: string; zoom: number; center?: readonly [number, number]; regionIds?: readonly string[] };
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
  const center = snapshot.viewport.center?.map(value => value.toFixed(6)).join(":") ?? "region";
  return `map:${snapshot.viewport.centerRegionId}:${snapshot.viewport.zoom}:${center}:${snapshot.selectedDongId ?? "none"}`;
}
