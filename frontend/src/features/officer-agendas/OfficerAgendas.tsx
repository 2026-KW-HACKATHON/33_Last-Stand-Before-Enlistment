"use client";
import { createContext, useContext, useEffect, useLayoutEffect, useMemo, useRef, useSyncExternalStore, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useNavigation, useSession, type Destination } from "../../lib/navigation";
import { institutionAccess } from "../institution/model";
import { useInstitution } from "../institution/provider";
import { PostCard } from "../post/PostCard";
import { PostDetail } from "../post/PostDetail";
import { canChange, canRead, createOfficerStore, ownRelation, publicPost, visibleAgendas, type AdoptionService, type OfficerAuthority, type OfficerStore } from "./model";

const Context = createContext<{ store: OfficerStore; authority: OfficerAuthority } | null>(null);
export function OfficerAgendasProvider({ children, subjectKey, service }: { children: ReactNode; subjectKey: string | null; service?: AdoptionService }) {
  const { session } = useSession(); const { state: institution } = useInstitution();
  const subjectId = session.status === "member" ? subjectKey ?? "" : "";
  return <Scoped key={subjectId || session.status} authority={{ subjectId, session, institution }} service={subjectId ? service ?? null : null}>{children}</Scoped>;
}
function Scoped({ children, authority, service }: { children: ReactNode; authority: OfficerAuthority; service: AdoptionService | null }) {
  const store = useMemo(() => createOfficerStore(service), [service]);
  // Session, qualification and region changes invalidate pending reads/mutations.
  const identity = JSON.stringify([authority.subjectId, authority.session, authority.institution.status === "ready" ? authority.institution.qualification : authority.institution.status, canRead(authority)]);
  useLayoutEffect(() => { store.setAuthority(authority); }, [store, authority]);
  useLayoutEffect(() => { store.activate(); store.clear(); return () => store.dispose(); }, [store, identity]);
  return <Context.Provider value={{ store, authority }}>{children}</Context.Provider>;
}
/** FE2 may project this store's publicPost/publicAdoptions without exposing private relation IDs. */
export function useOfficerAgendas() { return useContext(Context); }
export function useOfficerState(store: OfficerStore) { return useSyncExternalStore(store.subscribe, store.getState, store.getState); }

