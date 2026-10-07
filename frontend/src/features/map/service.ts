import type { MapResult, MapViewport } from "./model";

/** Real adapters map a later-agreed API response into this FE contract. */
export type MapService = {
  getMap(viewport: MapViewport, signal?: AbortSignal): Promise<MapResult>;
};
