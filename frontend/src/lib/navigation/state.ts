import type { SessionState } from "./guard";
import { type Destination, destinationKey, parseDestination, resolveDestination, routes } from "./routes";

export type NavigationEntry = {
  destination: Destination;
  origin?: Destination;
  /** Opaque, non-secret reference owned by the shared Page; never a share/access token. */
  sharedContextRef?: string;
};
export type SnapshotKind = "list" | "search" | "map" | "form";
export type SnapshotReference = { destination: Destination; kind: SnapshotKind; ref: string };
export type NavigationState = {
  authenticationAttempt: number;
  current: NavigationEntry | null;
  history: readonly NavigationEntry[];
  returnTo: { target: NavigationEntry; cancelTo: NavigationEntry } | null;
  snapshots: readonly SnapshotReference[];
};
export type NavigationResult = ReturnType<typeof resolveDestination> & { notice?: "unavailable" };
export type TargetAvailability = "available" | "unavailable" | "loading" | "error";

function cleanEntry(entry: NavigationEntry): NavigationEntry | null {
  const destination = parseDestination(entry.destination);
  if (!destination) return null;
  const origin = parseDestination(entry.origin);
  return { destination, ...(origin ? { origin } : {}),
    ...(typeof entry.sharedContextRef === "string" && entry.sharedContextRef ? { sharedContextRef: entry.sharedContextRef } : {}) };
}
export function backDestination(entry: NavigationEntry): Destination {
  if (["bookmarks", "myPosts", "participations"].includes(entry.destination.id)) {
    return entry.origin?.id === "settings" ? { id: "settings" } : { id: "me" };
  }
  if (entry.origin) return entry.origin;
  if (entry.destination.id === "editPost") return { id: "post", params: entry.destination.params };
  return { id: routes[entry.destination.id].fallback };
}
export function snapshotReference(state: NavigationState, destination: Destination, kind: SnapshotKind): string | undefined {
  return state.snapshots.find(snapshot => destinationKey(snapshot.destination) === destinationKey(destination) && snapshot.kind === kind)?.ref;
}

