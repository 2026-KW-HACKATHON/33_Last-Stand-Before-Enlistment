"use client";
/* eslint-disable @next/next/no-img-element -- map SDK/image host is intentionally unselected. */

import type { ComponentType } from "react";
import type { MapDong, MapViewport } from "./model";

export type MapRendererProps = {
  viewport: MapViewport;
  dongs: readonly MapDong[];
  selectedDongId: string | null;
  onSelectDong: (dongId: string) => void;
  onViewportChange: (viewport: MapViewport) => void;
};

/** Provider boundary: no map SDK, coordinates, or boundaries are selected here. */
export type MapRenderer = ComponentType<MapRendererProps>;

/** Development renderer for UI/state verification only; it is not geographical data. */
export function MockMapRenderer({ viewport, dongs, selectedDongId, onSelectDong, onViewportChange }: MapRendererProps) {
  return <section aria-label="지도 미리보기" className="relative overflow-hidden rounded-card border border-border bg-soft p-section">
    <div className="grid min-h-72 grid-cols-2 gap-3">
      {dongs.map((dong) => {
        const selected = dong.region.id === selectedDongId;
        const post = dong.representativePost;
        const firstImage = post?.images[0];
        return <button key={dong.region.id} type="button" onClick={() => onSelectDong(dong.region.id)} className={`relative min-h-28 overflow-hidden rounded-card border p-3 text-left ${selected ? "border-primary ring-2 ring-primary" : "border-border bg-surface"}`}>
          {firstImage && <img src={firstImage.url} alt="" className="absolute inset-0 h-full w-full object-cover opacity-25" />}
          <span className="relative block text-card-title text-text">{dong.region.name}</span>
          <span className="relative mt-2 block text-caption text-secondary">{post ? `반응 ${post.reactionCount}` : "게시물 없음"}</span>
        </button>;
      })}
    </div>
    <div className="relative mt-3 flex items-center justify-between gap-2 rounded-input bg-surface px-3 py-2 text-caption text-secondary"><span>확대 수준 {viewport.zoom}</span><div className="flex gap-2"><button type="button" className="rounded border border-border px-2 py-1" disabled={!selectedDongId} onClick={() => selectedDongId && onViewportChange({ ...viewport, centerRegionId: selectedDongId })}>중심으로</button><button type="button" className="rounded border border-border px-2 py-1" onClick={() => onViewportChange({ ...viewport, zoom: Math.max(1, viewport.zoom - 1) })}>−</button><button type="button" className="rounded border border-border px-2 py-1" onClick={() => onViewportChange({ ...viewport, zoom: viewport.zoom + 1 })}>+</button></div></div>
  </section>;
}
