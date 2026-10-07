"use client";

import { useEffect, useRef, useState } from "react";
import { BottomNavigation } from "../../components/layout/BottomNavigation";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { bottomNavigationDestinations, snapshotReference, useNavigation } from "../../lib/navigation";
import { PostCard } from "../post/PostCard";
import type { ExploreContext } from "../explore/model";
import { MockMapRenderer, type MapRenderer } from "./MapRenderer";
import { mapSnapshotReference, type MapLoadState, type MapViewport } from "./model";
import type { MapService } from "./service";
import { readMapSnapshot, rememberMapSnapshot } from "./snapshot";

type MapScreenProps = {
  context: ExploreContext;
  service?: MapService;
  renderer?: MapRenderer;
  rendererError?: string;
  onRetry?: () => void;
  onOpenPost?: (postId: string, snapshotRef: string) => void;
};

export function MapScreen({ context, service, renderer: Renderer = MockMapRenderer, rendererError, onRetry, onOpenPost }: MapScreenProps) {
  const navigation = useNavigation();
  const restored = readMapSnapshot(snapshotReference(navigation.state, { id: "map" }, "map"));
  const [viewport, setViewport] = useState<MapViewport>(() => restored?.viewport ?? { centerRegionId: context.defaultActivityRegion.id, zoom: 13 });
  const [state, setState] = useState<MapLoadState>(() => service ? { kind: "loading" } : { kind: "error", message: "Map data is waiting for a service connection." });
  const [selectedDongId, setSelectedDongId] = useState<string | null>(() => restored?.selectedDongId ?? null);
  const latestRequest = useRef(0);
  const request = async (nextViewport = viewport) => {
    if (!service) return;
    const requestId = ++latestRequest.current;
    setState({ kind: "loading" });
    try {
      const data = await service.getMap(nextViewport);
      if (requestId !== latestRequest.current) return;
      setState({ kind: "success", data });
      setSelectedDongId((current) => data.dongs.some((dong) => dong.region.id === current) ? current : data.dongs[0]?.region.id ?? null);
    } catch {
      if (requestId !== latestRequest.current) return;
      setState({ kind: "error", message: "Map data could not be loaded. Please retry." });
    }
  };
  useEffect(() => {
    const timer = window.setTimeout(() => { void request(); }, 0);
    return () => window.clearTimeout(timer);
    // Service changes are a new data source.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [service]);

  const changeViewport = (next: MapViewport) => { setViewport(next); void request(next); };
  const selected = state.kind === "success" ? state.data.dongs.find((dong) => dong.region.id === selectedDongId) : undefined;
  const openPost = () => {
    const post = selected?.representativePost;
    if (!post) return;
    const ref = mapSnapshotReference({ viewport, selectedDongId });
    rememberMapSnapshot(ref, { viewport, selectedDongId });
    navigation.registerSnapshot({ destination: { id: "map" }, kind: "map", ref });
    onOpenPost?.(post.id, ref);
    navigation.navigate({ destination: { id: "post", params: { postId: post.id } }, origin: { id: "map" }, sharedContextRef: ref });
  };

  return <MobileLayout header={<Header title="Issue map" />} bottomNavigation={<BottomNavigation activeItem="map" onNavigate={(item) => navigation.navigate({ destination: bottomNavigationDestinations[item], origin: { id: "map" } })} />}>
    <section className="rounded-card bg-soft p-section"><p className="text-caption text-secondary">Initial center</p><h1 className="text-section">{context.defaultActivityRegion.name}</h1><p className="mt-1 text-caption text-secondary">Changing the explore region does not change this map center.</p></section>
    {rendererError && <Notice tone="error" role="alert"><p>{rendererError}</p><Button className="mt-3" onClick={() => void request()}>Retry</Button></Notice>}
    {state.kind === "loading" && <div aria-busy="true" className="space-y-3"><div className="h-72 animate-pulse rounded-card bg-disabled" /><div className="h-24 animate-pulse rounded-card bg-disabled" /></div>}
    {state.kind === "error" && <Notice tone="error" role="alert"><p>{state.message}</p><Button className="mt-3" onClick={() => { onRetry?.(); void request(); }}>Retry</Button></Notice>}
    {state.kind === "success" && <><Renderer viewport={viewport} dongs={state.data.dongs} selectedDongId={selectedDongId} onSelectDong={setSelectedDongId} onViewportChange={changeViewport} />{selected && <section aria-live="polite"><h2 className="text-section">{selected.region.name}</h2>{selected.representativePost ? <PostCard className="mt-2" post={selected.representativePost} onOpen={openPost} /> : <Notice className="mt-2">No posts have been registered for this dong.</Notice>}</section>}</>}
  </MobileLayout>;
}
