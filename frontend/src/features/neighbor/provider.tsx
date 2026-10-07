"use client";
import { createContext, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createNeighborStore, type NeighborService, type NeighborState } from "./model";
const Context = createContext<ReturnType<typeof createNeighborStore> | null>(null);
function ScopedProvider({ service, children }: { service: NeighborService | null; children: ReactNode }) {
 const store = useMemo(() => createNeighborStore(service), [service]);
 useEffect(() => { void store.load(); return () => store.dispose(); }, [store]);
 return <Context.Provider value={store}>{children}</Context.Provider>;
}
/** Adapter must pass a stable opaque authenticated subject key; never a role or email fixture. */
export function NeighborProvider({ subjectKey, service = null, children }: { subjectKey: string | null; service?: NeighborService | null; children: ReactNode }) {
 const { session } = useSession();
 const active = session.status === "member" && !!subjectKey;
 return <ScopedProvider key={active ? subjectKey : "inactive"} service={active ? service : null}>{children}</ScopedProvider>;
}
export function useNeighbor() {
 const store = useContext(Context); if (!store) throw new Error("NeighborProvider is required");
 const state: NeighborState = useSyncExternalStore(store.subscribe, store.getState, store.getState);
 return { state, retry: store.load };
}
