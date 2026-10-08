"use client";
import { createContext, useContext, useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createInstitutionStore, type InstitutionService } from "./model";
const Context=createContext<ReturnType<typeof createInstitutionStore>|null>(null);
function Scoped({children,service,subjectId}:{children:ReactNode;service:InstitutionService|null;subjectId:string}) {
 const store=useMemo(()=>createInstitutionStore(service,subjectId),[service,subjectId]);
 useEffect(()=>{void store.load();const refresh=()=>store.refreshValidity();window.addEventListener("focus",refresh);document.addEventListener("visibilitychange",refresh);return()=>{window.removeEventListener("focus",refresh);document.removeEventListener("visibilitychange",refresh);store.dispose();};},[store]);
 return <Context.Provider value={store}>{children}</Context.Provider>;
}
export function InstitutionProvider({children,service=null,subjectKey}:{children:ReactNode;service?:InstitutionService|null;subjectKey:string|null}) {
 const {session}=useSession();const active=session.status==="member"&&!!subjectKey;
 return <Scoped key={active ? subjectKey : "inactive"} service={active ? service : null} subjectId={active ? subjectKey : ""}>{children}</Scoped>;
}
export function useInstitution(){const store=useContext(Context);if(!store)throw new Error("InstitutionProvider is required");const state=useSyncExternalStore(store.subscribe,store.getState,store.getState);return {state,retry:store.load};}
