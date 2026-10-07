"use client";

import { createContext, useContext, type ReactNode } from "react";
import { evaluateGuard, type GuardContext, type GuardResult, type SessionState } from "./guard";
import type { Destination } from "./routes";

type SessionContextValue = { session: SessionState; retry?: () => void | Promise<void> };
const SessionContext = createContext<SessionContextValue | null>(null);

/** Controlled injection boundary for #61. Production without an adapter stays loading. */
export function SessionProvider({ children, session = { status: "loading" }, retry }: {
  children: ReactNode; session?: SessionState; retry?: SessionContextValue["retry"];
}) {
  return <SessionContext.Provider value={{ session, retry }}>{children}</SessionContext.Provider>;
}
export function useSession() {
  const context = useContext(SessionContext);
  if (!context) throw new Error("SessionProvider is required");
  return context;
}
/** No automatic redirect or action replay. Consumer renders the appropriate state/Retry UI. */
export function AccessGuard({ destination, context, children, fallback }: {
  destination: Destination; context?: GuardContext; children: ReactNode;
  fallback: (result: Exclude<GuardResult, { status: "allowed" }>, retry: SessionContextValue["retry"]) => ReactNode;
}) {
  const { session, retry } = useSession();
  const result = evaluateGuard(destination, session, context);
  return result.status === "allowed" ? children : fallback(result, retry);
}
