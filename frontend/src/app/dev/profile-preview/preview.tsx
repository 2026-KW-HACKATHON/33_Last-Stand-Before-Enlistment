"use client";
import { useEffect, useRef, useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { NavigationProvider, SessionProvider, destinationFromPathname, useNavigation } from "../../../lib/navigation";
import { ProfileProvider } from "../../../features/profile/provider";
import { ProfileScreen } from "../../../features/profile/ProfileScreen";
import { createMockProfileService, scenarios, type Scenario } from "../../../features/profile/mock";
import { LoginProvider } from "../../../features/auth/provider";
import { SignupProvider } from "../../../features/signup/provider";
import { SignupScreen } from "../../../features/signup/SignupScreen";
import { profileSignupEditors } from "../../../features/profile/editors";
import { createMockSignupService, signupSessionFixtures, signupSharedEntry } from "../../../features/signup/mock";
function Pages({ screen, shared }: { screen: string; shared: boolean }) {
 const nav = useNavigation(); const seeded = useRef(false);
 useEffect(() => { if (!seeded.current && shared) { seeded.current = true; nav.beginAuthentication(signupSharedEntry, "signup"); } }, [nav, shared]);
 if (screen === "/signup") return <SignupScreen />;
 if (screen === "/me/profile") return <ProfileScreen />;
 if (screen === "/me/region") return <ProfileScreen region />;
 return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>Mock 원 화면 · {screen}</Notice><Button onClick={() => nav.navigate({ destination: { id: "profile" } })}>프로필 수정</Button><Button onClick={() => nav.navigate({ destination: { id: "activityRegion" } })}>기본 활동 지역</Button></div>;
}
function Harness({ scenario, access, signup, shared }: { scenario: Scenario; access: keyof typeof signupSessionFixtures; signup: boolean; shared: boolean }) {
 const [service] = useState(() => createMockProfileService(scenario)); const [signupService] = useState(() => createMockSignupService()); const [screen, setScreen] = useState(signup ? "/signup" : "/me");
 const [session, setSession] = useState(signupSessionFixtures[access]);
 return <SessionProvider session={session} retry={() => setSession(signupSessionFixtures.member)}><NavigationProvider currentDestination={destinationFromPathname(screen)} onNavigate={href => setScreen(href)}><LoginProvider><ProfileProvider service={service}><SignupProvider service={signupService} editors={profileSignupEditors}><Pages screen={screen} shared={shared} /></SignupProvider></ProfileProvider></LoginProvider></NavigationProvider></SessionProvider>;
}
export function ProfilePreview() {
 const [scenario, setScenario] = useState<Scenario>("success"); const [access, setAccess] = useState<keyof typeof signupSessionFixtures>("member"); const [signup, setSignup] = useState(false); const [shared, setShared] = useState(false);
 return <><div className="mx-auto flex max-w-mobile flex-col gap-internal bg-surface p-page"><Notice>#43 개발 Mock · 사진/지역 선택은 실제 저장·위치 인증이 아닙니다.</Notice><label>시나리오<select aria-label="시나리오" value={scenario} onChange={e => setScenario(e.currentTarget.value as Scenario)}>{scenarios.map(id => <option key={id}>{id}</option>)}</select></label><label>인증 상태<select aria-label="인증 상태" value={access} onChange={e => setAccess(e.currentTarget.value as keyof typeof signupSessionFixtures)}>{Object.keys(signupSessionFixtures).map(id => <option key={id}>{id}</option>)}</select></label><label><input type="checkbox" checked={signup} onChange={e => { setSignup(e.currentTarget.checked); setAccess(e.currentTarget.checked ? "incomplete" : "member"); }} />가입 편집기</label><label><input type="checkbox" checked={shared} onChange={e => setShared(e.currentTarget.checked)} />공유 returnTo</label></div><Harness key={[scenario,access,signup,shared].join("-")} scenario={scenario} access={access} signup={signup} shared={shared} /></>;
}
