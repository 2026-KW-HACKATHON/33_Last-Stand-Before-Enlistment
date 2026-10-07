import type { AccountInfoService } from "./model";
export const accountInfoScenarios = ["normal", "loading", "empty", "error", "refresh-error"] as const;
export type AccountInfoScenario = typeof accountInfoScenarios[number];
/** Mutable result fixture belongs to this adapter, never a production email update API. */
export function createMockAccountInfoService(scenario: AccountInfoScenario = "normal", delay = 250) {
  let email: string | null = scenario === "empty" ? null : "neighbor@example.com", loads = 0;
  const service: AccountInfoService = { async load(signal) {
    signal.throwIfAborted();
    await new Promise<void>((resolve, reject) => {
      const abort = () => { clearTimeout(timer); signal.removeEventListener("abort", abort); reject(new Error("Aborted")); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, scenario === "loading" ? 3000 : delay);
      signal.addEventListener("abort", abort, { once: true });
    });
    signal.throwIfAborted(); loads++;
    if ((scenario === "error" && loads === 1) || (scenario === "refresh-error" && loads === 2)) throw new Error("Mock account load failed");
    return { registeredEmail: email };
  } };
  return { ...service, source: "mock" as const, simulateChangedResult() { email = "new@example.com"; } };
}
