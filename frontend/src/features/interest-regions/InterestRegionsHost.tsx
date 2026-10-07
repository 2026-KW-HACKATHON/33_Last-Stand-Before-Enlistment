"use client";
import { createContext, useContext, useLayoutEffect, useMemo, useState, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createInterestRegionStore, type InterestRegionService } from "./model";
import { InterestRegionsScreen } from "./InterestRegionsScreen";

const Context = createContext<(() => void) | null>(null);
export const useOpenInterestRegions = () => useContext(Context);
/** I08 is a logical member destination, not a guessed production URL. Origin stays mounted. */
export function InterestRegionsHost({ children, service = null, subjectKey }: { children: ReactNode; service?: InterestRegionService | null; subjectKey: string | null }) {
  const { session } = useSession();
  const scope = session.status === "member" ? subjectKey : null;
  return <Scoped key={scope ?? "inactive"} service={scope ? service : null} active={!!scope}>{children}</Scoped>;
}
function Scoped({ children, service, active }: { children: ReactNode; service: InterestRegionService | null; active: boolean }) {
  const store = useMemo(() => createInterestRegionStore(service), [service]);
  const [opened, setOpened] = useState(false);
  const [visited, setVisited] = useState(false);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  const close = () => { store.cancel(); setOpened(false); };
  return <Context.Provider value={active ? () => { setVisited(true); setOpened(true); } : null}><div hidden={opened}>{children}</div>{visited && <div hidden={!opened}><InterestRegionsScreen store={store} visible={opened} onBack={close}/></div>}</Context.Provider>;
}
