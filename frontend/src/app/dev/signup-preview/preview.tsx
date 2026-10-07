"use client";

import { useEffect, useRef, useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { LoginScreen } from "../../../features/auth/LoginScreen";
import { createMockLoginService } from "../../../features/auth/mock";
import { LoginProvider } from "../../../features/auth/provider";
import { SignupScreen } from "../../../features/signup/SignupScreen";
import { signupEditorStubs } from "../../../features/signup/editor-stubs";
import { createMockSignupService, signupScenarios, signupSessionFixtures, signupSharedEntry, type SignupScenario } from "../../../features/signup/mock";
import { SignupProvider } from "../../../features/signup/provider";
import { destinationFromPathname, NavigationProvider, SessionProvider, useNavigation } from "../../../lib/navigation";

function Screens({ screen, shared, onReenter }: { screen: string; shared: boolean; onReenter: () => void }) {
  const navigation = useNavigation();
  const seeded = useRef(false);
  useEffect(() => {
    if (seeded.current) return;
    seeded.current = true;
    if (shared) navigation.beginAuthentication(signupSharedEntry, "signup");
  }, [shared, navigation]);
  if (screen === "/signup") return <SignupScreen />;
  if (screen === "/login") return <LoginScreen />;
  return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page">
    <Notice role="status">Mock 목적지: {screen}</Notice>
    <Notice>실제 메인/상세 Page와 참여 동작은 구현하지 않습니다.</Notice>
    <Button onClick={() => { onReenter(); navigation.beginAuthentication(undefined, "signup"); }}>가입 화면 재진입</Button>
  </div>;
}
function Harness({ scenario, shared, access }: { scenario: SignupScenario; shared: boolean; access: keyof typeof signupSessionFixtures }) {
  const [service] = useState(() => createMockSignupService(scenario, 350));
  const [loginService] = useState(() => createMockLoginService("newcomer", 250));
  const [screen, setScreen] = useState("/signup");
  const [session, setSession] = useState<typeof signupSessionFixtures[keyof typeof signupSessionFixtures]>(signupSessionFixtures[access]);
  return <SessionProvider session={session} retry={() => setSession(signupSessionFixtures.incomplete)}>
    <NavigationProvider currentDestination={destinationFromPathname(screen)} onNavigate={href => setScreen(href)}>
      <LoginProvider service={loginService}><SignupProvider service={service} editors={signupEditorStubs}>
        <Screens screen={screen} shared={shared} onReenter={() => setScreen("/signup")} />
      </SignupProvider></LoginProvider>
    </NavigationProvider>
  </SessionProvider>;
}
export function SignupPreview() {
  const [scenario, setScenario] = useState<SignupScenario>("success");
  const [shared, setShared] = useState(false);
  const [access, setAccess] = useState<keyof typeof signupSessionFixtures>("incomplete");
  const [revision, setRevision] = useState(0);
  return <>
    <div className="mx-auto flex max-w-mobile flex-col gap-internal bg-surface p-page">
      <Notice>개발용 가입 Mock/편집 stub · 실제 이메일/인증 코드 사용 금지</Notice>
      <label className="text-caption">Mock 시나리오<select aria-label="Mock 시나리오" className="w-full rounded-input border border-border p-internal" value={scenario} onChange={event => setScenario(event.currentTarget.value as SignupScenario)}>{Object.entries(signupScenarios).map(([id, label]) => <option key={id} value={id}>{label}</option>)}</select></label>
      <label className="text-caption">인증 상태<select aria-label="인증 상태" className="w-full rounded-input border border-border p-internal" value={access} onChange={event => setAccess(event.currentTarget.value as keyof typeof signupSessionFixtures)}>{Object.keys(signupSessionFixtures).map(id => <option key={id}>{id}</option>)}</select></label>
      <label className="text-caption"><input type="checkbox" checked={shared} onChange={event => setShared(event.currentTarget.checked)} />공유 게시물 returnTo</label>
      <Button variant="secondary" onClick={() => setRevision(revision + 1)}>Mock 초기화</Button>
    </div>
    <Harness key={`${scenario}-${shared}-${access}-${revision}`} scenario={scenario} shared={shared} access={access} />
  </>;
}
