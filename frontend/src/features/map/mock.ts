import type { PostDisplayModel } from "../post/model";
import type { MapDong, MapResult, MapViewport } from "./model";
import type { MapService } from "./service";

export type MapMockMode = "success" | "empty" | "error";
type DongSource = { region: { id: string; name: string }; candidates: readonly PostDisplayModel[] };

function createdAtValue(post: PostDisplayModel) {
  return Number(post.metadata.createdAtLabel.replaceAll(/[^0-9]/g, ""));
}

/** Same map ranking policy as the API contract: agenda/vote, reactions, then newest. */
export function selectRepresentative(posts: readonly PostDisplayModel[]): PostDisplayModel | null {
  return posts
    .filter((post) => post.type === "LOCAL_AGENDA" || post.type === "VOTE")
    .toSorted((left, right) => right.reactionCount - left.reactionCount || createdAtValue(right) - createdAtValue(left))[0] ?? null;
}

/** Fixture adapter only; it does not create endpoint, coordinate, or SDK contracts. */
export function createMapMockService(posts: readonly PostDisplayModel[], mode: MapMockMode = "success"): MapService {
  const sources: readonly DongSource[] = [
    { region: { id: "gongneung-2", name: "공릉2동" }, candidates: posts.filter((post) => post.id === "agenda-anonymous" || post.id === "activity") },
    { region: { id: "hagye-2", name: "하계2동" }, candidates: posts.filter((post) => post.id === "agenda-photo" || post.id === "vote") },
    { region: { id: "hagye-1", name: "하계1동" }, candidates: posts.filter((post) => post.id === "activity-ended") },
  ];
  return {
    async getMap(viewport: MapViewport, signal) {
      signal?.throwIfAborted();
      if (mode === "error") throw new Error("Mock map request failed.");
      const dongs: readonly MapDong[] = sources.map((source) => ({ ...source, representativePost: mode === "empty" ? null : selectRepresentative(source.candidates) }));
      const center = sources.find((source) => source.region.id === viewport.centerRegionId)?.region ?? sources[0].region;
      return { centerRegion: center, dongs } satisfies MapResult;
    },
  };
}
