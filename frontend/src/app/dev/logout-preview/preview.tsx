"use client";
import { useState } from "react";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Header } from "../../../components/layout/Header";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { AccessGuard, destinationFromPathname, useNavigation, useSession, type Destination, type SessionState } from "../../../lib/navigation";
import { LogoutSessionProvider } from "../../../features/logout/provider";
import { createMockLogoutService, type LogoutScenario } from "../../../features/logout/mock";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { LoginProvider } from "../../../features/auth/provider";
import { StartScreen } from "../../../features/auth/StartScreen";
function Source({ destination }: { destination: Destination }) {
  const { session } = useSession(); const navigation = useNavigation();
  if (destination.id === "start") return <StartScreen/>;
  return <MobileLayout header={<Header title={destination.id === "me" ? "마이 진입 Mock" : destination.id === "login" ? "로그인 연결 대기" : "메인 진입 Mock"}/>}><Notice>현재 세션 · {session.status}</Notice><AccessGuard destination={{ id: "settings" }} fallback={result => <Notice>보호 화면 접근 · {result.status}</Notice>}><Button onClick={() => navigation.navigate({ destination: { id: "settings" }, origin: destination })}>설정으로 이동</Button></AccessGuard><Button variant="secondary" onClick={() => navigation.navigate({ destination: { id: "start" } }, true)}>A01 시작 화면</Button></MobileLayout>;
}
function Harness({ scenario, status, subject, origin, connected }: { scenario: LogoutScenario; status: SessionState["status"]; subject: string; origin: "home" | "me"; connected: boolean }) {
  const [service] = useState(() => createMockLogoutService(scenario)); const [destination, setDestination] = useState<Destination>({ id: origin });
  const session: SessionState = status === "member" ? { status, capabilities: { status: "ready", grants: [{ capability: { kind: "neighbor-region", regionId: "demo-region" }, allowed: true }, { capability: { kind: "institution" }, allowed: true }] } } : { status };
  return <LogoutSessionProvider subjectKey={subject} session={session} service={connected ? service : null}><SettingsNavigation subjectKey={subject} currentDestination={destination} onNavigate={href => { const next = destinationFromPathname(href); if (next) setDestination(next); }}><LoginProvider subjectKey={subject}><Source destination={destination}/></LoginProvider></SettingsNavigation></LogoutSessionProvider>;
}
export function LogoutPreview() {
  const [scenario, setScenario] = useState<LogoutScenario>("normal"); const [status, setStatus] = useState<SessionState["status"]>("member"); const [subject, setSubject] = useState("dev-member-a"); const [origin, setOrigin] = useState<"home" | "me">("home"); const [connected, setConnected] = useState(true);
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#94 개발 Mock · 실제 Privy/서버 세션 종료가 아닙니다. 실패 시 로그인 유지, 성공 시 기존 A01로 이동합니다.</Notice><label>상태 <select value={scenario} onChange={e => setScenario(e.currentTarget.value as LogoutScenario)}><option value="normal">정상</option><option value="error-retry">첫 실패 후 Retry</option></select></label><label>진입 <select value={origin} onChange={e => setOrigin(e.currentTarget.value as "home" | "me")}><option value="home">메인</option><option value="me">마이</option></select></label><label>세션 <select value={status} onChange={e => setStatus(e.currentTarget.value as SessionState["status"])}>{["member", "guest", "signup-incomplete", "loading", "error"].map(value => <option key={value}>{value}</option>)}</select></label><label>계정 <select value={subject} onChange={e => setSubject(e.currentTarget.value)}><option>dev-member-a</option><option>dev-member-b</option></select></label><label><input type="checkbox" checked={connected} onChange={e => setConnected(e.currentTarget.checked)}/>현재 기기 종료 Adapter 연결</label></div><Harness key={scenario + origin} scenario={scenario} status={status} subject={subject} origin={origin} connected={connected}/></>;
}
