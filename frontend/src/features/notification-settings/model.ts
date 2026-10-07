/** FE preference adapter, not a wire DTO, OS permission or Push subscription contract. */
export interface PushPreferenceService {
  readonly source: "mock" | "api";
  load(signal: AbortSignal): Promise<boolean | null>;
  save(enabled: boolean, signal: AbortSignal): Promise<boolean>;
}
export type PushPreferenceState = {
  saved: boolean | null; draft: boolean | null; scroll: number;
  phase: "idle" | "loading" | "ready" | "unset" | "load-error" | "pending" | "success" | "save-error";
};
export function createPushPreferenceStore(service: PushPreferenceService | null) {
  let state: PushPreferenceState = { saved: null, draft: null, scroll: 0, phase: "idle" };
  let request: AbortController | null = null, failedValue: boolean | null = null;
  let disposed = false;
  const listeners = new Set<() => void>();
  const update = (patch: Partial<PushPreferenceState>) => { if (!disposed) { state = { ...state, ...patch }; listeners.forEach(listener => listener()); } };
  async function load() {
    if (disposed || state.phase === "pending") return;
    request?.abort(); const current = request = new AbortController(); update({ phase: "loading" });
    try {
      if (!service) throw new Error("Preference adapter unavailable");
      const saved = await service.load(current.signal);
      if (saved !== null && typeof saved !== "boolean") throw new Error("Invalid preference");
      if (!current.signal.aborted) { failedValue = null; update({ saved, draft: saved, phase: saved === null ? "unset" : "ready" }); }
    } catch { if (!current.signal.aborted) update({ phase: "load-error" }); }
  }
  async function save(value: boolean) {
    if (disposed || typeof value !== "boolean" || !["ready", "unset", "success", "save-error"].includes(state.phase)) return false;
    request?.abort(); const current = request = new AbortController(); update({ draft: value, phase: "pending" });
    try {
      if (!service) throw new Error("Preference adapter unavailable");
      const saved = await service.save(value, current.signal);
      if (typeof saved !== "boolean") throw new Error("Invalid saved preference");
      if (current.signal.aborted) return false;
      failedValue = null; update({ saved, draft: saved, phase: "success" }); return true;
    } catch {
      if (!current.signal.aborted) { failedValue = value; update({ draft: state.saved, phase: "save-error" }); }
      return false;
    }
  }
  return {
    getState: () => state,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    load, save,
    retry: () => failedValue === null ? Promise.resolve(false) : save(failedValue),
    cancel() { request?.abort(); failedValue = null; update({ draft: state.saved, phase: ["idle", "loading"].includes(state.phase) ? "idle" : state.phase === "load-error" ? "load-error" : state.saved === null ? "unset" : "ready" }); },
    setScroll(scroll: number) { update({ scroll }); },
    activate() { disposed = false; if (state.phase === "loading") update({ phase: "idle" }); if (state.phase === "pending") update({ draft: state.saved, phase: state.saved === null ? "unset" : "ready" }); },
    dispose() { disposed = true; request?.abort(); listeners.clear(); },
  };
}
export type PushPreferenceStore = ReturnType<typeof createPushPreferenceStore>;
