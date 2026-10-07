"use client";

import { useEffect, useRef, useState } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Notice } from "../../components/ui/Notice";
import { useNavigation } from "../../lib/navigation";
import { useLogin } from "./provider";
import { busy, type LoginState } from "./state";

const messages = {
  failed: "로그인에 실패했습니다. 입력을 확인하고 다시 시도해 주세요.",
  expired: "인증 코드가 만료되었습니다. 코드를 다시 요청해 주세요.",
  cancelled: "로그인을 취소했습니다. 다시 시작할 수 있습니다.",
  "rate-limited": "인증 요청이 제한되었습니다. Provider가 재시도를 허용하면 다시 시도해 주세요.",
  unavailable: "로그인 서비스 연결을 준비 중입니다. 실제 인증은 아직 사용할 수 없습니다.",
};

export function LoginScreen() {
  const { state, store, source } = useLogin();
  const navigation = useNavigation();
  const [input, setInput] = useState({ store, code: "" });
  if (input.store !== store) setInput({ store, code: "" });
  const code = input.store === store ? input.code : "";
  const setCode = (value: string) => setInput({ store, code: value });
  const delivered = useRef<LoginState["resolution"]>(null);
  const pending = busy(state);
  const canRetry = !state.failure || state.failure.retryable;
  const codeStep = state.phase !== "email" && state.phase !== "sending";
  const target = navigation.state.returnTo?.target ?? null;
  useEffect(() => {
    if (state.resolution && delivered.current !== state.resolution) {
      delivered.current = state.resolution;
      navigation.completeAuthentication(state.resolution.session, state.resolution.availability);
    }
  }, [state.resolution, navigation]);
  function cancel() { setCode(""); store.cancel(); navigation.cancelAuthentication(); }
  function continueLogin() {
    if (state.resolution) { setCode(""); navigation.completeAuthentication(state.resolution.session, state.resolution.availability); }
  }
  return (
    <MobileLayout header={<Header title="로그인" onBack={cancel} />}>
      {source === "mock" && <Notice>개발용 Mock · 이메일 발송 및 실제 인증이 실행되지 않습니다.</Notice>}
      {navigation.state.returnTo && <Notice>로그인이 필요한 기능입니다. 로그인 후 원래 화면으로 돌아갑니다.</Notice>}
      {state.failure && <Notice id="login-feedback" tone={state.failure.reason === "cancelled" ? "info" : "error"} role="status">{messages[state.failure.reason]}</Notice>}
      {state.phase === "success" ? (
        <>
          <Notice role="status">{source === "mock" ? "Mock 인증 흐름 확인 완료. " : "이메일 인증을 확인했습니다. "}{state.resolution?.session.status === "signup-incomplete" ? "약관·프로필·활동 지역 가입 단계가 필요합니다." : "로컬 회원 상태를 확인했습니다."}</Notice>
          {state.resolution?.availability === "unavailable" && <Notice tone="warning">원래 게시물을 열 수 없어 메인으로 돌아갑니다.</Notice>}
          <Button onClick={continueLogin}>{state.resolution?.session.status === "signup-incomplete" ? "가입 단계로 계속" : "계속"}</Button>
        </>
      ) : (
        <form className="flex flex-col gap-section" aria-busy={pending} onSubmit={event => {
          event.preventDefault();
          if (state.providerVerified) void store.retrySession(target);
          else if (codeStep) void store.verify(code, target);
          else void store.send();
        }}>
          <Input label="등록 이메일" type="email" autoComplete="email" placeholder="이메일 주소" value={state.email} onChange={event => store.setEmail(event.currentTarget.value)} required disabled={pending || codeStep} aria-describedby={state.failure ? "login-feedback" : undefined} />
          {codeStep && !state.providerVerified && <Input label="인증 코드" placeholder="이메일로 받은 인증 코드" autoComplete="one-time-code" value={code} onChange={event => setCode(event.currentTarget.value)} disabled={pending || state.failure?.reason === "expired"} required aria-describedby={state.failure ? "login-feedback" : undefined} />}
          <Button type="submit" disabled={pending || !canRetry || (codeStep && !state.providerVerified && (!code.trim() || state.failure?.reason === "expired")) || (!codeStep && !state.email.trim())}>
            {state.phase === "sending" ? "코드 요청 중…" : state.phase === "verifying" ? "인증 확인 중…" : state.phase === "resolving" ? "회원 상태 확인 중…" : state.providerVerified ? "회원 상태 다시 확인" : codeStep ? "로그인" : "이메일 인증 코드 요청"}
          </Button>
          {codeStep && !state.providerVerified && <>
            <Button variant="secondary" disabled={pending || !canRetry} onClick={() => { setCode(""); void store.send(); }}>코드 다시 요청</Button>
            <Button variant="secondary" disabled={pending} onClick={() => { setCode(""); store.changeEmail(); }}>이메일 변경</Button>
          </>}
          {!codeStep && <Button type="submit" variant="secondary" disabled={pending || !state.email.trim()}>회원가입 · 이메일 인증부터</Button>}
        </form>
      )}
      <Button variant="secondary" onClick={cancel}>취소</Button>
    </MobileLayout>
  );
}
