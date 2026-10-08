"use client";
import { createContext, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createPersonalListsStore, type PersonalListsService, type PersonalListsStore } from "./model";
const Context = createContext<{ store: PersonalListsStore; subjectKey: string | null } | null>(null);
export const usePersonalLists = () => useContext(Context);
export function PersonalListsProvider({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service?: PersonalListsService }) {
 const { session } = useSession(); const scope = session.status === "member" ? subjectKey : null;
 return <Scoped key={scope ?? session.status} subjectKey={scope} service={scope ? service ?? null : null}>{children}</Scoped>;
}
function Scoped({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service: PersonalListsService | null }) {
 const store = useMemo(() => createPersonalListsStore(service, subjectKey), [service, subjectKey]);
 useEffect(() => () => store.dispose(), [store]);
 return <Context.Provider value={{ store, subjectKey }}>{children}</Context.Provider>;
}
export function useListState(store: PersonalListsStore) { return useSyncExternalStore(store.subscribe, store.getState, store.getState); }
