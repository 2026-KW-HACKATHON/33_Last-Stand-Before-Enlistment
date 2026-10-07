import type { NavigationEntry } from "../../lib/navigation";
import type { LocalLoginSession, LoginFailure, OtpLoginService } from "./contracts";

export type LoginState = {
  email: string;
  phase: "email" | "sending" | "code" | "verifying" | "resolving" | "success";
  failure: LoginFailure | null;
  providerVerified: boolean;
  resolution: { session: LocalLoginSession; availability: "available" | "unavailable" } | null;
};
export const busy = (state: LoginState) => ["sending", "verifying", "resolving"].includes(state.phase);
const initial = (): LoginState => ({ email: "", phase: "email", failure: null, providerVerified: false, resolution: null });
const failed: LoginFailure = { kind: "failure", reason: "failed", retryable: true };

/** Per-root memory only: no OTP, token, user id, local/sessionStorage or automatic retry. */
export function createLoginStore(service: OtpLoginService | null) {
  let state = initial();
  let generation = 0;
  let operation: AbortController | null = null;
  const listeners = new Set<() => void>();
  function update(next: LoginState) { state = next; listeners.forEach(listener => listener()); }
  function fail(failure: LoginFailure, phase: LoginState["phase"]) { update({ ...state, phase, failure }); }
  function start(phase: LoginState["phase"]) {
    operation?.abort();
    operation = new AbortController();
    const id = ++generation;
    update({ ...state, phase, failure: null });
    return { signal: operation.signal, current: () => id === generation && !operation?.signal.aborted };
  }
  async function resolve(target: NavigationEntry | null) {
    if (!service || !state.providerVerified || busy(state)) return;
    const request = start("resolving");
    try {
      const result = await service.resolveSession(target, request.signal);
      if (!request.current()) return;
      if (result.kind === "failure") return fail(result, "code");
      if (result.availability === "loading" || result.availability === "error") return fail(failed, "code");
      update({ ...state, phase: "success", failure: null, resolution: { session: result.session, availability: result.availability } });
    } catch { if (request.current()) fail(failed, "code"); }
  }
  return {
    getState: () => state,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    setEmail(email: string) { if (state.phase === "email") update({ ...state, email, failure: null }); },
    async send() {
      if (busy(state) || state.phase === "success" || state.providerVerified || state.failure?.retryable === false) return;
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(state.email.trim())) return fail(failed, "email");
      if (!service) return fail({ kind: "failure", reason: "unavailable", retryable: false }, "email");
      const previous = state.phase;
      const request = start("sending");
      try {
        const result = await service.sendCode(state.email.trim(), request.signal);
        if (!request.current()) return;
        if (result.kind === "failure") return fail(result, previous);
        update({ ...state, phase: "code", failure: null });
      } catch { if (request.current()) fail(failed, previous); }
    },
    async verify(code: string, target: NavigationEntry | null) {
      if (!service || state.phase !== "code" || state.providerVerified || !code.trim() || state.failure?.reason === "expired" || state.failure?.retryable === false) return;
      const request = start("verifying");
      try {
        const result = await service.verifyCode(state.email.trim(), code, request.signal);
        if (!request.current()) return;
        if (result.kind === "failure") return fail(result, "code");
        update({ ...state, phase: "code", failure: null, providerVerified: true });
        await resolve(target);
      } catch { if (request.current()) fail(failed, "code"); }
    },
    retrySession: resolve,
    changeEmail() {
      if (state.providerVerified) return;
      operation?.abort(); generation++;
      update({ ...initial(), email: state.email });
    },
    cancel() { operation?.abort(); generation++; update({ ...initial(), email: state.email, failure: { kind: "failure", reason: "cancelled", retryable: true } }); },
    dispose() { operation?.abort(); generation++; },
  };
}
export type LoginStore = ReturnType<typeof createLoginStore>;

/** Identity/guest transitions and new auth attempts own separate, cancellable results. */
export type LoginScope = { guest: boolean; subjectKey: string | null; attempt: number };
export function createLoginLifecycle(service: OtpLoginService | null) {
  let scope: LoginScope | null = null;
  let store = createLoginStore(service);
  return {
    getStore(next: LoginScope) {
      if (scope && ((next.guest && !scope.guest) || (scope.subjectKey !== null && scope.subjectKey !== next.subjectKey) || scope.attempt !== next.attempt)) {
        store.dispose();
        store = createLoginStore(service);
      }
      scope = { ...next };
      return store;
    },
    dispose() { store.dispose(); },
  };
}
