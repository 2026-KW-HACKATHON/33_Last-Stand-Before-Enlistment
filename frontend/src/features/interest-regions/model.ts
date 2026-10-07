import type { SignupRegion } from "../signup/contracts";

/** FE service port. Uses the existing region display type, not an API DTO or a new region ID. */
export interface InterestRegionService {
  readonly source: "mock" | "api";
  load(signal: AbortSignal): Promise<SignupRegion[]>;
  search(query: string, signal: AbortSignal): Promise<SignupRegion[]>;
  save(regions: readonly SignupRegion[], signal: AbortSignal): Promise<SignupRegion[]>;
}
export type InterestRegionState = {
  saved: readonly SignupRegion[];
  candidates: readonly SignupRegion[];
  query: string;
  phase: "idle" | "loading" | "ready" | "error";
  searchPhase: "idle" | "loading" | "ready" | "error";
  mutation: "idle" | "pending" | "success" | "error";
  pending: SignupRegion | null;
  scroll: number;
};
export function normalizeRegions(regions: readonly SignupRegion[]): SignupRegion[] {
  if (regions.some(region => !region.reference.trim() || !region.label.trim())) throw new Error("Invalid region display");
  return Array.from(new Map(regions.map(region => [region.reference, { ...region }])).values());
}
export function createInterestRegionStore(service: InterestRegionService | null) {
  let state: InterestRegionState = { saved: [], candidates: [], query: "", phase: "idle", searchPhase: "idle", mutation: "idle", pending: null, scroll: 0 };
  const listeners = new Set<() => void>();
  let disposed = false;
  let loadController: AbortController | null = null;
  let searchController: AbortController | null = null;
  let saveController: AbortController | null = null;
  let retryRegion: SignupRegion | null = null;
  function update(patch: Partial<InterestRegionState>) {
    if (disposed) return;
    state = { ...state, ...patch };
    listeners.forEach(listener => listener());
  }
  async function load() {
    if (disposed || state.mutation === "pending") return;
    loadController?.abort();
    const controller = loadController = new AbortController();
    update({ phase: "loading" });
    try {
      if (!service) throw new Error("Service unavailable");
      const saved = normalizeRegions(await service.load(controller.signal));
      if (!controller.signal.aborted) update({ saved, phase: "ready" });
    } catch { if (!controller.signal.aborted) update({ phase: "error" }); }
  }
  async function search() {
    if (disposed) return;
    searchController?.abort();
    const controller = searchController = new AbortController();
    update({ searchPhase: "loading" });
    try {
      if (!service) throw new Error("Service unavailable");
      const candidates = normalizeRegions(await service.search(state.query, controller.signal));
      if (!controller.signal.aborted) update({ candidates, searchPhase: "ready" });
    } catch { if (!controller.signal.aborted) update({ searchPhase: "error" }); }
  }
  async function toggle(region: SignupRegion) {
    if (disposed || state.phase !== "ready" || state.mutation === "pending") return;
    const exists = state.saved.some(saved => saved.reference === region.reference);
    const next = exists ? state.saved.filter(saved => saved.reference !== region.reference) : [...state.saved, region];
    const controller = saveController = new AbortController();
    retryRegion = region;
    update({ mutation: "pending", pending: region });
    try {
      if (!service) throw new Error("Service unavailable");
      const saved = normalizeRegions(await service.save(next, controller.signal));
      if (!controller.signal.aborted) { retryRegion = null; update({ saved, mutation: "success", pending: null }); }
    } catch { if (!controller.signal.aborted) update({ mutation: "error", pending: null }); }
  }
  function cancel() {
    saveController?.abort();
    retryRegion = null;
    update({ mutation: "idle", pending: null });
  }
  return {
    /** React StrictMode re-attaches the same host; restart aborted reads, never replay a mutation. */
    activate() {
      disposed = false;
      update({ phase: state.phase === "loading" ? "idle" : state.phase, searchPhase: state.searchPhase === "loading" ? "idle" : state.searchPhase, mutation: state.mutation === "pending" ? "idle" : state.mutation, pending: null });
    },
    getState: () => state,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    load, search, toggle,
    retrySave: () => retryRegion ? toggle(retryRegion) : Promise.resolve(),
    setQuery(query: string) { searchController?.abort(); update({ query, candidates: [], searchPhase: "idle" }); },
    setScroll(scroll: number) { update({ scroll }); },
    cancel,
    dispose() { disposed = true; loadController?.abort(); searchController?.abort(); saveController?.abort(); listeners.clear(); },
  };
}
export type InterestRegionStore = ReturnType<typeof createInterestRegionStore>;
