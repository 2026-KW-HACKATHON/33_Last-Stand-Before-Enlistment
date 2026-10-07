"use client";
import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from "react";
import { useLogin } from "../auth/provider";
import { useSession } from "../../lib/navigation";
import type { ProfileService } from "./contracts";
import { createProfileStore } from "./state";
const Context = createContext<{ store: ReturnType<typeof createProfileStore>; service: ProfileService | null } | null>(null);
export function ProfileProvider({ children, service = null }: { children: ReactNode; service?: ProfileService | null }) {
 const [store] = useState(() => createProfileStore(service)); const { session } = useSession(); const login = useLogin();
 const verifiedSignup = login.state.providerVerified && login.state.resolution?.session.status === "signup-incomplete";
 useEffect(() => { if (session.status === "guest" && !verifiedSignup) store.clear(); else store.setActive(session.status === "member" || session.status === "signup-incomplete" || verifiedSignup); }, [store, session.status, verifiedSignup]);
 useEffect(() => () => store.dispose(), [store]);
 return <Context.Provider value={{ store, service }}>{children}</Context.Provider>;
}
export function useProfile() { const context = useContext(Context); if (!context) throw new Error("ProfileProvider is required"); const state = useSyncExternalStore(context.store.subscribe, context.store.getState, context.store.getState); return { ...context, state }; }
