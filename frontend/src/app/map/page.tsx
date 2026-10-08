"use client";

import { MapScreen } from "@/features/map/MapScreen";
import type { ExploreContext } from "@/features/explore/model";

const unavailableContext: ExploreContext = {
  defaultActivityRegion: { id: "pending", name: "활동 지역", neighborVerified: false },
  currentExploreRegion: { id: "pending", name: "탐색 지역", neighborVerified: false },
  exploreRegionCandidates: [], neighborVerifiedRegions: [], institutionRegions: [],
};

/** Production wiring waits for the agreed service/provider adapter. */
export default function MapPage() { return <MapScreen context={unavailableContext} />; }
