"use client";
import {createContext,useContext,useEffect,useMemo,useSyncExternalStore,type ReactNode} from "react";
import {useSession} from "../../lib/navigation";
import {createActivityStore,type ActivityService,type MyMenuHandler} from "./model";
const Context=createContext<{store:ReturnType<typeof createActivityStore>;subjectId:string;onMenu?:MyMenuHandler}|null>(null);
function Scoped({children,service,subjectId,onMenu}:{children:ReactNode;service:ActivityService|null;subjectId:string;onMenu?:MyMenuHandler}){const store=useMemo(()=>createActivityStore(service),[service]);useEffect(()=>()=>store.dispose(),[store]);return <Context.Provider value={{store,subjectId,onMenu}}>{children}</Context.Provider>;}
export function MyPageProvider({children,service=null,subjectKey,onMenu}:{children:ReactNode;service?:ActivityService|null;subjectKey:string|null;onMenu?:MyMenuHandler}){const {session}=useSession();const active=session.status==="member"&&!!subjectKey;return <Scoped key={active?subjectKey:"inactive"} subjectId={active?subjectKey:""} service={active?service:null} onMenu={onMenu}>{children}</Scoped>;}
export function useMyPage(){const context=useContext(Context);if(!context)throw new Error("MyPageProvider is required");const state=useSyncExternalStore(context.store.subscribe,context.store.getState,context.store.getState);return {...context,state};}