export function OfficerAgendasScreen({ store, authority, postId, onOpen, onBack }: { store: OfficerStore; authority: OfficerAuthority; postId?: string; onOpen: (id: string) => void; onBack: () => void }) {
  const state = useOfficerState(store); const shell = useRef<HTMLDivElement>(null);
  const { retry } = useInstitution(); const { retry: retrySession } = useSession(); const navigation = useNavigation();
  const access = institutionAccess(authority.institution, authority.subjectId, authority.session);
  useLayoutEffect(() => {
    const main = shell.current?.querySelector("main"); if (!main || postId) return;
    main.scrollTop = store.getState().scroll;
    const save = () => store.saveScroll(main.scrollTop);
    main.addEventListener("scroll", save); return () => main.removeEventListener("scroll", save);
  }, [store, postId, state.scope, state.regionId, state.phase]);
  const layout = (children: ReactNode, title = "기관 담당자") => <MobileLayout header={<Header title={title} onBack={onBack}/>}>{children}</MobileLayout>;
  if (access.status !== "allowed") return layout(<><Notice>{access.status === "loading" ? "기관 자격 확인 중" : access.status === "error" ? "기관 자격을 확인하지 못했습니다." : "현재 유효한 기관 인증이 필요합니다."}</Notice>{access.status === "error" && <Button onClick={() => { void retry(); void retrySession?.(); }}>다시 확인</Button>}{(access.status === "login-required" || access.status === "signup-required") && <Button onClick={() => navigation.beginAuthentication({ destination: postId ? { id: "officerAgenda", params: { postId } } : { id: "officerAgendas" }, origin: { id: "me" } }, access.status === "signup-required" ? "signup" : "login")}>로그인 / 가입</Button>}</>);
  if (postId) {
    const detail = state.detail;
    const title = state.record ? "지역 안건 · 채택 기록" : "지역 안건 검토";
    if (state.detailPhase !== "ready" || !detail || detail.post.id !== postId) return layout(<><Notice role="status">{state.detailPhase === "error" ? "안건을 불러오지 못했습니다." : state.detailPhase === "unavailable" ? "삭제되었거나 볼 수 없는 안건입니다. Service 미연결 시 실 API 연동 대기입니다." : "안건 불러오는 중"}</Notice>{state.detailPhase === "error" && <Button onClick={() => void store.read(postId)}>다시 시도</Button>}</>, title);
    const own = ownRelation(detail, authority); const allowed = canChange(detail, authority);
    return layout(<>
      {state.record ? <><h1 className="text-section">{detail.post.title}</h1>{detail.relations.map(relation => <div key={relation.id} className="rounded-card border border-border bg-surface px-page py-section text-body">{relation.institutionName} · {relation.adoptedAtLabel}</div>)}</> : <PostDetail post={publicPost(detail)} slots={{
        summary: detail.summary && <Notice><h2 className="text-section mb-2">AI 요약</h2>{detail.summary}</Notice>,
        reaction: <><h2 className="text-section">주민 참여</h2><Notice>공감해요 {detail.reactions.empathy} · 필요해요 {detail.reactions.need} · 궁금해요 {detail.reactions.curious}<br/>댓글·답글 {detail.post.commentCount}</Notice></>,
        comment: <section className="flex flex-col gap-internal"><h2 className="text-section">주민 의견</h2>{!detail.opinions.length ? <Notice>아직 주민 의견이 없습니다.</Notice> : detail.opinions.map(opinion => <article key={opinion.id} className="rounded-card border border-border bg-surface p-section text-body break-words"><p className="text-secondary">{opinion.authorLabel} · {opinion.createdAtLabel}</p><p className="mt-2 whitespace-pre-wrap">{opinion.body}</p>{opinion.replies.map((reply, index) => <p key={index} className="mt-2 whitespace-pre-wrap">↳ 답글 · {reply}</p>)}</article>)}</section>,
      }}/>}
      {state.feedback && <Notice role="status" tone={state.feedback.startsWith("저장하지") ? "error" : "info"}>{state.feedback}</Notice>}
      {own ? <Button variant="secondary" disabled={!allowed || state.pending} onClick={() => void store.change(false)}>{state.pending ? "취소 중…" : "본 기관 채택 취소"}</Button> : <Button disabled={!allowed || state.pending} onClick={() => void store.change(true)}>{state.pending ? "채택 중…" : allowed ? "이 안건 채택" : "채택 불가 · 담당 지역 밖"}</Button>}
      {state.feedback.startsWith("저장하지") && <Button disabled={!allowed || state.pending} onClick={() => void store.change(!own)}>다시 시도</Button>}
      {own && !state.record && <Button variant="secondary" onClick={() => store.showRecord(true)}>채택 기록</Button>}
      {state.record && <Button variant="secondary" disabled={state.pending} onClick={() => store.showRecord(false)}>안건 검토로</Button>}
      {!state.record && <Notice>기관명과 채택 시각만 공개됩니다.</Notice>}
      <Button variant="secondary" disabled={state.pending} onClick={onBack}>담당자 목록으로</Button>
    </>, title);
  }
  const items = visibleAgendas(state, authority);
  const qualification = authority.institution.status === "ready" ? authority.institution.qualification : null;
  const regions = [...new Map(state.items.map(item => [item.regionId, item.post.metadata.regionName])).entries()];
  const chip = (active: boolean) => `rounded-chip px-page py-internal text-caption ${active ? "bg-[#0F7662] text-white" : "bg-[#F3F4F6] text-[#4B5563]"}`;
  return <div ref={shell}>{layout(<>
    <Notice>유효 기관 인증 · 담당 지역 {qualification && "responsibleRegions" in qualification ? qualification.responsibleRegions.map(region => region.name).join(", ") : ""}</Notice>
    <label className="flex flex-col gap-internal rounded-card border border-border bg-surface px-page py-section text-body">지역 필터<select aria-label="지역 필터" value={state.regionId} onChange={event => store.filter(state.scope, event.target.value)} className="w-full bg-transparent text-secondary"><option value="">전체 지역</option>{regions.map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label>
    <p className="text-caption">목록</p><div className="flex flex-wrap gap-internal"><button type="button" aria-pressed={state.scope === "all"} className={chip(state.scope === "all")} onClick={() => store.filter("all", state.regionId)}>전체 공개 안건</button><button type="button" aria-pressed={state.scope === "adopted"} className={chip(state.scope === "adopted")} onClick={() => store.filter("adopted", state.regionId)}>채택한 게시물</button></div>
    <Notice>반응 수 내림차순 · {state.regionId ? regions.find(([id]) => id === state.regionId)?.[1] : "전체 지역"} · 결과 {state.phase === "ready" ? items.length : "확인 중"}{state.phase === "ready" ? "개" : ""}</Notice>
    {(state.phase === "idle" || state.phase === "loading") && <Notice role="status">안건 목록 불러오는 중</Notice>}
    {state.phase === "error" && <Notice tone="error"><p>안건 목록을 불러오지 못했습니다. 필터는 유지했습니다.</p><Button onClick={() => void store.load()}>다시 시도</Button></Notice>}
    {state.phase === "unavailable" && <Notice>Adoption Service 연결 대기 · 실 API 연동 대기</Notice>}
    {state.phase === "ready" && (items.length ? items.map(item => <PostCard key={item.post.id} post={publicPost(item)} onOpen={onOpen}/>) : <Notice>해당 조건의 안건이 없습니다.</Notice>)}
  </>)}</div>;
}
export function OfficerAgendasHost({ children, destination }: { children: ReactNode; destination: Destination | null }) {
  const context = useOfficerAgendas(); if (!context) return children;
  return <HostContent {...context} destination={destination}>{children}</HostContent>;
}
function HostContent({ children, destination, store, authority }: { children: ReactNode; destination: Destination | null; store: OfficerStore; authority: OfficerAuthority }) {
  const navigation = useNavigation(); const state = useOfficerState(store);
  const visible = destination?.id === "officerAgendas" || destination?.id === "officerAgenda";
  const postId = destination?.id === "officerAgenda" ? destination.params.postId : undefined;
  const allowed = canRead(authority);
  useEffect(() => {
    if (!visible || !allowed) return;
    if (postId) { if (state.detailPhase === "idle" || state.detail?.post.id !== postId && state.detailPhase === "ready") void store.read(postId); }
    else if (state.phase === "idle") void store.load();
  }, [store, visible, allowed, postId, state.phase, state.detailPhase, state.detail?.post.id]);
  return <><div hidden={visible}>{children}</div>{visible && <OfficerAgendasScreen store={store} authority={authority} postId={postId} onOpen={id => { void store.read(id); navigation.navigate({ destination: { id: "officerAgenda", params: { postId: id } }, origin: { id: "officerAgendas" } }); }} onBack={() => navigation.navigate({ destination: { id: postId ? "officerAgendas" : "me" }, origin: { id: "officerAgendas" } }, true)}/>}</>;
}
