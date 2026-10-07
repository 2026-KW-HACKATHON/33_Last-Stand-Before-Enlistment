"use client";
import { useLayoutEffect, useMemo, type ReactNode } from "react";
import { useSession, type NavigationEntry } from "../../lib/navigation";
import { AccountInfoScreen, type EmailChangeRenderer } from "./AccountInfoScreen";
import { createAccountInfoStore, type AccountInfoService } from "./model";
/** Keep L01/I01 mounted. A changed session creates a fresh store and aborts old callbacks. */
export function AccountInfoHost({ children, entry, subjectKey, service, renderEmailChange, onBack }: { children: ReactNode; entry: NavigationEntry | null; subjectKey: string | null; service?: AccountInfoService; renderEmailChange?: EmailChangeRenderer; onBack: () => void }) {
  const { session } = useSession(); const scope = session.status === "member" ? subjectKey : session.status;
  return <Scoped key={scope ?? "inactive"} entry={entry} service={session.status === "member" && subjectKey ? service ?? null : null} renderEmailChange={renderEmailChange} onBack={onBack}>{children}</Scoped>;
}
function Scoped({ children, entry, service, renderEmailChange, onBack }: { children: ReactNode; entry: NavigationEntry | null; service: AccountInfoService | null; renderEmailChange?: EmailChangeRenderer; onBack: () => void }) {
  const store = useMemo(() => createAccountInfoStore(service), [service]);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  return <><div hidden={!!entry}>{children}</div>{entry && <AccountInfoScreen entry={entry} store={store} renderEmailChange={renderEmailChange} onBack={onBack}/>}</>;
}
