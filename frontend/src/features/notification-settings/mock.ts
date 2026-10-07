import type { PushPreferenceService } from "./model";
export const pushPreferenceScenarios = ["on", "off", "unset", "load-error", "save-error", "pending"] as const;
export type PushPreferenceScenario = typeof pushPreferenceScenarios[number];
/** Explicit test fixtures, not an assumed product default. No NotificationService or Push Provider side effects. */
export function createMockPushPreferenceService(scenario: PushPreferenceScenario = "on", delay = 250): PushPreferenceService {
  let saved: boolean | null = scenario === "unset" ? null : scenario !== "off";
  let failedLoad = false, failedSave = false;
  const wait = (signal: AbortSignal) => new Promise<void>((resolve, reject) => {
    signal.throwIfAborted();
    const abort = () => { clearTimeout(timer); signal.removeEventListener("abort", abort); reject(new Error("Aborted")); };
    const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delay);
    signal.addEventListener("abort", abort, { once: true });
  });
  return { source: "mock",
    async load(signal) { await wait(signal); if (scenario === "load-error" && !failedLoad) { failedLoad = true; throw new Error("Mock load error"); } return saved; },
    async save(enabled, signal) {
      await wait(signal);
      if (scenario === "pending") await new Promise<void>((_, reject) => { const abort = () => reject(new Error("Aborted")); signal.addEventListener("abort", abort, { once: true }); if (signal.aborted) abort(); });
      signal.throwIfAborted();
      if (scenario === "save-error" && !failedSave) { failedSave = true; throw new Error("Mock save error"); }
      saved = enabled; return saved;
    },
  };
}
