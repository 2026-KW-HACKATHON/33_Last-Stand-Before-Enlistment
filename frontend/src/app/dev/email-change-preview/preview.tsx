"use client";
import { useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { SessionProvider, useNavigation, type SessionState } from "../../../lib/navigation";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { EmailChangeScreen } from "../../../features/email-change/EmailChangeScreen";
import { emailChangeScenarios, createMockEmailChangeServices, type EmailChangeScenario } from "../../../features/email-change/mock";
import { MyPageScreen } from "../../../features/my-page/MyPageScreen";
import { MyPageProvider } from "../../../features/my-page/provider";
import { createMockActivityService } from "../../../features/my-page/mock";
import { ProfileProvider } from "../../../features/profile/provider";
import { createMockProfileService } from "../../../features/profile/mock";
import { InstitutionProvider } from "../../../features/institution/provider";
import { NeighborProvider } from "../../../features/neighbor/provider";
function Source({ origin }: { origin: "home" | "me" }) {
  const navigation = useNavigation();
  return origin === "me" ? <MyPageScreen/> : <MobileLayout header={<Header title="메인"/>}><Button onClick={() => navigation.navigate({ destination: { id: "settings" }, origin: { id: "home" } })}>설정으로 이동</Button></MobileLayout>;
}
function Harness({ scenario, status, subject, origin, connected }: { scenario: EmailChangeScenario; status: SessionState["status"]; subject: string; origin: "home" | "me"; connected: boolean }) {
  const [services] = useState(() => createMockEmailChangeServices(subject, scenario)); const [profile] = useState(() => createMockProfileService()); const [activity] = useState(() => createMockActivityService());
  const session: SessionState = status === "member" ? { status: "member", capabilities: { status: "ready", grants: [] } } : { status };
  return <SessionProvider session={session}><SettingsNavigation subjectKey={subject} currentDestination={{ id: origin }} onNavigate={() => {}} accountInfoService={services.account} renderEmailChange={request => <EmailChangeScreen request={request} account={services.account} service={connected ? services.change : null} subjectKey={subject}/>}><ProfileProvider service={profile}><NeighborProvider subjectKey={subject}><InstitutionProvider subjectKey={subject}><MyPageProvider subjectKey={subject} service={activity}><Source origin={origin}/></MyPageProvider></InstitutionProvider></NeighborProvider></ProfileProvider></SettingsNavigation></SessionProvider>;
}
export function EmailChangePreview() {
  const [scenario, setScenario] = useState<EmailChangeScenario>("normal"); const [status, setStatus] = useState<SessionState["status"]>("member"); const [subject, setSubject] = useState("dev-member-a"); const [origin, setOrigin] = useState<"home" | "me">("me"); const [connected, setConnected] = useState(true);
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#93 개발 Mock · 오류는 첫 시도에 발생하며 재시도할 수 있습니다. other@example.com은 다른 회원 이메일입니다. 실제 Privy/API 연동이 아닙니다.</Notice><label>상태 <select value={scenario} onChange={e => setScenario(e.currentTarget.value as EmailChangeScenario)}>{emailChangeScenarios.map(value => <option key={value}>{value}</option>)}</select></label><label>진입 <select value={origin} onChange={e => setOrigin(e.currentTarget.value as "home" | "me")}><option value="me">마이 직접 진입</option><option value="home">메인 → 설정 진입</option></select></label><label>세션 <select value={status} onChange={e => setStatus(e.currentTarget.value as SessionState["status"])}>{["member", "guest", "signup-incomplete", "loading", "error"].map(value => <option key={value}>{value}</option>)}</select></label><label>계정 <select value={subject} onChange={e => setSubject(e.currentTarget.value)}><option>dev-member-a</option><option>dev-member-b</option></select></label><label><input type="checkbox" checked={connected} onChange={e => setConnected(e.currentTarget.checked)}/>이메일 변경 Adapter 연결</label></div><Harness key={scenario + origin + subject + status + connected} scenario={scenario} origin={origin} status={status} subject={subject} connected={connected}/></>;
}
