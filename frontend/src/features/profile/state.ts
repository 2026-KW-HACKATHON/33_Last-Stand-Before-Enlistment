import type { ProfileService, ProfileValue, Result } from "./contracts";
import type { SignupRegion } from "../signup/contracts";
export const emptyProfile = (): ProfileValue => ({ nickname: "", bio: "", attributes: [], activityRegion: null });
export const validProfile = (v: ProfileValue) => Boolean(v.nickname.trim()) && Array.from(v.nickname.trim()).length <= 10 && Array.from(v.bio).length <= 50;
export type ProfileState = { saved: ProfileValue | null; draft: ProfileValue; phase: "idle" | "loading" | "editing" | "saving" | "success" | "error" | "empty"; failure: Extract<Result<never>, { kind: "failure" }> | null; query: string; candidates: SignupRegion[]; candidate: SignupRegion | null; candidatePhase: "idle" | "loading" | "ready" | "empty" | "error" | "denied" | "unavailable"; origin: "profile" | "me"; scroll: { profile: number; region: number } };
export function createProfileStore(service: ProfileService | null) {
  let state: ProfileState = { saved: null, draft: emptyProfile(), phase: "idle", failure: null, query: "", candidates: [], candidate: null, candidatePhase: "idle", origin: "me", scroll: { profile: 0, region: 0 } };
  let regionBefore: SignupRegion | null = null;
  let active = false, version = 0, candidateVersion = 0;
  let operation: AbortController | null = null, candidateOperation: AbortController | null = null;
  const listeners = new Set<() => void>();
  const update = (patch: Partial<ProfileState>) => { state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  const failure = (reason: "unavailable" | "unknown" | "failed", retryable = true) => ({ kind: "failure" as const, reason, retryable });
  const interrupt = () => { version++; candidateVersion++; operation?.abort(); candidateOperation?.abort(); };
  async function load() {
    if (!active || state.phase === "loading" || state.phase === "saving") return;
    if (!service) return update({ phase: "error", failure: failure("unavailable", false) });
    const id = ++version; operation?.abort(); operation = new AbortController(); update({ phase: "loading", failure: null });
    try { const result = await service.load(operation.signal); if (!active || id !== version) return;
      if (result.kind === "failure") return update({ phase: "error", failure: result });
      update({ saved: structuredClone(result.value), draft: result.value ? structuredClone(result.value) : emptyProfile(), phase: result.value ? "editing" : "empty" });
    } catch { if (active && id === version) update({ phase: "error", failure: failure("failed") }); }
  }
  async function candidates(location: boolean) {
    if (!active) return;
    if (!service) return update({ candidatePhase: "unavailable" });
    const id = ++candidateVersion; candidateOperation?.abort(); candidateOperation = new AbortController(); update({ candidatePhase: "loading", candidates: [], candidate: null });
    try { const result = location ? await service.locate(candidateOperation.signal) : await service.search(state.query, candidateOperation.signal); if (!active || id !== candidateVersion) return;
      if (result.kind === "success") update({ candidates: structuredClone(result.value), candidatePhase: result.value.length ? "ready" : "empty" });
      else update({ candidatePhase: result.kind === "denied" ? "denied" : result.kind === "unavailable" ? "unavailable" : "error" });
    } catch { if (active && id === candidateVersion) update({ candidatePhase: "error" }); }
  }
  return {
    getState: () => state, subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    setActive(value: boolean) { if (active === value) return; active = value; if (!value) { const saving = state.phase === "saving"; interrupt(); update({ phase: saving ? "error" : state.saved ? "editing" : "idle", failure: saving ? failure("unknown", false) : null, candidatePhase: "idle", candidates: [], candidate: null }); } },
    load,
    edit(value: ProfileValue) { if (active && state.phase !== "saving" && state.phase !== "loading") update({ draft: structuredClone(value), phase: "editing", failure: state.failure?.reason === "unknown" ? state.failure : null }); },
    setQuery(query: string) { candidateVersion++; candidateOperation?.abort(); update({ query, candidate: null, candidates: [], candidatePhase: "idle" }); },
    search: () => candidates(false), locate: () => candidates(true),
    choose(region: SignupRegion) { if (active && state.candidatePhase === "ready" && state.candidates.some(v => v.reference === region.reference)) update({ candidate: structuredClone(region) }); },
    applyCandidate() { if (!active || !state.candidate || state.phase === "saving") return; update({ draft: { ...state.draft, activityRegion: structuredClone(state.candidate) }, phase: "editing" }); },
    setOrigin(origin: ProfileState["origin"]) { regionBefore = structuredClone(state.draft.activityRegion); update({ origin }); }, setScroll(page: "profile" | "region", value: number) { update({ scroll: { ...state.scroll, [page]: value } }); },
    async save() {
      if (!active || state.phase === "saving" || state.phase === "loading" || state.failure?.retryable === false || !validProfile(state.draft) || !state.draft.activityRegion) return;
      if (!service) return update({ phase: "error", failure: failure("unavailable", false) });
      const id = ++version; operation?.abort(); operation = new AbortController(); update({ phase: "saving", failure: null });
      try { const result = await service.save(structuredClone(state.draft), operation.signal); if (!active || id !== version) return;
        if (result.kind === "failure") return update({ phase: "editing", failure: result });
        update({ saved: structuredClone(result.value), draft: structuredClone(result.value), phase: "success", failure: null });
      } catch { if (active && id === version) update({ phase: "error", failure: failure("unknown", false) }); }
    },
    cancelRegion() { candidateVersion++; candidateOperation?.abort(); update({ draft: { ...state.draft, activityRegion: structuredClone(state.origin === "profile" ? regionBefore : state.saved?.activityRegion ?? null) }, candidate: null, candidatePhase: "idle" }); },
    cancel() { const saving = state.phase === "saving" || state.failure?.reason === "unknown"; interrupt(); update({ draft: state.saved ? structuredClone(state.saved) : emptyProfile(), phase: saving ? "error" : "editing", failure: saving ? failure("unknown", false) : null, candidate: null, candidatePhase: "idle" }); },
    clear() { interrupt(); active = false; update({ saved: null, draft: emptyProfile(), phase: "idle", failure: null, query: "", candidates: [], candidate: null, candidatePhase: "idle", origin: "me", scroll: { profile: 0, region: 0 } }); },
    dispose() { const phase = state.phase; interrupt(); if (phase === "loading") update({ phase: "idle" }); else if (phase === "saving") update({ phase: "error", failure: failure("unknown", false) }); },
  };
}
export type ProfileStore = ReturnType<typeof createProfileStore>;
