"use client";

import { useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { LoginScreen } from "../../../features/auth/LoginScreen";
import { StartScreen } from "../../../features/auth/StartScreen";
import { LoginProvider } from "../../../features/auth/provider";
import { createMockLoginService, initialLoginSessions, loginScenarios, type LoginScenario } from "../../../features/auth/mock";
import { NavigationProvider, SessionProvider, destinationFromPathname, useNavigation } from "../../../lib/navigation";

function Screens({ screen, shared }: { screen: string; shared: boolean }) {
  const navigation = useNavigation();
  if (screen === "/") return <StartScreen />;
  if (screen === "/login") return <LoginScreen />;
  if (screen.startsWith("/shared/")) return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page">
    <Notice>개발용 공유 진입 맥락만 표시합니다. 게시물 상세는 FE2 #60 범위입니다.</Notice>
    <Button onClick={() => navigation.beginAuthentication({ destination: { id: "sharedPost", params: { postId: "mock-post" } }, sharedContextRef: "mock-context" })}>회원 기능 로그인 유도</Button>
  </div>;
  return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page">
    <Notice role="status">Mock 이동 목적지: {screen}</Notice>
    <Notice>{screen === "/signup" ? "약관·프로필·활동 지역 화면은 #42/#43에서 연결합니다." : "실제 메인/상세 페이지는 구현하지 않습니다. 자동 참여 동작은 없습니다."}</Notice>
    <p className="text-caption">returnTo: {navigation.state.returnTo?.target.destination.id ?? "없음"} · 공유 진입: {shared ? "예" : "아니오"}</p>
  </div>;
}
function Harness({ scenario, shared, initialSession }: { scenario: LoginScenario; shared: boolean; initialSession: keyof typeof initialLoginSessions }) {
  const [service] = useState(() => createMockLoginService(scenario, 300));
  const [screen, setScreen] = useState(shared ? "/shared/posts/mock-post" : "/");
  const [session, setSession] = useState<typeof initialLoginSessions[keyof typeof initialLoginSessions]>(initialLoginSessions[initialSession]);
  return <SessionProvider session={session} retry={() => setSession(initialLoginSessions.guest)}>
    <NavigationProvider currentDestination={destinationFromPathname(screen)} onNavigate={href => setScreen(href)}>
      <LoginProvider service={service}><Screens screen={screen} shared={shared} /></LoginProvider>
    </NavigationProvider>
  </SessionProvider>;
}
export function LoginPreview() {
  const [scenario, setScenario] = useState<LoginScenario>("member");
  const [shared, setShared] = useState(false);
  const [revision, setRevision] = useState(0);
  const [initialSession, setInitialSession] = useState<keyof typeof initialLoginSessions>("guest");
  return <>
    <div className="mx-auto flex max-w-mobile flex-col gap-internal bg-surface p-page">
      <Notice>개발용 Mock 검증 전용 · 실제 이메일/코드를 입력하지 마세요.</Notice>
      <label className="text-caption">Mock 시나리오
        <select aria-label="Mock 시나리오" value={scenario} onChange={event => setScenario(event.currentTarget.value as LoginScenario)} className="w-full rounded-input border border-border bg-surface p-internal text-body">
          {Object.entries(loginScenarios).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <label className="text-caption"><input type="checkbox" checked={shared} onChange={event => setShared(event.currentTarget.checked)} /> 공유 상세에서 진입</label>
      <label className="text-caption">초기 Session
        <select aria-label="초기 Session" value={initialSession} onChange={event => setInitialSession(event.currentTarget.value as keyof typeof initialLoginSessions)} className="w-full rounded-input border border-border bg-surface p-internal text-body">
          {Object.keys(initialLoginSessions).map(value => <option key={value} value={value}>{value}</option>)}
        </select>
      </label>
      <Button variant="secondary" onClick={() => setRevision(revision + 1)}>Mock 초기화</Button>
    </div>
    <Harness key={`${scenario}-${shared}-${revision}-${initialSession}`} scenario={scenario} shared={shared} initialSession={initialSession} />
  </>;
}
