"use client";

import { useEffect, useMemo, useSyncExternalStore, type ReactNode } from "react";
import { SessionProvider, type SessionState } from "../../lib/navigation";

/** Maps an authentication provider plus local membership lookup into FE SessionState. */
export type AuthSessionAdapter = {
  readonly source: "privy" | "mock";
  resolve(signal: AbortSignal): Promise<SessionState>;
};

type Listener = () => void;

class AuthSessionStore {
  private session: SessionState = { status: "loading" };
  private listeners = new Set<Listener>();
  private request = 0;
  private controller: AbortController | null = null;

  constructor(private readonly adapter: AuthSessionAdapter | null | undefined) {}

  getSnapshot = () => this.session;
  subscribe = (listener: Listener) => { this.listeners.add(listener); return () => this.listeners.delete(listener); };

  private publish(session: SessionState) {
    this.session = session;
    this.listeners.forEach(listener => listener());
  }

  refresh = async () => {
    if (!this.adapter) return;
    this.controller?.abort();
    const controller = new AbortController();
    this.controller = controller;
    const request = ++this.request;
    this.publish({ status: "loading" });
    try {
      const session = await this.adapter.resolve(controller.signal);
      if (request === this.request && !controller.signal.aborted) this.publish(session);
    } catch {
      if (request === this.request && !controller.signal.aborted) this.publish({ status: "error" });
    }
  };

  dispose() { this.request++; this.controller?.abort(); this.controller = null; this.listeners.clear(); }
}

export function AuthSessionProvider({ children, adapter, session }: {
  children: ReactNode;
  adapter?: AuthSessionAdapter | null;
  /** Controlled injection remains available to integration tests and existing consumers. */
  session?: SessionState;
}) {
  const store = useMemo(() => new AuthSessionStore(adapter), [adapter]);
  const resolved = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);

  useEffect(() => {
    void store.refresh();
    return () => store.dispose();
  }, [store]);

  return <SessionProvider session={session ?? (adapter ? resolved : { status: "loading" })} retry={adapter ? store.refresh : undefined}>{children}</SessionProvider>;
}
