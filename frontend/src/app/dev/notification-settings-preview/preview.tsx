"use client";
import { useState } from "react";
import { SessionProvider, useNavigation, type SessionState } from "../../../lib/navigation";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { PushPreferenceHost } from "../../../features/notification-settings/PushPreferenceHost";
import { createMockPushPreferenceService, pushPreferenceScenarios, type PushPreferenceScenario } from "../../../features/notification-settings/mock";
import { createMockNotificationService } from "../../../features/notifications/mock";
import { Header, HeaderAction } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
function Source() {
  const navigation = useNavigation();
  return <MobileLayout header={<Header title="메인" rightAction={<HeaderAction action="settings" onAction={() => navigation.navigate({ destination: { id: "settings" }, origin: { id: "home" } })}/>}/>}>
    <Button onClick={() => navigation.navigate({ destination: { id: "settings" }, origin: { id: "home" } })}>설정 → 알림 설정</Button>
    <Button variant="secondary" onClick={() => navigation.navigate({ destination: { id: "notifications" }, origin: { id: "home" } })}>앱 내 알림·활동 확인</Button>
  </MobileLayout>;
}
function Harness({ scenario, status }: { scenario: PushPreferenceScenario; status: SessionState["status"] }) {
  const [service] = useState(() => createMockPushPreferenceService(scenario)); const [notifications] = useState(() => createMockNotificationService());
  const session: SessionState = status === "member" ? { status: "member", capabilities: { status: "ready", grants: [] } } : { status };
  return <SessionProvider session={session}><PushPreferenceHost subjectKey="dev-push-member" service={service}><SettingsNavigation subjectKey="dev-push-member" notificationService={notifications} currentDestination={{ id: "home" }} onNavigate={() => {}}><Source/></SettingsNavigation></PushPreferenceHost></SessionProvider>;
}
export function PushPreferencePreview() {
  const [scenario, setScenario] = useState<PushPreferenceScenario>("on"); const [status, setStatus] = useState<SessionState["status"]>("member");
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#90 개발 Mock · 실제 설정 저장·OS 권한·Push 발송 완료가 아닙니다. OFF 저장 후 뒤로가기로 메인에 돌아와 앱 내 알림·활동을 확인합니다.</Notice><label>상태 <select value={scenario} onChange={event => setScenario(event.currentTarget.value as PushPreferenceScenario)}>{pushPreferenceScenarios.map(value => <option key={value}>{value}</option>)}</select></label><label>세션 <select value={status} onChange={event => setStatus(event.currentTarget.value as SessionState["status"])}>{["member", "guest", "signup-incomplete", "loading", "error"].map(value => <option key={value}>{value}</option>)}</select></label></div><Harness key={scenario + status} scenario={scenario} status={status}/></>;
}
