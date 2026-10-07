"use client";
import { createContext, useContext, useLayoutEffect, useMemo, useState, type ReactNode } from "react";
import { useSession } from "../../lib/navigation";
import { createKeywordStore, type InterestKeywordService } from "./model";
import { InterestKeywordsScreen } from "./InterestKeywordsScreen";
const Context = createContext<(() => void) | null>(null);
export const useOpenInterestKeywords = () => useContext(Context);
/** L01 stays mounted with its origin and scroll. L07 never invents a production URL. */
export function InterestKeywordsHost({ children, subjectKey, service = null }: { children: ReactNode; subjectKey: string | null; service?: InterestKeywordService | null }) {
  const { session } = useSession(); const scope = session.status === "member" ? subjectKey : null;
  return <Scoped key={scope ?? "inactive"} active={!!scope} service={scope ? service : null}>{children}</Scoped>;
}
function Scoped({ children, service, active }: { children: ReactNode; service: InterestKeywordService | null; active: boolean }) {
  const store = useMemo(() => createKeywordStore(service), [service]);
  const [visible, setVisible] = useState(false); const [visited, setVisited] = useState(false);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  return <Context.Provider value={active ? () => { setVisited(true); setVisible(true); } : null}><div hidden={visible}>{children}</div>{visited && <div hidden={!visible}><InterestKeywordsScreen store={store} visible={visible} onReturn={() => setVisible(false)}/></div>}</Context.Provider>;
}
