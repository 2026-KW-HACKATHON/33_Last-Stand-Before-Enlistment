import type { Feature, Polygon, MultiPolygon } from "geojson";
import { array, id, nullableText, object, text } from "./wire";
export type Boundary = { code: string; name: string; feature: Feature<Polygon | MultiPolygon>; bounds: readonly [number, number, number, number] };
export type MappedDong = Boundary & { regionId: string; regionName: string };
export type MapCatalog = { dongs: readonly MappedDong[]; unmatched: readonly { id: string; name: string; reason: string }[] };
export function decodeBoundaries(value: unknown): Boundary[] {
  const collection = object(value);
  if (collection.type !== "FeatureCollection") throw new Error("Expected boundary FeatureCollection");
  const codes = new Set<string>();
  return array(collection.features).map(value => {
    const row = object(value), properties = object(row.properties), geometry = object(row.geometry);
    if (row.type !== "Feature") throw new Error("Expected boundary feature");
    const code = text(properties.adm_cd2), name = text(properties.adm_nm);
    if (!/^\d{10}$/.test(code) || codes.has(code)) throw new Error("Invalid/duplicate MOIS boundary code");
    codes.add(code);
    let west = Infinity, south = Infinity, east = -Infinity, north = -Infinity;
    const polygon = (value: unknown): number[][][] => array(value).map(value => {
      const ring = array(value).map(value => {
        const position = array(value);
        if (position.length !== 2 || typeof position[0] !== "number" || typeof position[1] !== "number" || !Number.isFinite(position[0]) || !Number.isFinite(position[1]) || Math.abs(position[0]) > 180 || Math.abs(position[1]) > 90) throw new Error("Invalid WGS84 position");
        const [lng, lat] = position; west = Math.min(west, lng); east = Math.max(east, lng); south = Math.min(south, lat); north = Math.max(north, lat);
        return [lng, lat];
      });
      if (ring.length < 4 || ring[0][0] !== ring.at(-1)![0] || ring[0][1] !== ring.at(-1)![1]) throw new Error("Unclosed polygon ring");
      return ring;
    });
    const shape: Polygon | MultiPolygon = geometry.type === "Polygon" ? { type: "Polygon", coordinates: polygon(geometry.coordinates) } : geometry.type === "MultiPolygon" ? { type: "MultiPolygon", coordinates: array(geometry.coordinates).map(polygon) } : (() => { throw new Error("Unsupported boundary geometry"); })();
    if (![west, south, east, north].every(Number.isFinite)) throw new Error("Empty boundary");
    return { code, name, bounds: [west, south, east, north], feature: { type: "Feature", properties: { adm_cd2: code, adm_nm: name }, geometry: shape } };
  });
}
/** Explicit 10-digit MOIS key wins. Null keys may use a unique exact full administrative name. */
export function mapRegions(rows: unknown[], boundaries: readonly Boundary[]): MapCatalog {
  const byCode = new Map(boundaries.map(boundary => [boundary.code, boundary]));
  const byName = new Map<string, Boundary[]>();
  for (const boundary of boundaries) { const key = boundary.name.normalize("NFC").trim(); byName.set(key, [...(byName.get(key) ?? []), boundary]); }
  const matched: MappedDong[] = [], unmatched: MapCatalog["unmatched"][number][] = [];
  const ids = new Set<string>();
  for (const value of rows) {
    const row = object(value), regionId = id(row.id), name = text(row.name), key = nullableText(row.mapFeatureKey);
    if (ids.has(regionId)) throw new Error("Duplicate server region ID"); ids.add(regionId);
    const candidates = key !== undefined ? (byCode.has(key) ? [byCode.get(key)!] : []) : byName.get(name.normalize("NFC").trim()) ?? [];
    if (candidates.length !== 1) { unmatched.push({ id: regionId, name, reason: key ? "등록된 지도 키에 대응하는 경계가 없습니다." : "전체 행정동 이름이 일치하는 유일한 경계가 없습니다." }); continue; }
    matched.push({ ...candidates[0], regionId, regionName: name });
  }
  const counts = new Map<string, number>(); matched.forEach(row => counts.set(row.code, (counts.get(row.code) ?? 0) + 1));
  return { dongs: matched.filter(row => { if (counts.get(row.code) === 1) return true; unmatched.push({ id: row.regionId, name: row.regionName, reason: "한 경계에 여러 서버 지역 ID가 연결되어 있습니다." }); return false; }), unmatched };
}
export function regionsInBounds(catalog: MapCatalog, view: readonly [number, number, number, number]): string[] {
  return catalog.dongs.filter(({ bounds: [west, south, east, north] }) => west <= view[2] && east >= view[0] && south <= view[3] && north >= view[1]).map(row => row.regionId);
}
