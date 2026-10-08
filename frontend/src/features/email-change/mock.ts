import type { AccountInfoService } from "../account-info/model";
import type { Confirmation, EmailChangeFailure, EmailChangeService } from "./contracts";
export const emailChangeScenarios = ["normal", "empty", "load-error", "send-failed", "mismatch", "expired", "cancelled", "duplicate", "failed"] as const;
export type EmailChangeScenario = typeof emailChangeScenarios[number];
/** Shared per-member fixture for L02 lookup and L03/L04 change, never used in production. */
export function createMockEmailChangeServices(subjectKey: string, scenario: EmailChangeScenario = "normal", delay = 300) {
  let registeredEmail: string | null = scenario === "empty" ? null : "neighbor@example.com";
  let failedOnce = false, loadFailedOnce = false;
  const handles = new WeakMap<Confirmation, { email: string; verified: boolean; active: boolean }>();
  const wait = (signal: AbortSignal) => new Promise<void>((resolve, reject) => {
    if (signal.aborted) { reject(new Error("Aborted")); return; }
    const abort = () => { clearTimeout(timer); signal.removeEventListener("abort", abort); reject(new Error("Aborted")); };
    const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delay);
    signal.addEventListener("abort", abort, { once: true });
  });
  const failOnce = (reasons: EmailChangeFailure[]) => { if (!failedOnce && reasons.includes(scenario as EmailChangeFailure)) { failedOnce = true; return { kind: "failure" as const, reason: scenario as EmailChangeFailure }; } return null; };
  const account: AccountInfoService = { async load(signal) { await wait(signal); if (scenario === "load-error" && !loadFailedOnce) { loadFailedOnce = true; throw new Error("Mock lookup failure"); } return { registeredEmail }; } };
  const change: EmailChangeService = {
    source: "mock",
    async begin(subject, email, signal) {
      await wait(signal);
      if (subject !== subjectKey) return { kind: "failure", reason: "unavailable" };
      // This fixture represents an email linked to another local member; never recover it.
      if (email.toLowerCase() === "other@example.com") return { kind: "failure", reason: "duplicate" };
      const failure = failOnce(["send-failed", "duplicate"]); if (failure) return failure;
      const confirmation = {}; handles.set(confirmation, { email, verified: false, active: true });
      return { kind: "pending", confirmation };
    },
    async confirm(confirmation, signal) {
      await wait(signal); const data = handles.get(confirmation);
      if (!data?.active) return { kind: "failure", reason: "expired" };
      data.verified = false;
      const failure = failOnce(["mismatch", "expired", "cancelled"]);
      if (failure) { if (failure.reason !== "mismatch") data.active = false; return failure; }
      data.verified = true; return { kind: "verified" };
    },
    async change(confirmation, signal) {
      await wait(signal); const data = handles.get(confirmation);
      if (!data?.active || !data.verified) return { kind: "failure", reason: "expired" };
      const failure = failOnce(["failed"]); if (failure) { data.verified = false; return failure; }
      data.active = false; registeredEmail = data.email;
      return { kind: "changed", subjectKey, registeredEmail };
    },
    release(confirmation) { const data = handles.get(confirmation); if (data) { data.active = false; data.verified = false; } },
  };
  return { account, change };
}
