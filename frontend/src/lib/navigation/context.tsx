"use client";

import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from "react";
import type { SessionState } from "./guard";
import type { Destination } from "./routes";
import { createNavigationStore, type NavigationEntry, type NavigationResult, type NavigationStore, type TargetAvailability } from "./state";

type NavigationContextValue = {
  state: ReturnType<NavigationStore["getState"]>;
  navigate: (entry: NavigationEntry, replace?: boolean) => NavigationResult;
  back: () => NavigationResult;
  beginAuthentication: (entry?: NavigationEntry, step?: "login" | "signup") => NavigationResult;
  completeAuthentication: (session: SessionState, availability: TargetAvailability) => NavigationResult | { status: "loading" | "error" };
  cancelAuthentication: () => NavigationResult;
  registerSnapshot: NavigationStore["registerSnapshot"];
  removeSnapshot: NavigationStore["removeSnapshot"];
  clear: NavigationStore["clear"];
};
const NavigationContext = createContext<NavigationContextValue | null>(null);

export function NavigationProvider({ children, currentDestination, onNavigate }: {
  children: ReactNode;
  currentDestination: Destination | null;
  onNavigate: (href: string, replace: boolean) => void;
}) {
  const [store] = useState(() => createNavigationStore(currentDestination ? { destination: currentDestination } : undefined));
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  useEffect(() => { store.syncDestination(currentDestination); }, [store, currentDestination]);
  function deliver<T extends NavigationResult | { status: "loading" | "error" }>(result: T, replace: boolean): T {
    if (result.status === "ready") onNavigate(result.href, replace);
    return result;
  }
  return <NavigationContext.Provider value={{
    state,
    navigate: (entry, replace = false) => deliver(store.navigate(entry, replace), replace),
    back: () => deliver(store.back(), true),
    beginAuthentication: (entry, step) => deliver(store.beginAuthentication(entry, step), true),
    completeAuthentication: (session, availability) => deliver(store.completeAuthentication(session, availability), true),
    cancelAuthentication: () => deliver(store.cancelAuthentication(), true),
    registerSnapshot: store.registerSnapshot,
    removeSnapshot: store.removeSnapshot,
    clear: store.clear,
  }}>{children}</NavigationContext.Provider>;
}
export function useNavigation() {
  const context = useContext(NavigationContext);
  if (!context) throw new Error("NavigationProvider is required");
  return context;
}
