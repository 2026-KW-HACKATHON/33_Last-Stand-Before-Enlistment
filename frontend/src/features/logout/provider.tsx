"use client";
import { createContext, useContext, useLayoutEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { SessionProvider, type SessionState } from "../../lib/navigation";
import { createLogoutLifecycle, logoutSession, type CurrentDeviceLogoutService, type LogoutStore } from "./model";
const LogoutContext = createContext<{ store: LogoutStore; source: CurrentDeviceLogoutService["source"] | null } | null>(null);
/** Controlled adapter input is preserved. A confirmed logout masks only this identity on this device until upstream transitions. */
export function LogoutSessionProvider({ children, session = { status: "loading" }, retry, subjectKey = null, service = null }: {
  children: ReactNode; session?: SessionState; retry?: () => void | Promise<void>; subjectKey?: string | null; service?: CurrentDeviceLogoutService | null;
}) {
  const lifecycle = useMemo(() => createLogoutLifecycle(), []);
  const store = lifecycle.getStore(service, subjectKey, session.status);
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  return <SessionProvider session={logoutSession(session, state)} retry={retry}><LogoutContext.Provider value={{ store, source: service?.source ?? null }}>{children}</LogoutContext.Provider></SessionProvider>;
}
export function useLogout() { return useContext(LogoutContext); }
