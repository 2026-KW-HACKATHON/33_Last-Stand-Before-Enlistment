"use client";
import { useCallback } from "react";
import { MapScreen } from "../map/MapScreen";
import { GeographicMapRenderer, MapGeometryProvider } from "../map/GeographicMapRenderer";
import type { ExploreContext } from "../explore/model";
import { useFeatureServices } from "./provider";
import { useResource } from "./use-resource";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
export function MapHost({ context }: { context: ExploreContext }) {
  const services = useFeatureServices()!;
  const load = useCallback((signal: AbortSignal) => services.mapCatalogService.get(signal), [services]);
  const { state, retry } = useResource(load);
  if (state.kind !== "success") return <Notice>{state.kind === "loading" ? "행정동 경계를 불러오는 중입니다." : state.message}<Button onClick={retry}>경계 다시 조회</Button></Notice>;
  if (!state.data.dongs.some(row => row.regionId === context.defaultActivityRegion.id)) return <Notice tone="warning">기본 활동 지역과 정확히 일치하는 행정동 경계가 없습니다. 서버 지역의 전체 행정동 이름 또는 10자리 지도 키를 확인해 주세요.<Button onClick={retry}>지역 다시 조회</Button></Notice>;
  return <MapGeometryProvider catalog={state.data}><MapScreen context={context} service={services.mapService} renderer={GeographicMapRenderer} />{state.data.unmatched.length > 0 && <Notice tone="warning">경계 연결이 확인되지 않은 지역 {state.data.unmatched.length}개는 지도에 표시하지 않습니다.</Notice>}</MapGeometryProvider>;
}
