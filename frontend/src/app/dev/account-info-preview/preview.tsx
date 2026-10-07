"use client";
import { useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { SessionProvider, useNavigation, type SessionState } from "../../../lib/navigation";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { accountInfoScenarios, createMockAccountInfoService, type AccountInfoScenario } from "../../../features/account-info/mock";
import type { EmailChangeRequest } from "../../../features/account-info/model";
import { MyPageScreen } from "../../../features/my-page/MyPageScreen";
import { MyPageProvider } from "../../../features/my-page/provider";
import { createMockActivityService } from "../../../features/my-page/mock";
import { ProfileProvider } from "../../../features/profile/provider";
import { createMockProfileService } from "../../../features/profile/mock";
import { InstitutionProvider } from "../../../features/institution/provider";
import { NeighborProvider } from "../../../features/neighbor/provider";
function EmailResultStub({ request, service }: { request: EmailChangeRequest; service: ReturnType<typeof createMockAccountInfoService> }) {
  return <MobileLayout header={<Header title="이메일 변경 연결 Mock" onBack={request.onCancel}/>}><Notice>#92 결과 콜백 검증용 stub입니다. #93 입력·Privy 인증·이메일 변경을 구현하지 않습니다.</Notice><Button onClick={() => { if (!request.signal.aborted) { service.simulateChangedResult(); request.onChanged(); } }}>변경 결과 Mock 수신</Button><Button variant="secondary" onClick={request.onFailure}>변경 실패 Mock 수신</Button><Button variant="secondary" onClick={request.onCancel}>취소</Button></MobileLayout>;
}
function Source({ origin }: { origin: "home" | "me" }) {
  const navigation = useNavigation();
  return origin === "me" ? <MyPageScreen/> : <MobileLayout header={<Header title="메인"/>}><Button onClick={() => navigation.navigate({ destination: { id: "settings" }, origin: { id: "home" } })}>설정으로 이동</Button></MobileLayout>;
}
function Harness({ scenario, status, subject, origin, connected }: { scenario: AccountInfoScenario; status: SessionState["status"]; subject: string; origin: "home" | "me"; connected: boolean }) {
  const [service] = useState(() => createMockAccountInfoService(scenario)); const [profile] = useState(() => createMockProfileService()); const [activity] = useState(() => createMockActivityService());
  const session: SessionState = status === "member" ? { status: "member", capabilities: { status: "ready", grants: [] } } : { status };
  return <SessionProvider session={session}><SettingsNavigation subjectKey={subject} currentDestination={{ id: origin }} onNavigate={() => {}} accountInfoService={service} renderEmailChange={connected ? request => <EmailResultStub request={request} service={service}/> : undefined}><ProfileProvider service={profile}><NeighborProvider subjectKey={subject}><InstitutionProvider subjectKey={subject}><MyPageProvider subjectKey={subject} service={activity}><Source origin={origin}/></MyPageProvider></InstitutionProvider></NeighborProvider></ProfileProvider></SettingsNavigation></SessionProvider>;
}
export function AccountInfoPreview() {
  const [scenario, setScenario] = useState<AccountInfoScenario>("normal"); const [status, setStatus] = useState<SessionState["status"]>("member"); const [subject, setSubject] = useState("dev-member-a"); const [origin, setOrigin] = useState<"home" | "me">("me"); const [connected, setConnected] = useState(true);
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#92 개발 Mock · 등록 이메일 조회와 #93 결과 콜백만 검증합니다. 실제 계정 조회·이메일 변경 완료가 아닙니다.</Notice><label>조회 상태 <select value={scenario} onChange={e => setScenario(e.currentTarget.value as AccountInfoScenario)}>{accountInfoScenarios.map(value => <option key={value}>{value}</option>)}</select></label><label>진입 <select value={origin} onChange={e => setOrigin(e.currentTarget.value as "home" | "me")}><option value="me">마이 직접 진입</option><option value="home">메인 → 설정 진입</option></select></label><label>세션 <select value={status} onChange={e => setStatus(e.currentTarget.value as SessionState["status"])}>{["member", "guest", "signup-incomplete", "loading", "error"].map(value => <option key={value}>{value}</option>)}</select></label><label>계정 <select value={subject} onChange={e => setSubject(e.currentTarget.value)}><option>dev-member-a</option><option>dev-member-b</option></select></label><label><input type="checkbox" checked={connected} onChange={e => setConnected(e.currentTarget.checked)}/>결과 stub 연결</label></div><Harness key={scenario + origin} scenario={scenario} origin={origin} status={status} subject={subject} connected={connected}/></>;
}
