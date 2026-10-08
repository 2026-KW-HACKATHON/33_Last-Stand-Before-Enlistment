"use client";
import { useState } from "react";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { SessionProvider, useSession, useNavigation, destinationFromPathname, type Destination, type SessionState } from "../../../lib/navigation";
import { WithdrawalSessionProvider } from "../../../features/withdrawal/provider";
import { createMockWithdrawalService, type WithdrawalScenario } from "../../../features/withdrawal/mock";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { LoginProvider } from "../../../features/auth/provider";
import { LoginScreen } from "../../../features/auth/LoginScreen";
function Source({ destination }: { destination: Destination }) {
  const navigation = useNavigation(); const { session } = useSession();
  if (destination.id === "login") return <LoginScreen/>;
  return <MobileLayout header={<Header title="설정 진입 Mock"/>}><Notice>현재 세션 · {session.status}</Notice><Button onClick={() => navigation.navigate({ destination: { id: "settings" }, origin: destination })}>설정으로 이동</Button></MobileLayout>;
}
function Harness({ scenario, status, subject, connected }: { scenario: WithdrawalScenario; status: SessionState["status"]; subject: string; connected: boolean }) {
  const [service] = useState(() => createMockWithdrawalService(scenario)); const [destination, setDestination] = useState<Destination>({ id: "me" });
  const session: SessionState = status === "member" ? { status, capabilities: { status: "ready", grants: [] } } : { status };
  return <SessionProvider session={session}><WithdrawalSessionProvider subjectKey={subject} service={connected ? service : null}><SettingsNavigation subjectKey={subject} currentDestination={destination} onNavigate={href => { const next = destinationFromPathname(href); if (next) setDestination(next); }}><LoginProvider subjectKey={subject}><Source destination={destination}/></LoginProvider></SettingsNavigation></WithdrawalSessionProvider></SessionProvider>;
}
export function WithdrawalPreview() {
  const [scenario, setScenario] = useState<WithdrawalScenario>("normal"), [status, setStatus] = useState<SessionState["status"]>("member"), [subject, setSubject] = useState("dev-member-a"), [connected, setConnected] = useState(true);
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#95 개발 Mock · 실제 Privy 본인 확인·탈퇴·저장·삭제가 아닙니다.</Notice><label>시나리오 <select value={scenario} onChange={e => setScenario(e.currentTarget.value as WithdrawalScenario)}>{["normal", "verification-error", "expired", "cancelled", "withdrawal-error"].map(value => <option key={value}>{value}</option>)}</select></label><label>세션 <select value={status} onChange={e => setStatus(e.currentTarget.value as SessionState["status"])}>{["member", "guest", "loading", "error", "signup-incomplete"].map(value => <option key={value}>{value}</option>)}</select></label><label>계정 <select value={subject} onChange={e => setSubject(e.currentTarget.value)}><option>dev-member-a</option><option>dev-member-b</option></select></label><label><input type="checkbox" checked={connected} onChange={e => setConnected(e.currentTarget.checked)}/>Adapter 연결</label></div><Harness key={scenario} scenario={scenario} status={status} subject={subject} connected={connected}/></>;
}
