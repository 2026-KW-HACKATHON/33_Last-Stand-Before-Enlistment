"use client";

import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from "react";
import type { OtpLoginService } from "./contracts";
import { createLoginStore, type LoginStore } from "./state";

const LoginContext = createContext<{ store: LoginStore; source: OtpLoginService["source"] | null } | null>(null);

/** Stable service injection, no automatic SDK/Mock/environment selection. #61 supplies a real adapter. */
export function LoginProvider({ children, service = null }: { children: ReactNode; service?: OtpLoginService | null }) {
  const [store] = useState(() => createLoginStore(service));
  useEffect(() => () => store.dispose(), [store]);
  return <LoginContext.Provider value={{ store, source: service?.source ?? null }}>{children}</LoginContext.Provider>;
}
export function useLogin() {
  const context = useContext(LoginContext);
  if (!context) throw new Error("LoginProvider is required");
  const state = useSyncExternalStore(context.store.subscribe, context.store.getState, context.store.getState);
  return { ...context, state };
}
