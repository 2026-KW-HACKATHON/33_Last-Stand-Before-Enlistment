import { accountReturnDestinations } from "../../lib/navigation/routes";
import type { NavigationStore } from "../../lib/navigation/state";
import type { SessionState } from "../../lib/navigation/guard";
/** Client port only: #74/#61 decide the actual Privy/local current-device termination order. */
export interface CurrentDeviceLogoutService {
  source: "mock" | "provider";
  endCurrentDevice(subjectKey: string, signal: AbortSignal): Promise<{ kind: "logged-out"; subjectKey: string } | { kind: "failure" }>;
}
export type LogoutState = { phase: "idle" | "pending" | "error" | "unavailable" | "success" };
/** Keep the upstream identity/Capabilities unchanged on failure; only confirmed success masks this device as guest. */
export function logoutSession(session: SessionState, state: LogoutState): SessionState {
  return state.phase === "success" ? { status: "guest" } : session;
}
export function createLogoutStore(service: CurrentDeviceLogoutService | null, subjectKey: string | null, status: SessionState["status"]) {
  let state: LogoutState = { phase: "idle" }, generation = 0, disposed = false;
  let controller: AbortController | null = null;
  const listeners = new Set<() => void>();
  const publish = (phase: LogoutState["phase"]) => { state = { phase }; listeners.forEach(fn => fn()); };
  return {
    getState: () => state,
    subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    activate() { disposed = false; },
    async run(): Promise<boolean> {
      if (disposed || status !== "member" || !subjectKey || state.phase === "pending" || state.phase === "success") return false;
      if (!service) { publish("unavailable"); return false; }
      controller?.abort(); controller = new AbortController(); const signal = controller.signal, version = ++generation;
      publish("pending");
      try {
        const result = await service.endCurrentDevice(subjectKey, signal);
        if (disposed || signal.aborted || version !== generation) return false;
        if (result.kind !== "logged-out" || result.subjectKey !== subjectKey) { publish("error"); return false; }
        publish("success"); return true;
      } catch { if (!disposed && !signal.aborted && version === generation) publish("error"); return false; }
    },
    cancel() { if (disposed || state.phase === "success") return; generation++; controller?.abort(); publish("idle"); },
    dispose() { disposed = true; generation++; controller?.abort(); listeners.clear(); },
  };
}
export type LogoutStore = ReturnType<typeof createLogoutStore>;

/** The ended identity survives adapter replacement; only an upstream identity/status transition starts a fresh scope. */
export function createLogoutLifecycle() {
  let current: { subjectKey: string | null; status: SessionState["status"]; service: CurrentDeviceLogoutService | null; store: LogoutStore } | null = null;
  return {
    getStore(service: CurrentDeviceLogoutService | null, subjectKey: string | null, status: SessionState["status"]) {
      const sameScope = current?.subjectKey === subjectKey && current?.status === status;
      if (!current || !sameScope || (current.service !== service && current.store.getState().phase !== "success")) {
        current?.store.dispose(); current = { subjectKey, status, service, store: createLogoutStore(service, subjectKey, status) };
      }
      return current.store;
    },
    dispose() { current?.store.dispose(); },
  };
}

/** Called only for the accepted current-device result; failures preserve history/returnTo/snapshots. */
export async function logoutAndReturn(store: LogoutStore, navigation: Pick<NavigationStore, "clear" | "navigate">): Promise<boolean> {
  if (!await store.run()) return false;
  navigation.clear(); navigation.navigate({ destination: accountReturnDestinations.logout }, true); return true;
}
