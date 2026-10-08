"use client";
import { createContext, useContext, useLayoutEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { SessionProvider, useSession } from "../../lib/navigation";
import { createWithdrawalLifecycle, withdrawalSession, type WithdrawalService, type WithdrawalStore } from "./model";
const Context = createContext<{ store: WithdrawalStore; source: WithdrawalService["source"] | null } | null>(null);
export function WithdrawalSessionProvider({ children, subjectKey, service = null }: { children: ReactNode; subjectKey: string | null; service?: WithdrawalService | null }) {
  const { session, retry } = useSession(); const lifecycle = useMemo(() => createWithdrawalLifecycle(), []);
  const store = lifecycle.getStore(service, subjectKey, session.status);
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  return <SessionProvider session={withdrawalSession(session, state)} retry={retry}><Context.Provider value={{ store, source: service?.source ?? null }}>{children}</Context.Provider></SessionProvider>;
}
export function useWithdrawal() { return useContext(Context); }
