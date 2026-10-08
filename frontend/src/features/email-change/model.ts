import type { AccountInfoService, EmailChangeRequest } from "../account-info/model";
import type { Confirmation, EmailChangeFailure, EmailChangeService } from "./contracts";
export type EmailChangeState = {
  phase: "idle" | "loading" | "load-error" | "email" | "starting" | "confirmation" | "verifying" | "changing" | "success";
  currentEmail: string | null;
  email: string;
  failure: EmailChangeFailure | null;
  renewed: boolean;
  confirmationReady: boolean;
};
export const pending = (state: EmailChangeState) => ["loading", "starting", "verifying", "changing"].includes(state.phase);
export function validEmail(email: string) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/u.test(email.trim()); }
export function createEmailChangeStore(account: AccountInfoService | null, service: EmailChangeService | null, subjectKey: string | null, request: EmailChangeRequest) {
  let state: EmailChangeState = { phase: "idle", currentEmail: null, email: "", failure: null, renewed: false, confirmationReady: false };
  let confirmation: Confirmation | null = null, operation: AbortController | null = null;
  let generation = 0, disposed = false;
  const listeners = new Set<() => void>();
  const publish = (patch: Partial<EmailChangeState>) => { state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  function release() { if (confirmation) service?.release(confirmation); confirmation = null; state = { ...state, confirmationReady: false }; }
  function invalidate() { generation++; operation?.abort(); release(); }
  function start(phase: EmailChangeState["phase"]) {
    operation?.abort(); operation = new AbortController(); const signal = operation.signal, version = ++generation;
    publish({ phase, failure: null });
    return { signal, current: () => !disposed && !request.signal.aborted && !signal.aborted && version === generation };
  }
  const failure = (reason: EmailChangeFailure, phase: "email" | "confirmation") => {
    if (["duplicate", "expired", "cancelled", "unavailable"].includes(reason)) release();
    publish({ failure: reason, phase: reason === "duplicate" ? "email" : phase });
  };
  async function load() {
    if (disposed || request.signal.aborted || pending(state) || !["idle", "load-error"].includes(state.phase)) return;
    request.signal.addEventListener("abort", dispose);
    const run = start("loading");
    try {
      if (!account || !subjectKey) throw new Error("Account adapter unavailable");
      const result = await account.load(run.signal);
      if (result.registeredEmail !== null && (typeof result.registeredEmail !== "string" || !result.registeredEmail.trim())) throw new Error("Invalid account display");
      if (run.current()) publish({ currentEmail: result.registeredEmail, phase: "email" });
    } catch { if (run.current()) publish({ phase: "load-error" }); }
  }
  async function begin() {
    if (disposed || request.signal.aborted || pending(state) || !["email", "confirmation"].includes(state.phase)) return;
    if (!validEmail(state.email)) return failure("invalid-email", "email");
    if (state.email.trim().toLowerCase() === state.currentEmail?.toLowerCase()) return failure("same-email", "email");
    if (!service || !subjectKey) return failure("unavailable", "email");
    const renewed = state.phase === "confirmation";
    release(); const run = start("starting");
    try {
      const result = await service.begin(subjectKey, state.email.trim(), run.signal);
      if (!run.current()) { if (result.kind === "pending") service.release(result.confirmation); return; }
      if (result.kind === "failure") return failure(result.reason, renewed ? "confirmation" : "email");
      confirmation = result.confirmation;
      publish({ phase: "confirmation", renewed, confirmationReady: true });
    } catch { if (run.current()) failure("send-failed", renewed ? "confirmation" : "email"); }
  }
  async function confirm() {
    if (disposed || request.signal.aborted || state.phase !== "confirmation" || !confirmation || !service || !subjectKey) return;
    const handle = confirmation, email = state.email.trim(); const run = start("verifying");
    try {
      const verified = await service.confirm(handle, run.signal);
      if (!run.current()) return;
      if (verified.kind === "failure") return failure(verified.reason, "confirmation");
      publish({ phase: "changing" });
      if (!run.current()) return;
      const changed = await service.change(handle, run.signal);
      if (!run.current()) return;
      if (changed.kind === "failure") return failure(changed.reason, "confirmation");
      if (changed.subjectKey !== subjectKey || changed.registeredEmail !== email) return failure("failed", "confirmation");
      release(); publish({ phase: "success" }); request.onChanged();
    } catch { if (run.current()) failure("failed", "confirmation"); }
  }
  const dispose = () => { disposed = true; invalidate(); };
  return {
    getState: () => state,
    subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    load, begin, confirm,
    setEmail(email: string) { if (!disposed && !request.signal.aborted && state.phase === "email") publish({ email, failure: null }); },
    edit() { if (disposed || request.signal.aborted || state.phase === "success") return; invalidate(); publish({ phase: "email", failure: null, renewed: false }); },
    cancel() { if (disposed || request.signal.aborted) return; dispose(); request.onCancel(); },
    dispose() { dispose(); request.signal.removeEventListener("abort", dispose); listeners.clear(); },
    activate() { if (!request.signal.aborted) { disposed = false; request.signal.addEventListener("abort", dispose); if (pending(state)) publish({ phase: state.phase === "loading" ? "idle" : "email", failure: null }); } },
  };
}
export type EmailChangeStore = ReturnType<typeof createEmailChangeStore>;
