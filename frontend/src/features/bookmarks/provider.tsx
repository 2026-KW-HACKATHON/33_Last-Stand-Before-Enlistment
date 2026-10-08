"use client";
import { createContext, useContext, useEffect, useLayoutEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { useNavigation, useSession, type NavigationEntry } from "../../lib/navigation";
import type { BookmarkBinding } from "../post/PostDetail";
import { createBookmarksStore, type BookmarkService, type BookmarksStore } from "./model";

const Context = createContext<{ store: BookmarksStore; subjectKey: string | null } | null>(null);
export const useBookmarks = () => useContext(Context);
export function BookmarksProvider({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service?: BookmarkService }) {
  const { session } = useSession();
  const scope = session.status === "member" ? subjectKey : null;
  return <Scoped key={scope ?? session.status} subjectKey={scope} service={scope ? service ?? null : null}>{children}</Scoped>;
}
function Scoped({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service: BookmarkService | null }) {
  const store = useMemo(() => createBookmarksStore(service, subjectKey), [service, subjectKey]);
  useLayoutEffect(() => { store.activate(); return () => store.dispose(); }, [store]);
  return <Context.Provider value={{ store, subjectKey }}>{children}</Context.Provider>;
}
export function useBookmarksState(store: BookmarksStore) { return useSyncExternalStore(store.subscribe, store.getState, store.getState); }
/** FE2 can consume its existing single-button interface. Authentication never replays a mutation. */
export function useBookmarkBinding(postId: string, entry: NavigationEntry): BookmarkBinding | undefined {
  const context = useBookmarks();
  const store = context?.store; const subjectKey = context?.subjectKey;
  const { session } = useSession(); const navigation = useNavigation();
  const state = useSyncExternalStore(store?.subscribe ?? emptySubscribe, store?.getState ?? emptySnapshot, store?.getState ?? emptySnapshot); const row = state?.details[postId];
  useEffect(() => { if (subjectKey && store) void store.read(postId); }, [store, subjectKey, postId]);
  if (!store) return undefined;
  return { isBookmarked: row?.value?.isBookmarked ?? false, pending: !!subjectKey && (!row || row.phase === "loading" || row.pending), feedback: row?.feedback,
    onToggle: session.status === "guest" || session.status === "signup-incomplete" ? () => { navigation.beginAuthentication(entry, session.status === "signup-incomplete" ? "signup" : "login"); } : row?.phase === "ready" ? () => { void store.toggle(postId); } : undefined };
}
const emptySubscribe = () => () => {};
const emptySnapshot = () => null;
