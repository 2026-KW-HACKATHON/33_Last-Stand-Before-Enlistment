"use client";
import { useEffect, useState, useSyncExternalStore } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Notice } from "../../components/ui/Notice";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import { AccessGuard } from "../../lib/navigation";
import type { AccountInfoService, EmailChangeRequest } from "../account-info/model";
import type { EmailChangeFailure, EmailChangeService } from "./contracts";
import { createEmailChangeStore, pending, validEmail, type EmailChangeState } from "./model";
const messages: Record<EmailChangeFailure, string> = {
  "invalid-email": "올바른 이메일 주소를 입력해 주세요.", "same-email": "현재 이메일과 다른 주소를 입력해 주세요.",
  duplicate: "이미 사용 중인 이메일입니다. 다른 이메일을 입력해 주세요.",
  "send-failed": "이메일 확인을 시작하지 못했습니다. 다시 시도해 주세요.",
  mismatch: "이메일 확인에 실패했습니다. 다시 확인해 주세요.", expired: "이메일 확인이 만료되었습니다. 다시 시작해 주세요.",
  cancelled: "이메일 확인이 취소되었습니다. 다시 시작하거나 이전 화면으로 돌아갈 수 있습니다.",
  failed: "이메일 변경을 완료하지 못했습니다. 기존 이메일을 유지합니다. 다시 확인해 주세요.",
  unavailable: "이메일 변경을 지금 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.",
};
export function EmailChangeView({ state, source, available, onBack, onCancel, onRetry, onEmail, onBegin, onConfirm, onEdit }: {
  state: EmailChangeState; source: EmailChangeService["source"] | null; available: boolean;
  onBack: () => void; onCancel: () => void; onRetry: () => void; onEmail: (value: string) => void; onBegin: () => void; onConfirm: () => void; onEdit: () => void;
}) {
  const confirmation = ["confirmation", "verifying", "changing"].includes(state.phase);
  const busy = pending(state);
  const restart = !state.confirmationReady || !available || ["expired", "cancelled", "send-failed", "unavailable"].includes(state.failure ?? "");
  return <MobileLayout header={<Header title={confirmation ? "새 이메일 인증" : "이메일 변경"} onBack={onBack}/>}>
    {state.phase === "idle" || state.phase === "loading" ? <Notice role="status">현재 이메일 불러오는 중</Notice>
      : state.phase === "load-error" ? <><Notice tone="error" role="alert">현재 이메일을 조회하지 못했습니다.</Notice><Button onClick={onRetry}>다시 조회</Button></>
      : <>
        <Notice role="status">{confirmation ? state.renewed ? "이메일 확인을 다시 시작했습니다." : "새 이메일 확인을 진행해 주세요." : state.currentEmail === null ? "현재 등록 이메일이 설정되지 않았습니다." : "현재 이메일 · " + state.currentEmail}</Notice>
        <Notice>이메일 변경은 Privy의 본인·이메일 확인을 거쳐 진행됩니다.</Notice>
        <Input label="새 이메일" type="email" autoComplete="email" placeholder="새 이메일 주소" value={state.email} readOnly={confirmation} disabled={busy || state.phase === "success"} aria-invalid={(!!state.email.trim() && !validEmail(state.email)) || state.failure === "invalid-email" || state.failure === "duplicate"} onChange={e => onEmail(e.currentTarget.value)}/>
        {confirmation ? <>
          <Notice role="status">{state.phase === "verifying" ? "이메일 확인 중" : state.phase === "changing" ? "확인 완료 · 이메일 변경 처리 중" : "Privy 확인 단계"}</Notice>
          <Button disabled={busy || restart} onClick={onConfirm}>확인 후 이메일 변경</Button>
          <Button variant="secondary" disabled={busy || !available} onClick={onBegin}>이메일 확인 다시 시작</Button>
          <Button variant="secondary" disabled={busy} onClick={onEdit}>새 이메일 수정</Button>
        </> : <Button disabled={busy || !available || !validEmail(state.email) || state.email.trim().toLowerCase() === state.currentEmail?.toLowerCase() || state.phase === "success"} onClick={onBegin}>{state.phase === "starting" ? "확인 시작 중" : state.phase === "success" ? "이메일 변경 완료" : "이메일 확인 시작"}</Button>}
        {state.phase === "email" && state.email.trim() && !validEmail(state.email) && !state.failure && <Notice tone="error" role="alert">{messages["invalid-email"]}</Notice>}
        {state.failure && <Notice tone={state.failure === "cancelled" ? "warning" : "error"} role="alert">{messages[state.failure]}</Notice>}
        {!available && <Notice role="status">이메일 변경 연결 대기</Notice>}
      </>}
    <Button variant="secondary" onClick={onCancel}>취소</Button>
    {source === "mock" && <Notice>개발 Mock · 실제 이메일 발송·인증·저장 완료가 아닙니다.</Notice>}
  </MobileLayout>;
}
function Content({ request, account, service, subjectKey }: { request: EmailChangeRequest; account: AccountInfoService | null; service: EmailChangeService | null; subjectKey: string | null }) {
  const [store] = useState(() => createEmailChangeStore(account, service, subjectKey, request));
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  useEffect(() => { store.activate(); void store.load(); return () => store.dispose(); }, [store]);
  return <EmailChangeView state={state} source={service?.source ?? null} available={!!service && !!subjectKey} onBack={() => ["confirmation", "verifying", "changing"].includes(state.phase) ? store.edit() : store.cancel()} onCancel={store.cancel} onRetry={() => void store.load()} onEmail={store.setEmail} onBegin={() => void store.begin()} onConfirm={() => void store.confirm()} onEdit={store.edit}/>;
}
export function EmailChangeScreen(props: Parameters<typeof Content>[0]) {
  return <AccessGuard destination={{ id: "emailChange" }} fallback={(result, retry) => <MobileLayout header={<Header title="이메일 변경" onBack={props.request.onCancel}/>}><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && result.status === "error" && <Button onClick={() => void retry()}>다시 확인</Button>}<Button variant="secondary" onClick={props.request.onCancel}>계정 관리로 돌아가기</Button></MobileLayout>}><Content {...props}/></AccessGuard>;
}
