"use client";
import { createContext, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createMyVotesStore, type MyVotesService, type MyVotesStore } from "./model";

const Context = createContext<{ store: MyVotesStore; subjectKey: string | null } | null>(null);
export const useMyVotes = () => useContext(Context);
export function MyVotesProvider({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service?: MyVotesService }) {
  const { session } = useSession();
  const scope = session.status === "member" ? subjectKey : null;
  return <Scoped key={scope ?? session.status} subjectKey={scope} service={scope ? service ?? null : null}>{children}</Scoped>;
}
function Scoped({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service: MyVotesService | null }) {
  const store = useMemo(() => createMyVotesStore(service, subjectKey), [service, subjectKey]);
  useEffect(() => () => store.dispose(), [store]);
  return <Context.Provider value={{ store, subjectKey }}>{children}</Context.Provider>;
}
export function useMyVotesState(store: MyVotesStore) { return useSyncExternalStore(store.subscribe, store.getState, store.getState); }
