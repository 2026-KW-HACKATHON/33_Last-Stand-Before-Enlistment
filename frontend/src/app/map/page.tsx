"use client";

import { MapScreen } from "@/features/map/MapScreen";
import type { ExploreContext } from "@/features/explore/model";

const unavailableContext: ExploreContext = {
  defaultActivityRegion: { id: "pending", name: "Activity region", neighborVerified: false },
  currentExploreRegion: { id: "pending", name: "Explore region", neighborVerified: false },
  exploreRegionCandidates: [], neighborVerifiedRegions: [], institutionRegions: [],
};

/** Production wiring waits for the agreed service/provider adapter. */
export default function MapPage() { return <MapScreen context={unavailableContext} />; }
