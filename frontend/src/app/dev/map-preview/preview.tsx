"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { explorePreviewContext } from "@/features/explore/mock";
import { MapScreen } from "@/features/map/MapScreen";
import { createMapMockService, type MapMockMode } from "@/features/map/mock";
import { postFixtures } from "@/features/post/mock";

export function MapPreview() {
  const [mode, setMode] = useState<MapMockMode>("success");
  const [rendererError, setRendererError] = useState(false);
  return <div className="min-h-dvh bg-background p-4"><div className="mx-auto mb-3 flex max-w-mobile flex-wrap gap-2 rounded-card border border-border bg-surface p-3"><Button className="min-h-0 px-3 py-1 text-caption" variant={mode === "success" ? "primary" : "secondary"} onClick={() => setMode("success")}>Success</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={mode === "empty" ? "primary" : "secondary"} onClick={() => setMode("empty")}>Empty dongs</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={mode === "error" ? "primary" : "secondary"} onClick={() => setMode("error")}>Service error</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={rendererError ? "primary" : "secondary"} onClick={() => setRendererError((current) => !current)}>Renderer error</Button></div><MapScreen context={explorePreviewContext} service={createMapMockService(Object.values(postFixtures), mode)} rendererError={rendererError ? "Mock renderer could not render the map." : undefined} /></div>;
}
