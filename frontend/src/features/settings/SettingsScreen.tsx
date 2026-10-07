"use client";
import { useEffect, useMemo, useRef, useSyncExternalStore } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { SettingRow } from "../../components/ui/SettingRow";
import { AccessGuard, snapshotReference, useNavigation } from "../../lib/navigation";
import { useOpenPushPreference } from "../notification-settings/PushPreferenceHost";
import { useOpenInterestKeywords } from "../interest-keywords/InterestKeywordsHost";
import { createSettingsMenuStore, settingsMenus, settingsMenuEntry, type SettingsMenuHandler, type SettingsMenuId } from "./model";
export function SettingsScreen({ onBack, onMenu, onReturn, visible, getScroll, onScroll }: { onBack: () => void; onMenu?: SettingsMenuHandler; onReturn: () => void; visible: boolean; getScroll: () => number; onScroll: (value: number) => void }) {
 const openPushPreference = useOpenPushPreference();
 const openKeywords = useOpenInterestKeywords(); const navigation = useNavigation(); const shell = useRef<HTMLDivElement>(null);
 const store = useMemo(() => createSettingsMenuStore(onMenu), [onMenu]);
 const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
 useEffect(() => () => store.dispose(), [store]);
 useEffect(() => { const main = shell.current?.querySelector("main"); if (!main) return; if (!visible) return; main.scrollTop = getScroll(); const record = () => onScroll(main.scrollTop); main.addEventListener("scroll", record); return () => main.removeEventListener("scroll", record); }, [onScroll, getScroll, visible]);
 function open(id: SettingsMenuId) { if (id === "notificationSettings" && openPushPreference) { openPushPreference(); return; } if (id === "interestKeywords" && openKeywords) { openKeywords(); return; } const target = settingsMenuEntry(id); const ref = target ? snapshotReference(navigation.state, target.destination, "list") : undefined; void store.open(id, settingsMenuEntry(id, ref), onReturn); }
 const selected = settingsMenus.find(menu => menu.id === state.selected);
 function back() { store.cancel(); onBack(); }
 return <div ref={shell}><MobileLayout header={<Header title="설정" onBack={back}/> }><AccessGuard destination={{ id: "settings" }} fallback={(result, retry) => <><Notice role="status">{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}{result.status === "login-required" && <Button onClick={() => { back(); navigation.beginAuthentication({ destination: { id: "settings" }, origin: navigation.state.current?.destination }); }}>로그인</Button>}{result.status === "signup-required" && <Button onClick={() => { back(); navigation.beginAuthentication({ destination: { id: "settings" }, origin: navigation.state.current?.destination }, "signup"); }}>가입 계속하기</Button>}</>}>
 {settingsMenus.map((menu, index) => <div key={menu.id} className="flex shrink-0 flex-col gap-section">{(index === 0 || settingsMenus[index - 1].section !== menu.section) && <h2 className="text-section">{menu.section}</h2>}<SettingRow label={menu.label} pending={state.phase === "pending" && state.selected === menu.id} disabled={state.phase === "pending"} onClick={() => open(menu.id)}/></div>)}
 {state.phase === "unavailable" && <Notice role="status">{selected?.label} 연결 대기 · 담당 Issue #{selected?.issue}. 기능은 제품 범위에 포함되며 대상 화면을 중복 구현하지 않습니다.</Notice>}
 {state.phase === "error" && <Notice tone="error" role="alert"><p>화면 연결에 실패했습니다. 현재 계정과 설정은 변경하지 않았습니다.</p><Button onClick={() => state.selected && open(state.selected)}>다시 시도</Button><Button variant="secondary" onClick={() => store.cancel()}>취소</Button></Notice>}
 </AccessGuard></MobileLayout></div>;
}