/** Per-provider memory only. A feature owns the values addressed by snapshot references. */
export function createNavigationStore(initialEntry?: NavigationEntry) {
  let state: NavigationState = {
    authenticationAttempt: 0, current: initialEntry ? cleanEntry(initialEntry) : null, history: [], returnTo: null, snapshots: [],
  };
  const listeners = new Set<() => void>();
  function update(next: NavigationState) {
    state = next;
    listeners.forEach(listener => listener());
  }
  function move(entry: NavigationEntry, replace = false): NavigationResult {
    const cleaned = cleanEntry(entry);
    const result = resolveDestination(cleaned?.destination);
    // Unconfirmed URLs are an explicit intent for consumers, never a fabricated navigation.
    if (!cleaned || result.status !== "ready") return result;
    const history = replace || !state.current ? state.history : [...state.history, state.current];
    const startsAuthentication = ["login", "signup"].includes(cleaned.destination.id) && !["login", "signup"].includes(state.current?.destination.id ?? "");
    update({ ...state, current: cleaned, history, authenticationAttempt: state.authenticationAttempt + Number(startsAuthentication) });
    return result;
  }
  function finish(entry: NavigationEntry, notice?: "unavailable", onIntent?: (entry: NavigationEntry) => void): NavigationResult {
    const result = resolveDestination(entry.destination);
    if (result.status === "unresolved" && onIntent) onIntent(entry);
    else if (result.status !== "ready") return result;
    update({ ...state, current: cleanEntry(entry), history: [], returnTo: null });
    return { ...result, ...(notice ? { notice } : {}) };
  }
  return {
    getState: () => state,
    subscribe(listener: () => void) {
      listeners.add(listener);
      return () => { listeners.delete(listener); };
    },
    navigate: move,
    /** Next pathname changes, including native back. Unknown Pages have no invented identity. */
    syncDestination(destination: Destination | null) {
      if (!destination) {
        if (state.current) update({ ...state, current: null });
        return;
      }
      const canonical = parseDestination(destination);
      if (!canonical) return;
      const key = destinationKey(canonical);
      if (state.current && destinationKey(state.current.destination) === key) return;
      const startsAuthentication = ["login", "signup"].includes(canonical.id) && !["login", "signup"].includes(state.current?.destination.id ?? "");
      const index = state.history.findLastIndex(entry => destinationKey(entry.destination) === key);
      if (index >= 0) update({ ...state, current: state.history[index], history: state.history.slice(0, index), authenticationAttempt: state.authenticationAttempt + Number(startsAuthentication) });
      else {
        update({ ...state, current: { destination: canonical }, history: state.current ? [...state.history, state.current] : state.history, authenticationAttempt: state.authenticationAttempt + Number(startsAuthentication) });
      }
    },
    back(): NavigationResult {
      if (!state.current) return { status: "invalid" };
      const target = backDestination(state.current);
      const result = resolveDestination(target);
      if (result.status !== "ready") return result;
      const index = state.history.findLastIndex(entry => destinationKey(entry.destination) === destinationKey(target));
      update({ ...state, current: index >= 0 ? state.history[index] : { destination: target }, history: index >= 0 ? state.history.slice(0, index) : [] });
      return result;
    },
    /** Caller can provide the protected destination before attempting an unauthenticated move. */
    beginAuthentication(entry: NavigationEntry = state.current ?? { destination: { id: "start" } }, step: "login" | "signup" = "login"): NavigationResult {
      const cleaned = cleanEntry(entry);
      if (!cleaned) return { status: "invalid" };
      if (!state.returnTo) {
        if (["login", "signup"].includes(cleaned.destination.id)) return move({ destination: { id: step } }, true);
        const target: NavigationEntry = cleaned.destination.id === "sharedPost"
          ? { ...cleaned, destination: { id: "post", params: cleaned.destination.params } } : cleaned;
        const cancelTo = cleaned.destination.id === "sharedPost" ? cleaned
          : state.current && !["login", "signup"].includes(state.current.destination.id) ? state.current
          : cleaned.origin ? { destination: cleaned.origin } : { destination: { id: "start" } as Destination };
        update({ ...state, returnTo: { target, cancelTo } });
      }
      return move({ destination: { id: step } }, true);
    },
    /** Must be called after the real adapter confirms membership and target availability. */
    completeAuthentication(session: SessionState, availability: TargetAvailability, onIntent?: (entry: NavigationEntry) => void): NavigationResult | { status: "loading" | "error" } {
      if (session.status === "loading" || availability === "loading") return { status: "loading" };
      if (session.status === "error" || availability === "error") return { status: "error" };
      if (session.status === "guest") return move({ destination: { id: "login" } }, true);
      if (session.status === "signup-incomplete") return move({ destination: { id: "signup" } }, true);
      if (availability === "unavailable") return finish({ destination: { id: "home" } }, "unavailable");
      return finish(state.returnTo?.target ?? { destination: { id: "home" } }, undefined, onIntent);
    },
    cancelAuthentication(): NavigationResult {
      return finish(state.returnTo?.cancelTo ?? { destination: { id: "start" } });
    },
    registerSnapshot(snapshot: SnapshotReference) {
      const destination = parseDestination(snapshot.destination);
      if (!destination || typeof snapshot.ref !== "string" || !snapshot.ref || !["list", "search", "map", "form"].includes(snapshot.kind)) return;
      const key = destinationKey(destination);
      const snapshots = state.snapshots.filter(item => destinationKey(item.destination) !== key || item.kind !== snapshot.kind);
      update({ ...state, snapshots: [...snapshots, { destination, kind: snapshot.kind, ref: snapshot.ref }] });
    },
    removeSnapshot(destination: Destination, kind: SnapshotKind) {
      update({ ...state, snapshots: state.snapshots.filter(item => destinationKey(item.destination) !== destinationKey(destination) || item.kind !== kind) });
    },
    /** Session adapter calls this on logout/account switch; feature stores clear their own values. */
    clear() { update({ authenticationAttempt: state.authenticationAttempt, current: null, history: [], returnTo: null, snapshots: [] }); },
  };
}
export type NavigationStore = ReturnType<typeof createNavigationStore>;
