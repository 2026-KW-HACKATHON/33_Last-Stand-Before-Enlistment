"use client";
import { useEffect, useSyncExternalStore } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { SettingRow } from "../../components/ui/SettingRow";
import { useNavigation } from "../../lib/navigation";
import { useLogout } from "./provider";
import { logoutAndReturn, type CurrentDeviceLogoutService, type LogoutState } from "./model";
export function LogoutActionView({ state, source, disabled, onRun, onCancel }: { state: LogoutState; source: CurrentDeviceLogoutService["source"] | null; disabled: boolean; onRun: () => void; onCancel: () => void }) {
  return <><SettingRow label="로그아웃" pending={state.phase === "pending"} disabled={disabled || state.phase === "success"} onClick={onRun}/>
    {state.phase === "pending" && <Notice role="status">현재 기기 로그아웃 중입니다.</Notice>}
    {state.phase === "error" && <Notice tone="error" role="alert"><p>로그아웃하지 못했습니다. 기존 로그인 상태를 유지합니다.</p><Button disabled={disabled} onClick={onRun}>다시 시도</Button><Button variant="secondary" disabled={disabled} onClick={onCancel}>취소</Button></Notice>}
    {state.phase === "unavailable" && <Notice role="status">현재 기기 로그아웃 연결 대기 · 로그인 상태를 유지합니다.</Notice>}
    {source === "mock" && state.phase !== "idle" && <Notice>개발 Mock · 실제 Privy/서버 세션 종료가 아닙니다.</Notice>}
  </>;
}
export function LogoutAction({ disabled, visible }: { disabled: boolean; visible: boolean }) {
  const logout = useLogout()!; const navigation = useNavigation();
  const state = useSyncExternalStore(logout.store.subscribe, logout.store.getState, logout.store.getState);
  useEffect(() => { if (!visible) logout.store.cancel(); return () => logout.store.cancel(); }, [visible, logout.store]);
  async function run() { if (disabled || !visible) return; await logoutAndReturn(logout.store, navigation); }
  return <LogoutActionView state={state} source={logout.source} disabled={disabled || !visible} onRun={() => void run()} onCancel={logout.store.cancel}/>;
}
