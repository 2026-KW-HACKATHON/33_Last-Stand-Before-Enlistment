"use client";

import { useEffect } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Notice } from "../../components/ui/Notice";
import { useLogin } from "../auth/provider";
import { useNavigation, useSession } from "../../lib/navigation";
import type { SignupAccess } from "./contracts";
import { useSignup } from "./provider";
import { requiredAgreements, signupBusy, validProfile } from "./state";

const failures = {
  failed: "가입 처리를 확인하지 못했습니다. 입력은 유지됩니다. 다시 시도해 주세요.",
  nickname: "이미 사용 중인 닉네임입니다. 닉네임을 수정해 주세요.",
  region: "활동 지역을 선택하거나 다시 확인해 주세요.",
  forbidden: "가입 권한을 확인하지 못했습니다. 인증 상태를 다시 확인해 주세요.",
  unavailable: "가입 서비스 연결을 준비 중입니다. 실제 가입은 아직 사용할 수 없습니다.",
  unknown: "가입 처리 결과를 확인할 수 없습니다. 중복 가입을 막기 위해 다시 제출하지 않습니다. 회원 상태 확인이 필요합니다.",
};

export function SignupScreen() {
  const { session, retry } = useSession();
  const login = useLogin();
  const navigation = useNavigation();
  const { store, state, source, editors, commitSession } = useSignup();
  const loginIncomplete = login.state.providerVerified && login.state.resolution?.session.status === "signup-incomplete";
  const access: SignupAccess = session.status === "member" ? "member"
    : session.status === "signup-incomplete" || loginIncomplete ? "verified-incomplete"
    : session.status === "guest" ? "unverified" : session.status;
  useEffect(() => { store.setAccess(access); }, [store, access]);
  const pending = signupBusy(state);
  const authorized = access === "verified-incomplete";
  const blocked = pending || !authorized || Boolean(state.member);
  const draft = state.draft;
  function cancel() { store.cancel(); navigation.cancelAuthentication(); }
  function back() { if (state.step === "agreements") cancel(); else store.back(); }
  const title = state.step === "agreements" ? "회원가입" : state.step === "profile" ? "프로필 설정" : "활동 지역 설정";
  return <MobileLayout header={<Header title={title} onBack={pending || state.member ? cancel : back} />}>
    {source === "mock" && <Notice>개발용 Mock · 실제 인증이나 회원 저장이 실행되지 않습니다.</Notice>}
    {access === "loading" && <Notice role="status">인증 상태 확인 중입니다.</Notice>}
    {access === "error" && <><Notice role="status" tone="error">인증 상태를 확인하지 못했습니다.</Notice>{retry && <Button onClick={() => void retry()}>인증 상태 다시 확인</Button>}</>}
    {access === "unverified" && <><Notice>이메일 인증 후 가입할 수 있습니다.</Notice><Button onClick={() => navigation.beginAuthentication()}>이메일 인증하기</Button></>}
    {access === "member" && <Notice>이미 로컬 가입을 완료한 회원입니다. 신규 가입은 필요하지 않습니다.</Notice>}
    {authorized && <>
      <Notice>이메일 인증은 확인되었습니다. 약관·프로필·활동 지역을 완료해야 가입됩니다.</Notice>
      {navigation.state.returnTo && <Notice>가입 완료 후 원래 화면으로 돌아갑니다. 참여 동작은 자동 실행되지 않습니다.</Notice>}
      {state.failure && <Notice role="status" tone="error">{failures[state.failure.reason]}</Notice>}
      {state.failure?.reason === "unknown" && retry && <Button onClick={() => void retry()}>회원 상태 다시 확인</Button>}
      {state.phase === "success" ? <>
        <Notice role="status">{source === "mock" ? "Mock 가입 흐름 확인 완료. 실제 회원 생성은 아닙니다." : "로컬 가입을 완료했습니다."}</Notice>
        {state.availability === "unavailable" && <Notice tone="warning">원래 게시물을 열 수 없어 메인으로 돌아갑니다.</Notice>}
        <Button onClick={() => { if (state.member && state.availability) { navigation.completeAuthentication(state.member, state.availability); commitSession?.(); } }}>계속</Button>
      </> : <>
        {state.step === "agreements" && <>
          {login.state.email && <Input label="인증 이메일" value={login.state.email} readOnly />}
          <h2 className="text-section">약관 동의</h2>
          <fieldset className="flex flex-col gap-section" disabled={blocked}>
            <legend className="sr-only">필수 및 선택 동의</legend>
            <label className="flex min-h-[80px] items-baseline gap-internal rounded-card border border-border bg-surface px-page py-section">
              <input type="checkbox" checked={Object.values(draft.agreements).every(Boolean)} onChange={event => store.setDraft({ ...draft, agreements: { terms: event.currentTarget.checked, privacy: event.currentTarget.checked, marketing: event.currentTarget.checked } })} />전체 동의
            </label>
            {([ ["terms", "이용약관 동의 (필수)"], ["privacy", "개인정보 수집·이용 동의 (필수)"], ["marketing", "소식·이벤트/마케팅 수신 (선택)"] ] as const).map(([key, label]) => <label key={key} className="flex min-h-[80px] items-baseline gap-internal rounded-card border border-border bg-surface px-page py-section">
              <input type="checkbox" checked={draft.agreements[key]} onChange={event => store.setDraft({ ...draft, agreements: { ...draft.agreements, [key]: event.currentTarget.checked } })} />{label}
            </label>)}
          </fieldset>
          {!requiredAgreements(draft) && <Notice>필수 이용약관과 개인정보 수집·이용 동의가 필요합니다.</Notice>}
          <Button disabled={blocked || !requiredAgreements(draft)} onClick={store.next}>동의하고 프로필 설정하기</Button>
        </>}
        {state.step === "profile" && <>
          {editors ? <editors.Profile value={draft.profile} disabled={blocked} onChange={profile => store.setDraft({ ...draft, profile })} /> : <Notice>#43 프로필 편집기 연결 대기</Notice>}
          {draft.activityRegion && <p className="rounded-card border border-border bg-surface px-page py-section">{draft.activityRegion.label}</p>}
          <Button disabled={blocked || !editors || !validProfile(draft)} onClick={store.next}>활동 지역 설정</Button>
        </>}
        {state.step === "region" && <>
          {editors ? <editors.Region value={draft.activityRegion} disabled={blocked} onChange={activityRegion => store.setDraft({ ...draft, activityRegion })} /> : <Notice>#43 활동 지역 선택기 연결 대기</Notice>}
          <Notice>활동 지역과 프로필 속성은 이웃·기관 참여 자격을 부여하지 않습니다.</Notice>
          <Button disabled={pending || !authorized || !editors || !draft.activityRegion || state.failure?.retryable === false} onClick={() => void store.submit(navigation.state.returnTo?.target ?? null)}>
            {state.phase === "submitting" ? "가입 처리 중…" : state.phase === "resolving" ? "복귀 대상 확인 중…" : state.member ? "복귀 대상 다시 확인" : "설정 완료하고 시작하기"}
          </Button>
          {!state.member && <Button variant="secondary" disabled={pending} onClick={store.reviewProfile}>프로필 다시 확인</Button>}
        </>}
      </>}
    </>}
    <Button variant="secondary" onClick={cancel}>가입 취소</Button>
  </MobileLayout>;
}
