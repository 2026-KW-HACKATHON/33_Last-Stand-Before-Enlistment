"use client";

import { createContext, useContext, useEffect, useRef, useState, useSyncExternalStore, type ReactNode } from "react";
import { safeReturnDestination, safeSharedHref } from "./safe-return";
import type { SessionState } from "./guard";
import type { Destination } from "./routes";
import { createNavigationStore, type NavigationEntry, type NavigationResult, type NavigationStore, type TargetAvailability, backDestination } from "./state";

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

export function NavigationProvider({ children, currentDestination, onNavigate, onIntent }: {
  children: ReactNode;
  currentDestination: Destination | null;
  onNavigate: (href: string, replace: boolean) => void;
  /** Consumer of a confirmed logical identity whose URL is still unconfirmed. */
  onIntent?: (entry: NavigationEntry) => void;
}) {
  const [store] = useState(() => createNavigationStore(currentDestination ? { destination: currentDestination } : undefined));
  const sharedLinks = useRef(new Map<string, string>());
  const initialized = useRef(false);
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  useEffect(() => { store.syncDestination(currentDestination); }, [store, currentDestination]);
  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;
    if (currentDestination?.id !== "login" && currentDestination?.id !== "signup") return;
    const values = new URLSearchParams(window.location.search).getAll("returnTo");
    const target = values.length === 1 ? safeReturnDestination(values[0]) : null;
    if (target) store.beginAuthentication({ destination: target }, currentDestination.id);
  }, [store, currentDestination]);
  function deliver<T extends NavigationResult | { status: "loading" | "error" }>(result: T, replace: boolean): T {
    if (result.status === "ready") {
      const entry = store.getState().current;
      const shared = entry?.destination.id === "sharedPost" && entry.sharedContextRef ? sharedLinks.current.get(entry.sharedContextRef) : null;
      onNavigate(shared ?? result.href, replace);
    }
    return result;
  }
  return <NavigationContext.Provider value={{
    state,
    navigate: (entry, replace = false) => { const result = store.navigate(entry, replace); if (result.status === "unresolved") onIntent?.(entry); return deliver(result, replace); },
    back: () => { const current = store.getState().current; const result = store.back(); if (result.status === "unresolved" && current) onIntent?.({ destination: backDestination(current), origin: current.origin }); return deliver(result, true); },
    beginAuthentication: (entry, step) => {
      let target = entry;
      if (entry?.destination.id === "sharedPost") {
        const href = safeSharedHref(window.location.pathname + window.location.search, entry.destination.params.postId);
        if (href) { const ref = `share-${sharedLinks.current.size + 1}`; sharedLinks.current.set(ref, href); target = { ...entry, sharedContextRef: ref }; }
      }
      return deliver(store.beginAuthentication(target, step), true);
    },
    completeAuthentication: (session, availability) => deliver(store.completeAuthentication(session, availability, onIntent), true),
    cancelAuthentication: () => deliver(store.cancelAuthentication(), true),
    registerSnapshot: store.registerSnapshot,
    removeSnapshot: store.removeSnapshot,
    clear: () => { sharedLinks.current.clear(); store.clear(); },
  }}>{children}</NavigationContext.Provider>;
}
export function useNavigation() {
  const context = useContext(NavigationContext);
  if (!context) throw new Error("NavigationProvider is required");
  return context;
}
