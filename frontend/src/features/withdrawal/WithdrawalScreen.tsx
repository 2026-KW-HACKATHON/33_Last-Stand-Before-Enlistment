"use client";
import { useSyncExternalStore, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { AccessGuard, useNavigation } from "../../lib/navigation";
import { pending, withdrawAndComplete, finishWithdrawal, type WithdrawalState } from "./model";
import { useWithdrawal } from "./provider";
const messages = { failed: "확인 또는 처리에 실패했습니다. 탈퇴하지 않았으며 기존 로그인 상태를 유지합니다.", expired: "본인 확인이 만료되었습니다. 다시 확인해 주세요.", cancelled: "본인 확인을 취소했습니다. 탈퇴하지 않았습니다.", unavailable: "회원 탈퇴 서비스 연결을 준비 중입니다. 기존 상태를 유지합니다." };
export function WithdrawalView({ state, onNext, onVerify, onWithdraw, onBack, onCancel, onFinish }: { state: WithdrawalState; onNext: () => void; onVerify: () => void; onWithdraw: () => void; onBack: () => void; onCancel: () => void; onFinish: () => void }) {
  const complete = state.phase === "complete", verification = state.phase === "verification" || state.phase === "verifying", final = state.phase === "final" || state.phase === "withdrawing";
  return <MobileLayout header={<Header title={complete ? "회원 이용 종료" : verification ? "본인 확인" : final ? "최종 탈퇴 확인" : "회원 탈퇴"} onBack={complete ? undefined : onBack}/>}>
    {complete ? <><Notice tone="warning" role="status">회원 탈퇴가 완료되었습니다.</Notice><p className="text-body">회원 서비스 이용이 종료되었습니다.</p><Button onClick={onFinish}>로그인 화면으로 이동</Button></> : <>
      {state.phase === "caution" && <><h2 className="text-section">탈퇴 전 확인해 주세요</h2><p className="text-body">게시물·댓글·답글은 유지되며 작성자는 ‘회원 탈퇴한 사용자’로 표시됩니다. 기존 익명 게시물은 익명 표시가 유지됩니다.</p><p className="text-body whitespace-pre-line">{"\n북마크와 개인 설정은 삭제됩니다. 지역 활동 게시물의 문의 이메일은 제거됩니다. 반응·평가·투표 집계는 유지되며 회원 식별 연결은 제거됩니다."}</p><Button onClick={onNext}>Privy 본인 확인</Button></>}
      {verification && <><Notice>Privy에서 본인 확인을 진행합니다. 확인 실패 시 탈퇴하지 않습니다.</Notice><Button disabled={pending(state)} aria-busy={state.phase === "verifying"} onClick={onVerify}>{state.phase === "verifying" ? "본인 확인 중…" : state.failure ? "본인 확인 다시 시도" : "본인 확인"}</Button></>}
      {final && <><Notice tone="warning">정말 회원 탈퇴를 진행하시겠습니까?</Notice><Button disabled={pending(state)} aria-busy={state.phase === "withdrawing"} onClick={onWithdraw}>{state.phase === "withdrawing" ? "처리 중…" : state.failure ? "탈퇴 다시 시도" : "최종 탈퇴 확인"}</Button></>}
      <Button variant="secondary" onClick={onCancel}>취소</Button>
      {state.failure && <Notice tone="error" role="alert">{messages[state.failure]}</Notice>}
      {pending(state) && <Notice role="status">{verification ? "본인 확인" : "탈퇴"} 처리 중입니다.</Notice>}
    </>}
  </MobileLayout>;
}
/** L01 remains mounted. Complete is public even after the same device becomes guest. */
export function WithdrawalHost({ children }: { children: ReactNode }) {
  const withdrawal = useWithdrawal();
  if (!withdrawal) return children;
  return <Connected>{children}</Connected>;
}
function Connected({ children }: { children: ReactNode }) {
  const { store } = useWithdrawal()!; const navigation = useNavigation();
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  const active = state.phase !== "closed";
  const view = <WithdrawalView state={state} onNext={store.next} onVerify={() => void store.verify()} onWithdraw={() => void withdrawAndComplete(store, navigation)} onBack={store.back} onCancel={store.cancel} onFinish={() => finishWithdrawal(store, navigation)}/>;
  return <><div hidden={active}>{children}</div>{active && (state.phase === "complete" ? view : <AccessGuard destination={{ id: "withdrawal" }} fallback={(result, retry) => <MobileLayout header={<Header title="회원 탈퇴" onBack={store.cancel}/>}><Notice role="status">{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}</MobileLayout>}>{view}</AccessGuard>)}</>;
}
