"use client";
import { useCallback, useRef, useState } from "react";
import { HomeScreen, BoardScreen } from "../explore/ExploreScreens";
import type { ExploreContext, ExploreRegion, BoardFilters, BoardPage } from "../explore/model";
import { MapHost } from "./MapHost";
import { MapScreen } from "../map/MapScreen";
import type { MapRenderer } from "../map/MapRenderer";
import { useFeatureServices } from "./provider";
import { useResource } from "./use-resource";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";

export function ExploreHost({ kind, mapRenderer }: { kind: "home" | "board" | "map"; mapRenderer?: MapRenderer }) {
  const services = useFeatureServices();
  const load = useCallback(async (signal: AbortSignal): Promise<ExploreContext> => {
    const [profile, completed, qualification, candidates] = await Promise.all([services!.profileService.load(signal), services!.neighborService.getCompletedRegions(signal), services!.institutionService.getQualification(signal), services!.profileService.search("", signal)]);
    if (profile.kind !== "success" || !profile.value?.activityRegion || candidates.kind !== "success") throw new Error("Context unavailable");
    const verified = new Set(completed.map(row => row.id));
    const activity = { id: profile.value.activityRegion.reference, name: profile.value.activityRegion.label, neighborVerified: verified.has(profile.value.activityRegion.reference) };
    return { defaultActivityRegion: activity, currentExploreRegion: activity, exploreRegionCandidates: candidates.value.map(row => ({ id: row.reference, name: row.label, neighborVerified: verified.has(row.reference) })), neighborVerifiedRegions: [...verified], institutionRegions: qualification.status === "completed" ? qualification.responsibleRegions.map(row => row.id) : [] };
  }, [services]);
  const { state, retry } = useResource(services ? load : null);
  if (state.kind !== "success") return <Notice tone={state.kind === "loading" ? "info" : "error"}>{state.kind === "loading" ? "탐색 지역을 불러오는 중입니다." : state.message}<Button onClick={retry}>다시 조회</Button></Notice>;
  return <ExploreReady key={state.data.defaultActivityRegion.id} initial={state.data} kind={kind} mapRenderer={mapRenderer ?? services?.mapRenderer} />;
}
function ExploreReady({ initial, kind, mapRenderer }: { initial: ExploreContext; kind: "home" | "board" | "map"; mapRenderer?: MapRenderer }) {
  const services = useFeatureServices()!;
  const generation = useRef(0);
  const [context, setContext] = useState(initial);
  const [filters, setFilters] = useState<BoardFilters>({ regionId: initial.currentExploreRegion.id, type: "ALL", topic: "ALL" });
  const [extra, setExtra] = useState<BoardPage | null>(null), [morePending, setMorePending] = useState(false), [moreError, setMoreError] = useState("");
  const load = useCallback(() => services.exploreService.getHome(context), [services, context]);
  const home = useResource(kind === "home" ? load : null);
  const loadBoard = useCallback(() => services.exploreService.getBoard(filters), [services, filters]);
  const board = useResource(kind === "board" ? loadBoard : null);
  const changeRegion = (region: ExploreRegion) => { generation.current++; setMorePending(false); setContext(current => ({ ...current, currentExploreRegion: region })); setFilters(current => ({ ...current, regionId: region.id })); setExtra(null); };
  if (kind === "map") return mapRenderer ? <MapScreen context={context} service={services.mapService} renderer={mapRenderer} /> : <MapHost context={context} />;
  if (kind === "home") return <HomeScreen context={context} state={home.state.kind === "unavailable" ? { kind: "error", message: home.state.message } : home.state} onRetry={home.retry} onExploreRegionChange={changeRegion} />;
  const current = board.state.kind === "success" ? extra ?? board.state.data : null;
  const more = async () => {
    if (!current?.pageInfo.nextCursor || morePending) return;
    const attempt = generation.current;
    setMorePending(true); setMoreError("");
    try { const next = await services.exploreService.getBoard(filters, current.pageInfo.nextCursor); if (attempt !== generation.current) return; setExtra({ posts: [...new Map([...current.posts, ...next.posts].map(post => [post.id, post])).values()], pageInfo: next.pageInfo }); }
    catch { if (attempt === generation.current) setMoreError("다음 페이지를 불러오지 못했습니다. 다시 시도해 주세요."); }
    finally { if (attempt === generation.current) setMorePending(false); }
  };
  return <><BoardScreen context={context} state={current ? { kind: "success", data: current } : board.state.kind === "unavailable" ? { kind: "error", message: board.state.message } : board.state} onRetry={() => { generation.current++; setMorePending(false); setExtra(null); board.retry(); }} onExploreRegionChange={changeRegion} onFiltersChange={next => { generation.current++; setMorePending(false); setExtra(null); setFilters(next); }} onLoadMore={() => void more()} loadingMore={morePending} />{moreError && <Notice tone="error">{moreError}</Notice>}</>;
}
