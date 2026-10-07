"use client";
import { useEffect, useRef, useSyncExternalStore } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
import { AccessGuard } from "../../lib/navigation";
import { PushPreferenceToggle } from "./PushPreferenceToggle";
import type { PushPreferenceState, PushPreferenceStore } from "./model";

export function PushPreferenceView({ state, onChange, onRetryLoad, onRetrySave, onBack }: { state: PushPreferenceState; onChange: (value: boolean) => void; onRetryLoad: () => void; onRetrySave: () => void; onBack: () => void }) {
  const editable = ["ready", "unset", "success", "save-error"].includes(state.phase);
  return <MobileLayout header={<Header title="알림 설정" onBack={onBack}/>}>
    {["idle", "loading"].includes(state.phase) ? <Notice role="status">알림 수신 설정 불러오는 중</Notice>
      : state.phase === "load-error" ? <><Notice tone="error" role="alert">알림 수신 설정을 불러오지 못했습니다.</Notice><Button onClick={onRetryLoad}>다시 조회</Button></>
      : state.draft === null ? <><Notice>푸시 알림 수신 값이 아직 설정되지 않았습니다. 수신 여부를 선택해 주세요.</Notice><div className="flex gap-internal"><Button disabled={!editable} onClick={() => onChange(true)}>ON으로 설정</Button><Button variant="secondary" disabled={!editable} onClick={() => onChange(false)}>OFF로 설정</Button></div></>
      : <PushPreferenceToggle enabled={state.draft} disabled={!editable} pending={state.phase === "pending"} onChange={onChange}/>}
    <Notice>OFF여도 서비스 내 알림과 활동 기록은 유지됩니다.</Notice>
    <p className="text-body">ON에서는 푸시를 1회 시도하며 실패하더라도 자동으로 다시 보내지 않습니다.</p>
    {state.phase === "pending" && <><Notice role="status">설정 저장 중</Notice><Button variant="secondary" onClick={onBack}>취소</Button></>}
    {state.phase === "save-error" && <><Notice tone="error" role="alert">저장에 실패했습니다. 기존 저장값으로 복구했습니다.</Notice><Button onClick={onRetrySave}>다시 시도</Button></>}
    {state.phase === "success" && <Notice role="status">알림 수신 설정이 저장되었습니다.</Notice>}
  </MobileLayout>;
}
function Content({ store, visible, onReturn }: { store: PushPreferenceStore; visible: boolean; onReturn: () => void }) {
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState); const shell = useRef<HTMLDivElement>(null);
  useEffect(() => { if (visible && store.getState().phase === "idle") void store.load(); }, [store, visible]);
  useEffect(() => { const main = shell.current?.querySelector("main"); if (!main || !visible) return; main.scrollTop = store.getState().scroll; const record = () => store.setScroll(main.scrollTop); main.addEventListener("scroll", record); return () => main.removeEventListener("scroll", record); }, [store, visible]);
  return <div ref={shell}><PushPreferenceView state={state} onChange={value => void store.save(value)} onRetryLoad={() => void store.load()} onRetrySave={() => void store.retry()} onBack={() => { store.cancel(); onReturn(); }}/></div>;
}
export function PushPreferenceScreen(props: Parameters<typeof Content>[0]) {
  return <AccessGuard destination={{ id: "notificationSettings" }} fallback={(result, retry) => <MobileLayout header={<Header title="알림 설정" onBack={props.onReturn}/>}><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}</MobileLayout>}><Content {...props}/></AccessGuard>;
}
