"use client";

import { useMemo, useState } from "react";
import { Notice } from "../../../components/ui/Notice";
import { Button } from "../../../components/ui/Button";
import { AuthSessionProvider } from "../../../features/auth/session-adapter";
import { createMockAuthSessionAdapter, type SessionMockMode } from "../../../features/auth/session-mock";
import { useSession } from "../../../lib/navigation";

function Status() {
  const { session, retry } = useSession();
  return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>세션: {session.status}{session.status === "member" ? ` / 권한: ${session.capabilities.status}` : ""}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}</div>;
}

export function SessionPreview() {
  const [mode, setMode] = useState<SessionMockMode>("member");
  const adapter = useMemo(() => createMockAuthSessionAdapter(mode), [mode]);
  return <><div className="mx-auto max-w-mobile p-page"><label className="text-caption">세션 Mock <select value={mode} onChange={event => setMode(event.currentTarget.value as SessionMockMode)} className="ml-2 rounded-input border border-border p-internal">{(["guest", "signup-incomplete", "member", "loading-error", "capabilities-loading", "capabilities-error"] as const).map(value => <option key={value}>{value}</option>)}</select></label></div><AuthSessionProvider adapter={adapter}><Status /></AuthSessionProvider></>;
}
