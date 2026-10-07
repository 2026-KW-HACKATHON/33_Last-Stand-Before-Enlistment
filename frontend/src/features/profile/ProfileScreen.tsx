"use client";
import { useEffect, useRef } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { AccessGuard, useNavigation } from "../../lib/navigation";
import { ProfileFields, RegionCandidates } from "./editors";
import { useProfile } from "./provider";
import { validProfile } from "./state";
const failures = { failed: "처리에 실패했습니다. 다시 시도해 주세요.", nickname: "이미 사용 중인 닉네임입니다.", region: "지역을 다시 선택해 주세요.", forbidden: "저장 권한이 없습니다.", unavailable: "프로필 연결 준비 중입니다.", unknown: "저장 결과를 확인할 수 없습니다. 재조회 후 확인해 주세요." };
function Content({ region }: { region: boolean }) {
 const navigation = useNavigation(); const { store, state, service } = useProfile();
 useEffect(() => { store.setActive(true); if (store.getState().phase === "idle") void store.load(); }, [store]);
 const shell = useRef<HTMLDivElement>(null);
 const fieldsReady = !["idle", "loading", "saving", "error"].includes(state.phase);
 useEffect(() => { const main = shell.current?.querySelector("main"); if (!main || !fieldsReady) return; const page = region ? "region" : "profile"; main.scrollTop = store.getState().scroll[page]; const record = () => store.setScroll(page, main.scrollTop); main.addEventListener("scroll", record); return () => main.removeEventListener("scroll", record); }, [store, region, fieldsReady]);
 const entryOrigin = navigation.state.current?.origin?.id;
 useEffect(() => { if (region) store.setOrigin(entryOrigin === "profile" ? "profile" : "me"); }, [store, region, entryOrigin]);
 const busy = state.phase === "loading" || state.phase === "saving";
 const back = () => { if (region) store.cancelRegion(); else store.cancel(); navigation.back(); };
 return <div ref={shell}><MobileLayout header={<Header title={region ? "기본 활동 지역" : "프로필 수정"} onBack={back} />}>
 {service?.source === "mock" && <Notice>개발용 Mock · 실제 저장이나 위치 확인은 아닙니다.</Notice>}
 {state.phase === "loading" && <Notice role="status">프로필 불러오는 중</Notice>}
 {state.failure && <Notice role="status" tone="error">{failures[state.failure.reason]}</Notice>}
 {state.phase === "error" && <Button disabled={busy} onClick={() => void store.load()}>프로필 다시 조회</Button>}
 {state.phase === "empty" && <Notice>저장된 프로필이 없습니다.</Notice>}
 {state.phase === "success" && <><Notice role="status">{service?.source === "mock" ? "Mock 저장 완료" : "저장 완료"}</Notice><Button onClick={() => navigation.back()}>원 화면으로 돌아가기</Button></>}
 {!busy && state.phase !== "idle" && state.phase !== "error" && <>
 {region ? <><Notice>현재 기본 활동 지역 · {state.saved?.activityRegion?.label ?? "미설정"}</Notice><RegionCandidates value={state.draft.activityRegion} disabled={busy} onChange={activityRegion => store.edit({ ...state.draft, activityRegion })} /></> : <><ProfileFields value={state.draft} photoUrl={state.draft.photoUrl} onChange={v => store.edit({ ...state.draft, ...v })} disabled={busy} /><Button variant="secondary" onClick={() => { store.setOrigin("profile"); navigation.navigate({ destination: { id: "activityRegion" }, origin: { id: "profile" } }); }}>활동 지역 설정 · {state.draft.activityRegion?.label ?? "미설정"}</Button></>}
 <Button disabled={!validProfile(state.draft) || !state.draft.activityRegion || state.failure?.retryable === false || state.phase === "success"} onClick={() => { if (region && state.origin === "profile") navigation.back(); else void store.save(); }}>{region ? "이 지역으로 설정하기" : "저장"}</Button>
 </>}
 {state.phase === "saving" && <Notice role="status">저장 중…</Notice>}
 <Button variant="secondary" onClick={back}>취소</Button>
 </MobileLayout></div>;
}
export function ProfileScreen({ region = false }: { region?: boolean }) {
 const navigation = useNavigation();
 return <AccessGuard destination={{ id: region ? "activityRegion" : "profile" }} fallback={(result, retry) => <MobileLayout header={<Header title="프로필" onBack={() => navigation.back()} />}><Notice role="status">{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}{result.status === "login-required" && <Button onClick={() => navigation.beginAuthentication()}>로그인</Button>}{result.status === "signup-required" && <Button onClick={() => navigation.beginAuthentication(undefined, "signup")}>가입 완료하기</Button>}</MobileLayout>}><Content region={region} /></AccessGuard>;
}
