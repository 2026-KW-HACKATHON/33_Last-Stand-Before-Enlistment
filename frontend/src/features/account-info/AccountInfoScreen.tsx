"use client";
import { useEffect, useRef, useSyncExternalStore, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
import { SettingRow } from "../../components/ui/SettingRow";
import { AccessGuard, useNavigation, type NavigationEntry } from "../../lib/navigation";
import type { AccountInfoState, AccountInfoStore, EmailChangeRequest } from "./model";
export type EmailChangeRenderer = (request: EmailChangeRequest) => ReactNode;
export function AccountInfoView({ state, onBack, onRetry, onChange }: { state: AccountInfoState; onBack: () => void; onRetry: () => void; onChange: () => void }) {
  return <MobileLayout header={<Header title="계정 관리" onBack={onBack}/>}>
    {state.phase === "idle" || state.phase === "loading" ? <Notice role="status">등록 이메일 불러오는 중</Notice>
      : state.phase === "error" ? <><Notice tone="error" role="alert">등록 이메일을 조회하지 못했습니다. 다시 조회해 주세요.</Notice><Button onClick={onRetry}>다시 조회</Button></>
      : <Notice role="status">{state.account?.registeredEmail === null ? "등록 이메일이 아직 설정되지 않았습니다." : "등록 이메일 · " + state.account?.registeredEmail}</Notice>}
    <SettingRow label="이메일 변경" disabled={!["ready", "empty"].includes(state.phase)} onClick={onChange}/>
    {state.changed && (state.phase === "ready" || state.phase === "empty") && <span className="sr-only" role="status">이메일 변경 결과를 다시 조회했습니다.</span>}
    {state.changePhase === "unavailable" && <Notice role="status">이메일 변경 화면 연결 대기 · #93</Notice>}
    {state.changePhase === "error" && <><Notice tone="error" role="alert">이메일 변경을 완료하지 못했습니다. 기존 등록 이메일을 유지합니다.</Notice><Button onClick={onChange}>다시 시도</Button></>}
  </MobileLayout>;
}
function Content({ store, onBack, renderEmailChange }: { store: AccountInfoStore; onBack: () => void; renderEmailChange?: EmailChangeRenderer }) {
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState); const shell = useRef<HTMLDivElement>(null);
  useEffect(() => { void store.load(); return () => store.cancel(); }, [store]);
  useEffect(() => { const main = shell.current?.querySelector("main"); if (!main || state.changePhase === "active") return; main.scrollTop = store.getState().scroll; const save = () => store.setScroll(main.scrollTop); main.addEventListener("scroll", save); return () => main.removeEventListener("scroll", save); }, [store, state.changePhase]);
  const request = store.getChangeRequest();
  return <><div hidden={state.changePhase === "active"} ref={shell}><AccountInfoView state={state} onBack={() => { store.cancel(); onBack(); }} onRetry={() => void store.retry()} onChange={() => store.startChange(!!renderEmailChange)}/></div>{state.changePhase === "active" && request && renderEmailChange?.(request)}</>;
}
export function AccountInfoScreen({ entry, ...props }: Parameters<typeof Content>[0] & { entry: NavigationEntry }) {
  const navigation = useNavigation();
  return <AccessGuard destination={{ id: "account" }} fallback={(result, retry) => <MobileLayout header={<Header title="계정 관리" onBack={props.onBack}/>}><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}{result.status === "login-required" && <Button onClick={() => { props.onBack(); navigation.beginAuthentication(entry); }}>로그인</Button>}{result.status === "signup-required" && <Button onClick={() => { props.onBack(); navigation.beginAuthentication(entry, "signup"); }}>가입 계속하기</Button>}</MobileLayout>}><Content {...props}/></AccessGuard>;
}
