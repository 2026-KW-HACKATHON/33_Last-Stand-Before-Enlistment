"use client";

import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from "react";
import type { SignupEditors, SignupService } from "./contracts";
import { createSignupStore, type SignupStore } from "./state";

const SignupContext = createContext<{ store: SignupStore; source: SignupService["source"] | null; editors?: SignupEditors } | null>(null);
/** Stable service and #43 editor injection. No production Mock registration. */
export function SignupProvider({ children, service = null, editors }: { children: ReactNode; service?: SignupService | null; editors?: SignupEditors }) {
  const [store] = useState(() => createSignupStore(service));
  useEffect(() => () => store.dispose(), [store]);
  return <SignupContext.Provider value={{ store, source: service?.source ?? null, editors }}>{children}</SignupContext.Provider>;
}
export function useSignup() {
  const context = useContext(SignupContext);
  if (!context) throw new Error("SignupProvider is required");
  const state = useSyncExternalStore(context.store.subscribe, context.store.getState, context.store.getState);
  return { ...context, state };
}
