import { evaluateGuard, type SessionState } from "../../lib/navigation/guard";
/** FE display contract only; #74/#11 must confirm the real wire adapter. */
export type CompletedRegion = Readonly<{ id: string; name: string }>;
export interface NeighborService { getCompletedRegions(signal: AbortSignal): Promise<readonly CompletedRegion[]> }
export type NeighborState = { status: "idle" | "loading" | "error" } | { status: "ready"; regions: readonly CompletedRegion[] };
export function validateRegions(regions: readonly CompletedRegion[]): readonly CompletedRegion[] {
 if (!Array.isArray(regions) || regions.length > 3 || regions.some(r => !r || typeof r.id !== "string" || !r.id.trim() || typeof r.name !== "string" || !r.name.trim()) || new Set(regions.map(r => r.id)).size !== regions.length) throw new Error("Invalid completed regions");
 return regions.map(r => Object.freeze({ id: r.id, name: r.name }));
}
export function neighborParticipation(state: NeighborState, session: SessionState, regionId: string) {
 const guard = evaluateGuard({ id: "newPost" }, session, { regionId });
 if (guard.status !== "allowed" && guard.status !== "forbidden") return guard;
 if (state.status === "idle" || state.status === "loading") return { status: "loading" } as const;
 if (state.status !== "ready" || !regionId) return { status: "error", retryable: true } as const;
 const completed = state.regions.some(r => r.id === regionId);
 // Conflicting display/capability assertions must be requeried, never inferred.
 if (completed !== (guard.status === "allowed")) return { status: "error", retryable: true } as const;
 return guard;
}
export function createNeighborStore(service: NeighborService | null) {
 let state: NeighborState = { status: "idle" }; let controller: AbortController | null = null; let generation = 0;
 const listeners = new Set<() => void>();
 const publish = (next: NeighborState) => { state = next; listeners.forEach(fn => fn()); };
 const clear = () => { generation++; controller?.abort(); controller = null; publish({ status: "idle" }); };
 return {
  getState: () => state,
  subscribe: (fn: () => void) => { listeners.add(fn); return () => { listeners.delete(fn); }; },
  clear,
  async load() {
   const request = ++generation; controller?.abort(); controller = new AbortController(); const signal = controller.signal;
   publish({ status: "loading" });
   try { if (!service) throw new Error("Service unavailable"); const regions = validateRegions(await service.getCompletedRegions(signal)); if (request === generation && !signal.aborted) publish({ status: "ready", regions }); }
   catch { if (request === generation && !signal.aborted) publish({ status: "error" }); }
  },
  dispose() { clear(); listeners.clear(); },
 };
}
