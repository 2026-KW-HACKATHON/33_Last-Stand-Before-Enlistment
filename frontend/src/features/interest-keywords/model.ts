import { boardTopics, type BoardTopic } from "../explore/model";

/** Same existing seven-topic display contract for UI and recommendation consumers; not a wire DTO. */
export { boardTopics as interestKeywords };
export type InterestKeyword = BoardTopic;
export interface InterestKeywordService {
  readonly source: "mock" | "api";
  load(signal: AbortSignal): Promise<InterestKeyword[]>;
  save(keywords: readonly InterestKeyword[], signal: AbortSignal): Promise<InterestKeyword[]>;
}
export type KeywordState = {
  saved: readonly InterestKeyword[]; draft: readonly InterestKeyword[];
  phase: "idle" | "loading" | "ready" | "load-error" | "pending" | "success" | "save-error";
  scroll: number;
};
export function validateKeywords(value: readonly InterestKeyword[]): InterestKeyword[] {
  if (value.length > 4 || new Set(value).size !== value.length || value.some(item => !boardTopics.includes(item))) throw new Error("Invalid interest keywords");
  return [...value];
}
export function createKeywordStore(service: InterestKeywordService | null) {
  let state: KeywordState = { saved: [], draft: [], phase: "idle", scroll: 0 };
  let controller: AbortController | null = null;
  let failedDraft: InterestKeyword[] | null = null;
  let disposed = false;
  const listeners = new Set<() => void>();
  const update = (patch: Partial<KeywordState>) => { if (!disposed) { state = { ...state, ...patch }; listeners.forEach(listener => listener()); } };
  async function load() {
    if (disposed || state.phase === "pending") return;
    controller?.abort(); const request = controller = new AbortController(); update({ phase: "loading" });
    try {
      if (!service) throw new Error("Service unavailable");
      const saved = validateKeywords(await service.load(request.signal));
      if (!request.signal.aborted) { failedDraft = null; update({ saved, draft: [...saved], phase: "ready" }); }
    } catch { if (!request.signal.aborted) update({ phase: "load-error" }); }
  }
  async function persist(value: readonly InterestKeyword[]) {
    if (disposed || !["ready", "success", "save-error"].includes(state.phase)) return false;
    const target = validateKeywords(value);
    controller?.abort(); const request = controller = new AbortController(); update({ phase: "pending" });
    try {
      if (!service) throw new Error("Service unavailable");
      const saved = validateKeywords(await service.save(target, request.signal));
      if (request.signal.aborted) return false;
      failedDraft = null; update({ saved, draft: [...saved], phase: "success" }); return true;
    } catch {
      if (!request.signal.aborted) { failedDraft = target; update({ draft: [...state.saved], phase: "save-error" }); }
      return false;
    }
  }
  return {
    getState: () => state,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    activate() { disposed = false; update({ phase: state.phase === "loading" ? "idle" : state.phase === "pending" ? "ready" : state.phase, draft: state.phase === "pending" ? [...state.saved] : state.draft }); },
    load,
    toggle(keyword: InterestKeyword) {
      if (disposed || !["ready", "success", "save-error"].includes(state.phase) || !boardTopics.includes(keyword)) return;
      const selected = state.draft.includes(keyword);
      if (!selected && state.draft.length >= 4) return;
      failedDraft = null;
      update({ draft: selected ? state.draft.filter(item => item !== keyword) : [...state.draft, keyword], phase: "ready" });
    },
    save: () => persist(state.draft),
    retry: () => failedDraft ? persist(failedDraft) : Promise.resolve(false),
    cancel() { controller?.abort(); failedDraft = null; update({ draft: [...state.saved], phase: state.phase === "idle" || state.phase === "loading" || state.phase === "load-error" ? "idle" : "ready" }); },
    setScroll(scroll: number) { update({ scroll }); },
    dispose() { disposed = true; controller?.abort(); listeners.clear(); },
  };
}
export type KeywordStore = ReturnType<typeof createKeywordStore>;
