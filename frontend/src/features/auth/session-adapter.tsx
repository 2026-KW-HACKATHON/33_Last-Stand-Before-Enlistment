"use client";

import { useCallback, useEffect, useState, type ReactNode } from "react";
import { SessionProvider, type SessionState } from "../../lib/navigation";

/** Maps an authentication provider plus local membership lookup into FE SessionState. */
export type AuthSessionAdapter = {
  readonly source: "privy" | "mock";
  resolve(signal: AbortSignal): Promise<SessionState>;
};

export function AuthSessionProvider({ children, adapter, session }: {
  children: ReactNode;
  adapter?: AuthSessionAdapter | null;
  /** Controlled injection remains available to integration tests and existing consumers. */
  session?: SessionState;
}) {
  const [resolved, setResolved] = useState<SessionState>({ status: "loading" });
  const refresh = useCallback(async () => {
    if (!adapter) return;
    const controller = new AbortController();
    setResolved({ status: "loading" });
    try { setResolved(await adapter.resolve(controller.signal)); } catch { setResolved({ status: "error" }); }
  }, [adapter]);

  useEffect(() => { void refresh(); }, [refresh]);
  return <SessionProvider session={session ?? (adapter ? resolved : { status: "loading" })} retry={adapter ? refresh : undefined}>{children}</SessionProvider>;
}
