"use client";
import { useState } from "react";
import { Header, HeaderAction } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { BottomNavigation } from "../../../components/layout/BottomNavigation";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { SessionProvider, useNavigation, type SessionState } from "../../../lib/navigation";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { createMockNotificationService, notificationScenarios, type NotificationScenario } from "../../../features/notifications/mock";
import { createMockPostService } from "../../../features/post/mock";
import { PostDetail } from "../../../features/post/PostDetail";
import type { PostDisplayModel } from "../../../features/post/model";

function Entry() {
  const navigation = useNavigation();
  const open = () => navigation.navigate({ destination: { id: "notifications" }, origin: { id: "home" } });
  return <MobileLayout header={<Header title="메인" rightAction={<HeaderAction action="notification" onAction={open}/>}/>} bottomNavigation={<BottomNavigation activeItem="main" onNavigate={item => { if (item === "notification") open(); }}/> }><Button onClick={open}>알림 및 활동</Button></MobileLayout>;
}
function Harness({ scenario, role }: { scenario: NotificationScenario; role: string }) {
  const [service] = useState(() => createMockNotificationService(scenario));
  const [original] = useState(() => createMockPostService());
  const [detail, setDetail] = useState<{ post: PostDisplayModel; onReturn: () => void } | null>(null);
  const session: SessionState = role === "member" ? { status: "member", capabilities: { status: "ready", grants: [] } } : { status: role as "guest" | "signup-incomplete" | "loading" | "error" };
  return <SessionProvider session={session}><SettingsNavigation subjectKey="preview-member" notificationService={service} currentDestination={{ id: "home" }} onNavigate={() => setDetail(null)} onNotificationTarget={async (target, _entry, onReturn, signal) => {
    const post = await original.getPost(target.destination.params.postId, signal); signal.throwIfAborted();
    setDetail({ post, onReturn: () => { setDetail(null); onReturn(); } });
  }}>{detail ? <MobileLayout header={<Header title="게시물 상세" onBack={detail.onReturn}/>}><PostDetail post={detail.post}/><Button variant="secondary" onClick={detail.onReturn}>알림으로</Button></MobileLayout> : <Entry/>}</SettingsNavigation></SessionProvider>;
}
export function NotificationsPreview() {
  const [scenario, setScenario] = useState<NotificationScenario>("success"); const [role, setRole] = useState("member");
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#89 개발 Mock · 실 API 조회·읽음 저장·알림 생성 완료가 아닙니다. 기관 항목은 같은 원본의 현재 공개 채택 정보를 재사용합니다.</Notice><label>상태 <select value={scenario} onChange={event => setScenario(event.currentTarget.value as NotificationScenario)}>{notificationScenarios.map(value => <option key={value}>{value}</option>)}</select></label><label>세션 <select value={role} onChange={event => setRole(event.currentTarget.value)}>{["member", "guest", "signup-incomplete", "loading", "error"].map(value => <option key={value}>{value}</option>)}</select></label></div><Harness key={scenario + role} scenario={scenario} role={role}/></>;
}
