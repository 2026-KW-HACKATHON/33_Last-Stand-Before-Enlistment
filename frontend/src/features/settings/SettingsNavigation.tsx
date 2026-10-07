"use client";
import { useCallback, useRef, useState, type ReactNode } from "react";
import { NavigationProvider, resolveDestination, useSession, type Destination, type NavigationEntry } from "../../lib/navigation";
import { SettingsScreen } from "./SettingsScreen";
import { settingsEntry, type SettingsMenuHandler } from "./model";
/** Logical L01 host: no unconfirmed URL. Keep the origin Page mounted to preserve its state. */
export function SettingsNavigation({ children, currentDestination, onNavigate, onMenu, subjectKey }: { children: ReactNode; currentDestination: Destination | null; onNavigate: (href: string, replace: boolean) => void; onMenu?: SettingsMenuHandler; subjectKey: string | null }) {
 const { session } = useSession(); const key = session.status === "member" ? subjectKey : session.status;
 return <Scoped scope={key} currentDestination={currentDestination} onNavigate={onNavigate} onMenu={onMenu}>{children}</Scoped>;
}
function Scoped({ children, currentDestination, onNavigate, onMenu, scope }: Omit<Parameters<typeof SettingsNavigation>[0], "subjectKey"> & { scope: string | null }) {
 const [entry, setEntry] = useState<NavigationEntry | null>(null); const [requestedVisible, setVisible] = useState(false); const [entryScope, setEntryScope] = useState(scope); const visible = requestedVisible && entryScope === scope; const scroll = useRef(0);
 const saveScroll = useCallback((value: number) => { scroll.current = value; }, []);
 const getScroll = useCallback(() => scroll.current, []);
 const handoff = useCallback<SettingsMenuHandler>(async (id, target, onReturn, signal) => { if (!onMenu) return; let returned = false; await onMenu(id, target, () => { returned = true; onReturn(); }, signal); if (!signal.aborted && !returned) setVisible(false); }, [onMenu]);
 function intent(next: NavigationEntry) { if (next.destination.id === "settings") { setEntryScope(scope); setEntry(old => { if (!old || next.origin?.id !== "settings") scroll.current = 0; return settingsEntry(old, next); }); setVisible(true); } }
 return <NavigationProvider currentDestination={currentDestination} onIntent={intent} onNavigate={(href, replace) => { setVisible(false); onNavigate(href, replace); }}><div hidden={visible}>{children}</div>{entry && entryScope === scope && <div hidden={!visible}><SettingsScreen key={scope} visible={visible} getScroll={getScroll} onScroll={saveScroll} onMenu={onMenu ? handoff : undefined} onReturn={() => setVisible(true)} onBack={() => { setVisible(false); if (entry.origin && entry.origin.id !== currentDestination?.id) { /* Source Page is already mounted unless the child Owner navigated. */ const origin = entry.origin; const result = resolveDestination(origin); if (result.status === "ready") onNavigate(result.href, true); } }}/></div>}</NavigationProvider>;
}
