"use client";
import { useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { bottomNavigationDestinations, useNavigation, type NavigationEntry } from "../../lib/navigation";
import { createNotificationStore, type NotificationService, type NotificationTargetHandler } from "./model";
import { NotificationsScreen } from "./NotificationsScreen";

/** H01 logical layer persists through original detail visits. Its account-scoped parent owns entry/visibility. */
export function NotificationsHost({ children, entry, visible, service = null, onTarget, onVisible, onBack }: {
  children: ReactNode; entry: NavigationEntry | null; visible: boolean; service?: NotificationService | null;
  onTarget?: NotificationTargetHandler; onVisible: (visible: boolean) => void; onBack: () => void;
}) {
  const navigation = useNavigation();
  const { registerSnapshot, removeSnapshot } = navigation;
  const store = useMemo(() => createNotificationStore(service), [service]);
  const [targetError, setTargetError] = useState(false);
  const [handoffPending, setHandoffPending] = useState(false);
  const handoff = useRef<AbortController | null>(null);
  useLayoutEffect(() => { store.activate(); return () => { store.dispose(); handoff.current?.abort(); }; }, [store]);
  useEffect(() => { if (!entry) return; registerSnapshot({ destination: { id: "notifications" }, kind: "list", ref: "notifications-list" }); return () => removeSnapshot({ id: "notifications" }, "list"); }, [entry, registerSnapshot, removeSnapshot]);
  async function open(id?: string) {
    if (handoffPending) return;
    setTargetError(false);
    const target = await (id ? store.open(id) : store.retry()); if (!target) return;
    handoff.current?.abort(); const request = handoff.current = new AbortController();
    const targetEntry: NavigationEntry = { destination: target.destination, origin: { id: "notifications" } };
    setHandoffPending(true);
    try {
      if (onTarget) {
        let returned = false;
        await onTarget(target, targetEntry, () => { if (!request.signal.aborted) { returned = true; onVisible(true); } }, request.signal);
        if (!request.signal.aborted && !returned) onVisible(false);
      } else {
        // Public original detail already displays current adoption information; no invented K04 URL.
        const result = navigation.navigate(targetEntry);
        if (result.status !== "ready") throw new Error("Original route unavailable");
      }
    } catch { if (!request.signal.aborted) setTargetError(true); }
    finally { if (handoff.current === request) { setHandoffPending(false); } }
  }
  function cancel() { handoff.current?.abort(); handoff.current = null; setHandoffPending(false); store.returnToList(); }
  return <><div hidden={visible}>{children}</div>{entry && <div hidden={!visible}><NotificationsScreen store={store} visible={visible} onBack={() => { cancel(); onBack(); }} onCancel={cancel} handoffPending={handoffPending} onTarget={id => void open(id)} targetError={targetError} onNavigate={item => { if (item === "notification") return; cancel(); navigation.navigate({ destination: bottomNavigationDestinations[item], origin: { id: "notifications" } }); }}/></div>}</>;
}
