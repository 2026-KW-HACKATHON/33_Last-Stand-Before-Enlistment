import type { SessionState } from "../../lib/navigation";
import type { NavigationStore } from "../../lib/navigation/state";
import { accountReturnDestinations } from "../../lib/navigation/routes";
/** Client port, not a Privy SDK Method, HTTP DTO or deletion policy. */
export interface WithdrawalService {
  source: "mock" | "provider";
  verify(subjectKey: string, signal: AbortSignal): Promise<{ kind: "verified"; confirmation: object } | WithdrawalFailure>;
  withdraw(confirmation: object, signal: AbortSignal): Promise<{ kind: "withdrawn"; subjectKey: string } | WithdrawalFailure>;
  release(confirmation: object): void;
}
export type WithdrawalFailure = { kind: "failure"; reason: "failed" | "expired" | "cancelled" | "unavailable" };
export type WithdrawalState = { phase: "closed" | "caution" | "verification" | "verifying" | "final" | "withdrawing" | "complete"; failure: WithdrawalFailure["reason"] | null; ended: boolean };
export const pending = (state: WithdrawalState) => state.phase === "verifying" || state.phase === "withdrawing";
export const withdrawalSession = (session: SessionState, state: WithdrawalState): SessionState => state.ended ? { status: "guest" } : session;
export function createWithdrawalStore(service: WithdrawalService | null, subjectKey: string | null, status: SessionState["status"]) {
  let state: WithdrawalState = { phase: "closed", failure: null, ended: false }, disposed = false, generation = 0;
  let controller: AbortController | null = null, confirmation: object | null = null;
  const listeners = new Set<() => void>();
  const publish = (patch: Partial<WithdrawalState>) => { state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  function release() { if (confirmation) service?.release(confirmation); confirmation = null; }
  function invalidate() { generation++; controller?.abort(); release(); }
  function start(phase: WithdrawalState["phase"]) { controller?.abort(); controller = new AbortController(); const signal = controller.signal, version = ++generation; publish({ phase, failure: null }); return { signal, current: () => !disposed && !signal.aborted && version === generation }; }
  const allowed = () => !disposed && !state.ended && status === "member" && !!subjectKey;
  return {
    getState: () => state,
    subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    activate() { disposed = false; },
    open() { if (allowed() && state.phase === "closed") publish({ phase: "caution", failure: null }); },
    next() { if (allowed() && state.phase === "caution") publish({ phase: "verification", failure: null }); },
    async verify() {
      if (!allowed() || state.phase !== "verification") return;
      if (!service) { publish({ failure: "unavailable" }); return; }
      release(); const run = start("verifying");
      try {
        const result = await service.verify(subjectKey!, run.signal);
        if (!run.current()) { if (result.kind === "verified") service.release(result.confirmation); return; }
        if (result.kind === "failure") { publish({ phase: "verification", failure: result.reason }); return; }
        confirmation = result.confirmation; publish({ phase: "final" });
      } catch { if (run.current()) publish({ phase: "verification", failure: "failed" }); }
    },
    async withdraw(): Promise<boolean> {
      if (!allowed() || state.phase !== "final" || !confirmation || !service) return false;
      const run = start("withdrawing");
      try {
        const result = await service.withdraw(confirmation, run.signal);
        if (!run.current()) return false;
        if (result.kind === "failure") {
          const reverify = result.reason !== "failed";
          if (reverify) release();
          publish({ phase: reverify ? "verification" : "final", failure: result.reason }); return false;
        }
        if (result.subjectKey !== subjectKey) { release(); publish({ phase: "verification", failure: "failed" }); return false; }
        release(); publish({ phase: "complete", ended: true }); return true;
      } catch { if (run.current()) publish({ phase: "final", failure: "failed" }); return false; }
    },
    back() { if (!allowed()) return; const prior = state.phase; invalidate(); publish({ phase: prior === "final" || prior === "withdrawing" ? "verification" : prior === "verification" || prior === "verifying" ? "caution" : "closed", failure: null }); },
    cancel() { if (!allowed()) return; invalidate(); publish({ phase: "closed", failure: null }); },
    finish() { if (!disposed && state.phase === "complete") publish({ phase: "closed" }); },
    dispose() { disposed = true; invalidate(); listeners.clear(); },
  };
}
export type WithdrawalStore = ReturnType<typeof createWithdrawalStore>;
export async function withdrawAndComplete(store: WithdrawalStore, navigation: Pick<NavigationStore, "clear">) {
  if (!await store.withdraw()) return false;
  navigation.clear(); return true;
}
export function finishWithdrawal(store: WithdrawalStore, navigation: Pick<NavigationStore, "navigate">) {
  if (store.getState().phase !== "complete") return;
  store.finish(); navigation.navigate({ destination: accountReturnDestinations.withdrawal }, true);
}
export function createWithdrawalLifecycle() {
  let scope: { service: WithdrawalService | null; subjectKey: string | null; status: SessionState["status"]; store: WithdrawalStore } | null = null;
  return { getStore(service: WithdrawalService | null, subjectKey: string | null, status: SessionState["status"]) {
    if (!scope || scope.subjectKey !== subjectKey || scope.status !== status || (scope.service !== service && !scope.store.getState().ended)) {
      scope?.store.dispose(); scope = { service, subjectKey, status, store: createWithdrawalStore(service, subjectKey, status) };
    }
    return scope.store;
  } };
}
