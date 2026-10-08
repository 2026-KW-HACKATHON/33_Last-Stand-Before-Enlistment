"use client";
import { createContext, useContext, useLayoutEffect, useMemo, useState, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createPushPreferenceStore, type PushPreferenceService } from "./model";
import { PushPreferenceScreen } from "./PushPreferenceScreen";
const Context = createContext<(() => void) | null>(null);
export const useOpenPushPreference = () => useContext(Context);
/** L01 remains mounted with its own entry and scroll; L08 has no invented production URL. */
export function PushPreferenceHost({ children, subjectKey, service = null }: { children: ReactNode; subjectKey: string | null; service?: PushPreferenceService | null }) {
  const { session } = useSession(); const scope = session.status === "member" ? subjectKey : null;
  return <Scoped key={scope ?? "inactive"} active={!!scope} service={scope ? service : null}>{children}</Scoped>;
}
function Scoped({ children, active, service }: { children: ReactNode; active: boolean; service: PushPreferenceService | null }) {
  const store = useMemo(() => createPushPreferenceStore(service), [service]); const [visible, setVisible] = useState(false); const [visited, setVisited] = useState(false);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  return <Context.Provider value={active ? () => { store.cancel(); setVisited(true); setVisible(true); } : null}><div hidden={visible}>{children}</div>{visited && <div hidden={!visible}><PushPreferenceScreen store={store} visible={visible} onReturn={() => setVisible(false)}/></div>}</Context.Provider>;
}
