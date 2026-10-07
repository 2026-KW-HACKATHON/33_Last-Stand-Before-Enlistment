import type { NavigationEntry } from "../../lib/navigation";
/** Client display port, not an agreed wire DTO or endpoint. */
export interface AccountInfo { registeredEmail: string | null }
export interface AccountInfoService { load(signal: AbortSignal): Promise<AccountInfo> }
export type AccountInfoState = {
  phase: "idle" | "loading" | "ready" | "empty" | "error";
  account: AccountInfo | null;
  changed: boolean;
  changePhase: "idle" | "active" | "unavailable" | "error";
  scroll: number;
};
export type EmailChangeRequest = {
  entry: NavigationEntry;
  signal: AbortSignal;
  onChanged: () => void;
  onCancel: () => void;
  onFailure: () => void;
};
export function accountEntry(incoming: NavigationEntry): NavigationEntry {
  return { destination: { id: "account" }, origin: incoming.origin?.id === "me" ? { id: "me" } : { id: "settings" } };
}
function validate(value: AccountInfo): AccountInfo {
  if (!value || (value.registeredEmail !== null && (typeof value.registeredEmail !== "string" || !value.registeredEmail.trim()))) throw new Error("Invalid account display value");
  return { registeredEmail: value.registeredEmail };
}
export function createAccountInfoStore(service: AccountInfoService | null) {
  let state: AccountInfoState = { phase: "idle", account: null, changed: false, changePhase: "idle", scroll: 0 };
  let generation = 0, changeGeneration = 0, disposed = false;
  let controller: AbortController | null = null, changeController: AbortController | null = null;
  let request: EmailChangeRequest | null = null;
  const listeners = new Set<() => void>();
  const publish = (patch: Partial<AccountInfoState>) => { state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  const cancelChange = () => { changeGeneration++; changeController?.abort(); request = null; };
  async function load(changed = false) {
    if (disposed || state.changePhase === "active") return;
    const version = ++generation; controller?.abort(); controller = new AbortController(); const signal = controller.signal;
    publish({ phase: "loading", account: null, changed });
    try {
      if (!service) throw new Error("Account adapter unavailable");
      const account = validate(await service.load(signal));
      if (!disposed && version === generation && !signal.aborted) publish({ phase: account.registeredEmail === null ? "empty" : "ready", account });
    } catch { if (!disposed && version === generation && !signal.aborted) publish({ phase: "error", account: null }); }
  }
  return {
    getState: () => state, getChangeRequest: () => request,
    subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    activate() { disposed = false; }, load,
    retry() { return load(state.changed); },
    setScroll(scroll: number) { if (!disposed && Number.isFinite(scroll) && scroll >= 0) publish({ scroll }); },
    startChange(available: boolean) {
      if (disposed || !["ready", "empty"].includes(state.phase) || state.changePhase === "active") return;
      cancelChange();
      if (!available) { publish({ changePhase: "unavailable", changed: false }); return; }
      const version = changeGeneration; changeController = new AbortController(); const signal = changeController.signal;
      const finish = (result: "changed" | "cancel" | "failure") => {
        if (disposed || signal.aborted || version !== changeGeneration) return;
        cancelChange(); publish({ changePhase: result === "failure" ? "error" : "idle", changed: false });
        if (result === "changed") void load(true);
      };
      request = { entry: { destination: { id: "emailChange" }, origin: { id: "account" } }, signal, onChanged: () => finish("changed"), onCancel: () => finish("cancel"), onFailure: () => finish("failure") };
      publish({ changePhase: "active", changed: false });
    },
    cancel() { generation++; controller?.abort(); cancelChange(); publish({ changePhase: "idle", ...(state.phase === "loading" ? { phase: "idle" as const, account: null, changed: false } : {}) }); },
    dispose() { disposed = true; generation++; controller?.abort(); cancelChange(); listeners.clear(); },
  };
}
export type AccountInfoStore = ReturnType<typeof createAccountInfoStore>;
