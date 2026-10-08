"use client";

import { createContext, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import type { OtpLoginService } from "./contracts";
import { createLoginLifecycle, type LoginStore } from "./state";

import { useNavigation, useSession } from "../../lib/navigation";

const LoginContext = createContext<{ store: LoginStore; source: OtpLoginService["source"] | null; commitSession?: () => void; cancelProvider?: () => void } | null>(null);

/** Stable service injection, no automatic SDK/Mock/environment selection. #61 supplies a real adapter. */
export function LoginProvider({ children, service = null, subjectKey = null }: { children: ReactNode; service?: OtpLoginService | null; subjectKey?: string | null }) {
  const { session } = useSession();
  const { state: navigation } = useNavigation();
  const lifecycle = useMemo(() => createLoginLifecycle(service), [service]);
  const store = lifecycle.getStore({ guest: session.status === "guest", subjectKey, attempt: navigation.authenticationAttempt });
  useEffect(() => () => lifecycle.dispose(), [lifecycle]);
  return <LoginContext.Provider value={{ store, source: service?.source ?? null, commitSession: service?.commitSession, cancelProvider: service?.cancel }}>{children}</LoginContext.Provider>;
}
export function useLogin() {
  const context = useContext(LoginContext);
  if (!context) throw new Error("LoginProvider is required");
  const state = useSyncExternalStore(context.store.subscribe, context.store.getState, context.store.getState);
  return { ...context, state };
}
