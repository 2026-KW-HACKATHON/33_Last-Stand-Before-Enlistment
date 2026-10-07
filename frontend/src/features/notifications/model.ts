import { parseDestination, type Destination, type NavigationEntry } from "../../lib/navigation";

/** Display/adapter contracts only. No notification wire DTO or event-generation policy. */
export type NotificationTab = "all" | "notification" | "activity";
export type NotificationItemModel = {
  id: string; category: Exclude<NotificationTab, "all">; typeLabel: string;
  title: string; description: string; timeLabel: string; read: boolean;
};
export type NotificationTarget = {
  kind: "post" | "adoption-record";
  destination: Extract<Destination, { params: unknown }> & { id: "post" };
};
export type NotificationTargetResult = { status: "available"; target: NotificationTarget } | { status: "deleted" | "inaccessible" };
export interface NotificationService {
  readonly source: "mock" | "api";
  load(signal: AbortSignal): Promise<NotificationItemModel[]>;
  markRead(id: string, signal: AbortSignal): Promise<void>;
  resolveTarget(id: string, signal: AbortSignal): Promise<NotificationTargetResult>;
}
/** #50/#53 consume the same original, and call onReturn after leaving it. */
export type NotificationTargetHandler = (target: NotificationTarget, entry: NavigationEntry, onReturn: () => void, signal: AbortSignal) => void | Promise<void>;
export type NotificationState = {
  items: readonly NotificationItemModel[]; tab: NotificationTab; scroll: number;
  phase: "idle" | "loading" | "ready" | "load-error" | "pending" | "read-error" | "target-error" | "deleted" | "inaccessible";
  pendingId: string | null;
};
export function filteredNotifications(state: NotificationState) {
  return state.items.filter(item => state.tab === "all" || item.category === state.tab);
}
export function createNotificationStore(service: NotificationService | null) {
  let state: NotificationState = { items: [], tab: "all", scroll: 0, phase: "idle", pendingId: null };
  let controller: AbortController | null = null;
  let disposed = false;
  const listeners = new Set<() => void>();
  const update = (patch: Partial<NotificationState>) => { if (!disposed) { state = { ...state, ...patch }; listeners.forEach(listener => listener()); } };
  async function load() {
    if (disposed || state.phase === "pending") return;
    controller?.abort(); const request = controller = new AbortController(); update({ phase: "loading" });
    try {
      if (!service) throw new Error("Notification adapter unavailable");
      const items = await service.load(request.signal);
      if (!request.signal.aborted) update({ items: items.map(item => ({ ...item })), phase: "ready", pendingId: null });
    } catch { if (!request.signal.aborted) update({ phase: "load-error" }); }
  }
  async function open(id: string): Promise<NotificationTarget | null> {
    if (disposed || !["ready", "read-error", "target-error"].includes(state.phase)) return null;
    const item = state.items.find(item => item.id === id); if (!item) return null;
    controller?.abort(); const request = controller = new AbortController(); update({ phase: "pending", pendingId: id });
    let readCompleted = item.read;
    try {
      if (!service) throw new Error("Notification adapter unavailable");
      if (!item.read) {
        await service.markRead(id, request.signal);
        if (request.signal.aborted) return null;
        readCompleted = true;
        update({ items: state.items.map(value => value.id === id ? { ...value, read: true } : value) });
      }
      const result = await service.resolveTarget(id, request.signal);
      if (request.signal.aborted) return null;
      if (result.status !== "available") { update({ phase: result.status }); return null; }
      const destination = parseDestination(result.target.destination);
      if (destination?.id !== "post") throw new Error("Invalid original destination");
      update({ phase: "ready", pendingId: null });
      return { ...result.target, destination: { id: "post", params: destination.params } };
    } catch { if (!request.signal.aborted) update({ phase: readCompleted ? "target-error" : "read-error" }); return null; }
  }
  return {
    getState: () => state,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    activate() { disposed = false; if (state.phase === "loading") update({ phase: "idle" }); else if (state.phase === "pending") update({ phase: "ready", pendingId: null }); },
    load, open,
    retry: () => state.pendingId ? open(state.pendingId) : Promise.resolve(null),
    setTab(tab: NotificationTab) { if (state.phase !== "pending") update({ tab, scroll: 0 }); },
    setScroll(scroll: number) { update({ scroll }); },
    returnToList() { controller?.abort(); update({ phase: ["idle", "loading"].includes(state.phase) ? "idle" : state.phase === "load-error" ? "load-error" : "ready", pendingId: null }); },
    dispose() { disposed = true; controller?.abort(); listeners.clear(); },
  };
}
export type NotificationStore = ReturnType<typeof createNotificationStore>;
