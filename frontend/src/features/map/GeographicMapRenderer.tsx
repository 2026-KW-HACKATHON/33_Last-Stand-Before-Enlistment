"use client";
import "leaflet/dist/leaflet.css";
import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import type { GeoJSON, Map as LeafletMap } from "leaflet";
import type { MapRendererProps } from "./MapRenderer";
import { regionsInBounds, type MapCatalog } from "../integration/map-geometry";
import { Notice } from "../../components/ui/Notice";
const Context = createContext<MapCatalog | null>(null);
export function MapGeometryProvider({ catalog, children }: { catalog: MapCatalog; children: ReactNode }) { return <Context.Provider value={catalog}>{children}</Context.Provider>; }
export function GeographicMapRenderer({ viewport, dongs, selectedDongId, onSelectDong, onViewportChange }: MapRendererProps) {
  const catalog = useContext(Context);
  const element = useRef<HTMLDivElement>(null), mapRef = useRef<LeafletMap | null>(null), layers = useRef(new Map<string, GeoJSON>());
  const callbacks = useRef({ onSelectDong, onViewportChange });
  const [error, setError] = useState("");
  useEffect(() => { callbacks.current = { onSelectDong, onViewportChange }; }, [onSelectDong, onViewportChange]);
  useEffect(() => {
    if (!catalog || !element.current) return;
    const node = element.current, center = catalog.dongs.find(row => row.regionId === viewport.centerRegionId);
    if (!center) return;
    let active = true, map: LeafletMap | undefined;
    void import("leaflet").then(L => {
      if (!active) return;
      map = L.map(node, { zoomControl: true }); mapRef.current = map;
      const [west, south, east, north] = center.bounds;
      map.setView(viewport.center ? [viewport.center[0], viewport.center[1]] : [(south + north) / 2, (west + east) / 2], viewport.zoom);
      const tiles = L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", { maxZoom: 19, attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' }).addTo(map);
      tiles.on("tileerror", () => { if (active) setError("배경지도 타일을 불러오지 못했습니다. 행정동 경계와 게시물은 계속 확인할 수 있습니다."); });
      for (const dong of catalog.dongs) {
        const layer = L.geoJSON(dong.feature, { style: { color: "#315C44", weight: 1.5, fillOpacity: 0.15 } }).addTo(map);
        layer.on("click", () => callbacks.current.onSelectDong(dong.regionId));
        const label = document.createElement("span"); label.textContent = dong.regionName; layer.bindTooltip(label);
        layers.current.set(dong.regionId, layer);
      }
      const publish = () => {
        if (!map || !active) return;
        const bounds = map.getBounds(), center = map.getCenter();
        callbacks.current.onViewportChange({ centerRegionId: viewport.centerRegionId, zoom: map.getZoom(), center: [center.lat, center.lng], regionIds: regionsInBounds(catalog, [bounds.getWest(), bounds.getSouth(), bounds.getEast(), bounds.getNorth()]) });
      };
      map.on("moveend", publish); publish();
    }).catch(() => { if (active) setError("지도 SDK를 불러오지 못했습니다. 새로고침해 주세요."); });
    const currentLayers = layers.current;
    return () => { active = false; map?.remove(); mapRef.current = null; currentLayers.clear(); };
    // The catalog fixes geometry. Pan/zoom stay inside this map instance.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [catalog]);
  useEffect(() => {
    for (const [id, layer] of layers.current) layer.setStyle({ fillOpacity: selectedDongId === id ? 0.45 : 0.15, weight: selectedDongId === id ? 3 : 1.5 });
  }, [selectedDongId, dongs]);
  return <section aria-label="행정동 이슈 지도">
    <div ref={element} className="h-[400px] w-full rounded-card border border-border" aria-label="확대·축소와 이동이 가능한 실제 지도" />
    {error && <Notice tone="warning">{error}</Notice>}
    <label className="mt-2 block">표시 지역 선택<select aria-label="지도 지역 선택" value={selectedDongId ?? ""} onChange={event => onSelectDong(event.currentTarget.value)}><option value="" disabled>지역 선택</option>{dongs.map(dong => <option key={dong.region.id} value={dong.region.id}>{dong.region.name} · {dong.representativePost ? `반응 ${dong.representativePost.reactionCount}` : "게시물 없음"}</option>)}</select></label>
    <p className="mt-2 text-caption text-secondary">행정동 경계: <a href="https://sgis.kostat.go.kr" target="_blank" rel="noreferrer">통계청 SGIS · 공공누리 제1유형</a>, 가공: <a href="https://github.com/vuski/admdongkor" target="_blank" rel="noreferrer">vuski/admdongkor · CC BY 4.0</a>. 2026-07-01 자료를 화면 표시용으로 단순화했습니다.</p>
  </section>;
}
