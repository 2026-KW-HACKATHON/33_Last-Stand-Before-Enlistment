import type { MapSnapshot } from "./model";

/** Feature-owned memory addressed by #40's opaque map snapshot reference. */
const snapshots = new Map<string, MapSnapshot>();

export function rememberMapSnapshot(ref: string, snapshot: MapSnapshot) {
  snapshots.set(ref, snapshot);
}

export function readMapSnapshot(ref: string | undefined): MapSnapshot | undefined {
  return ref ? snapshots.get(ref) : undefined;
}
