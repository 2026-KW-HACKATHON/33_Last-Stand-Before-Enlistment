"use client";
import { useCallback, useState } from "react";
import { Header, HeaderAction } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { SessionProvider, destinationFromPathname, useNavigation, type NavigationEntry, type SessionState } from "../../../lib/navigation";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { createMockSettingsHandler, settingsScenarios, type SettingsScenario } from "../../../features/settings/mock";
import { settingsMenus, type SettingsMenuId } from "../../../features/settings/model";
type OwnerStub = { id: SettingsMenuId; entry: NavigationEntry | null; onReturn: () => void };
/** Port-only target stub: no list cards/Service/account body. Owner data stays outside L01. */
function TargetStub({ target }: { target: OwnerStub }) { const menu = settingsMenus.find(item => item.id === target.id)!; return <MobileLayout header={<Header title={menu.label + " · Owner stub"} onBack={target.onReturn}/> }><Notice>담당 Issue #{menu.issue} 화면 연결 Mock입니다. 실제 목록·계정 작업을 수행하지 않습니다.</Notice><p>origin: {target.entry?.origin?.id ?? "settings action"}</p><p>상태 참조: {target.entry?.sharedContextRef ?? "없음"}</p><Button onClick={target.onReturn}>설정으로 돌아가기</Button></MobileLayout>; }
function Source({ origin }: { origin: "home" | "me" }) { const nav = useNavigation(); return <MobileLayout header={<Header title={origin === "home" ? "메인 진입 stub" : "마이 진입 stub"} rightAction={<HeaderAction action="settings" onAction={() => nav.navigate({ destination: { id: "settings" }, origin: { id: origin } })}/>}/>}><Notice>기존 Header와 논리 Navigation을 사용하는 진입 stub입니다.</Notice><Button onClick={() => nav.navigate({ destination: { id: "settings" }, origin: { id: origin } })}>설정 열기</Button></MobileLayout>; }
function Harness({ origin, scenario, status }: { origin: "home" | "me"; scenario: SettingsScenario; status: SessionState["status"] }) {
 const [path, setPath] = useState(origin === "home" ? "/home" : "/me"); const [target, setTarget] = useState<OwnerStub | null>(null);
 const [handler] = useState(() => createMockSettingsHandler(scenario, (id, entry, onReturn) => setTarget({ id, entry, onReturn })));
 const menuHandler = useCallback<typeof handler>((id, entry, onReturn, signal) => handler(id, entry, () => { setTarget(null); onReturn(); }, signal), [handler]);
 const session: SessionState = status === "member" ? { status: "member", capabilities: { status: "ready", grants: [] } } : { status };
 return <SessionProvider session={session} retry={() => {}}><SettingsNavigation subjectKey="dev-settings-member" currentDestination={destinationFromPathname(path)} onNavigate={href => { setTarget(null); setPath(href); }} onMenu={menuHandler}>{target ? <TargetStub target={target}/> : path === "/home" || path === "/me" ? <Source origin={path === "/home" ? "home" : "me"}/> : <MobileLayout><Notice>인증 진입 {path}. 실제 Privy 연결은 별도입니다.</Notice></MobileLayout>}</SettingsNavigation></SessionProvider>;
}
export function SettingsPreview() { const [origin, setOrigin] = useState<"home" | "me">("me"); const [scenario, setScenario] = useState<SettingsScenario>("success"); const [status, setStatus] = useState<SessionState["status"]>("member"); return <><div className="mx-auto flex max-w-mobile flex-col gap-internal p-page"><Notice>#91 개발 Mock · 설정 메뉴/Owner callback만 검증</Notice><label>원 진입<select value={origin} onChange={event => setOrigin(event.currentTarget.value as typeof origin)}><option>home</option><option>me</option></select></label><label>연결 상태<select value={scenario} onChange={event => setScenario(event.currentTarget.value as SettingsScenario)}>{settingsScenarios.map(item => <option key={item}>{item}</option>)}</select></label><label>세션<select value={status} onChange={event => setStatus(event.currentTarget.value as typeof status)}>{["member", "loading", "error", "guest", "signup-incomplete"].map(item => <option key={item}>{item}</option>)}</select></label></div><Harness key={origin + scenario + status} origin={origin} scenario={scenario} status={status}/></>; }
