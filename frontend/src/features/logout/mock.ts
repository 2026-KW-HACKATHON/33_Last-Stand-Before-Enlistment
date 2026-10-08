import type { CurrentDeviceLogoutService } from "./model";
export type LogoutScenario = "normal" | "error-retry";
/** One adapter instance is one current device. No global session list or token invalidation. */
export function createMockLogoutService(scenario: LogoutScenario = "normal", delay = 300): CurrentDeviceLogoutService & { isCurrentDeviceActive: () => boolean } {
  let active = true, failedOnce = false;
  return {
    source: "mock", isCurrentDeviceActive: () => active,
    async endCurrentDevice(subjectKey, signal) {
      await new Promise<void>((resolve, reject) => {
        if (signal.aborted) { reject(new Error("Aborted")); return; }
        const abort = () => { clearTimeout(timer); signal.removeEventListener("abort", abort); reject(new Error("Aborted")); };
        const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delay);
        signal.addEventListener("abort", abort, { once: true });
      });
      if (signal.aborted) throw new Error("Aborted");
      if (scenario === "error-retry" && !failedOnce) { failedOnce = true; return { kind: "failure" }; }
      active = false; return { kind: "logged-out", subjectKey };
    },
  };
}
