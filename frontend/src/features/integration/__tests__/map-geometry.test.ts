import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";
import { decodeBoundaries, mapRegions, regionsInBounds } from "../map-geometry";
const polygon = (code: string, name: string) => ({ type: "Feature", properties: { adm_cd2: code, adm_nm: name }, geometry: { type: "Polygon", coordinates: [[[126.9, 37.5], [127, 37.5], [127, 37.6], [126.9, 37.5]]] } });
const source = { type: "FeatureCollection", features: [polygon("1135056000", "서울특별시 노원구 월계1동")] };
test("server numeric ID maps by exact full name without mutating DB", () => {
  const catalog = mapRegions([{ id: 1, name: "서울특별시 노원구 월계1동", mapFeatureKey: null }], decodeBoundaries(source)); assert.equal(catalog.dongs[0].regionId, "1"); assert.equal(catalog.dongs[0].code, "1135056000"); assert.equal(catalog.unmatched.length, 0);
});
test("explicit 10-digit map key uses MOIS code even if label differs", () => {
  const catalog = mapRegions([{ id: 1, name: "화면 이름", mapFeatureKey: "1135056000" }], decodeBoundaries(source)); assert.equal(catalog.dongs[0].regionName, "화면 이름");
});
test("short name and unknown explicit key never guess dong", () => {
  const catalog = mapRegions([{ id: 1, name: "월계1동", mapFeatureKey: null }, { id: 2, name: "서울특별시 노원구 월계1동", mapFeatureKey: "unknown" }], decodeBoundaries(source)); assert.equal(catalog.dongs.length, 0); assert.equal(catalog.unmatched.length, 2);
});
test("ambiguous names and duplicate feature assignment fail closed", () => {
  const boundaries = decodeBoundaries({ ...source, features: [...source.features, polygon("1135057000", "서울특별시 노원구 월계1동")] });
  assert.equal(mapRegions([{ id: 1, name: "서울특별시 노원구 월계1동", mapFeatureKey: null }], boundaries).dongs.length, 0);
  assert.equal(mapRegions([{ id: 1, name: "이름", mapFeatureKey: "1135056000" }, { id: 2, name: "이름", mapFeatureKey: "1135056000" }], boundaries).dongs.length, 0);
});
test("viewport returns only registered intersecting server IDs", () => {
  const catalog = mapRegions([{ id: 23, name: "서울특별시 노원구 월계1동", mapFeatureKey: null }], decodeBoundaries(source));
  assert.deepEqual(regionsInBounds(catalog, [126.95, 37.55, 127.1, 37.7]), ["23"]); assert.deepEqual(regionsInBounds(catalog, [128, 36, 129, 37]), []);
});
test("invalid WGS84/unclosed rings/duplicate codes are rejected", () => {
  const invalid = polygon("1135056000", "이름"); invalid.geometry.coordinates[0][0] = [200, 100];
  assert.throws(() => decodeBoundaries({ ...source, features: [invalid] })); assert.throws(() => decodeBoundaries({ ...source, features: [source.features[0], source.features[0]] }));
});
test("bundled real dataset has 3558 valid features and exact Wolgye-1 mapping", () => {
  const relative = "public/maps/administrative-dongs-20260701.geojson";
  const path = existsSync(resolve(relative)) ? resolve(relative) : resolve("frontend", relative);
  const boundaries = decodeBoundaries(JSON.parse(readFileSync(path, "utf8")) as unknown);
  assert.equal(boundaries.length, 3558);
  const catalog = mapRegions([{ id: 1, name: "서울특별시 노원구 월계1동", mapFeatureKey: null }], boundaries);
  assert.equal(catalog.dongs[0].code, "1135056000");
  const [, south, , north] = catalog.dongs[0].bounds; assert.ok(south > 37 && north < 38);
});
