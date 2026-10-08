"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { BottomNavigation } from "../../components/layout/BottomNavigation";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
import { PostCard } from "../post/PostCard";
import type { PersonalDetailRenderer } from "../personal-lists/PersonalListsScreen";
import { AccessGuard, bottomNavigationDestinations, useNavigation, type Destination } from "../../lib/navigation";
import { selectVotes, voteFilters, voteSummary, type MyVotesStore, type VoteFilter } from "./model";
import { useMyVotes, useMyVotesState } from "./provider";

export function VoteSummaryContent({ summary }: { summary: string }) {
  return <div className="flex flex-wrap gap-x-2 gap-y-1">{summary.split(" | ").map((part, index) => {
    const labeled = /^([^:]+): ([\s\S]*)$/.exec(part);
    return <span key={index} className="min-w-0">{labeled ? <><span className="mr-1 inline-flex items-center whitespace-nowrap rounded-chip bg-white px-1 py-[1px] align-middle text-[11px] leading-none">{labeled[1]}</span>{labeled[2]}</> : part}</span>;
  })}</div>;
}
export function MyVotesScreen({ store, subjectKey, onBack, renderDetail }: { store: MyVotesStore; subjectKey: string | null; onBack: () => void; renderDetail?: PersonalDetailRenderer }) {
  const state = useMyVotesState(store);
  const navigation = useNavigation();
  const shell = useRef<HTMLDivElement>(null);
  const [detail, setDetail] = useState<string | null>(null);
  const items = subjectKey ? selectVotes(state.records, subjectKey, state.filter) : [];
  const detailRow = subjectKey ? selectVotes(state.records, subjectKey, "ALL").find(row => row.postId === detail) : undefined;
  const inaccessible = !!detail && detailRow?.availability !== "available";
  const returnToList = () => { setDetail(null); void store.load(); };
  useEffect(() => {
    const main = shell.current?.querySelector("main");
    if (!main || !state.visible || detail || state.phase !== "ready") return;
    main.scrollTop = store.getState().scroll;
    const save = () => store.scroll(main.scrollTop);
    main.addEventListener("scroll", save);
    return () => main.removeEventListener("scroll", save);
  }, [store, state.visible, state.phase, state.filter, detail]);
  function open(postId: string, available: boolean) {
    if (!available || renderDetail) { setDetail(postId); return; }
    store.close();
    navigation.navigate({ destination: { id: "post", params: { postId } }, origin: { id: "myVotes" } });
  }
  return <AccessGuard destination={{ id: "myVotes" }} fallback={(result, retry) => <MobileLayout header={<Header title="참여한 투표" onBack={onBack}/>}><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}{["login-required", "signup-required"].includes(result.status) && <Button onClick={() => navigation.beginAuthentication({ destination: { id: "myVotes" }, origin: { id: "me" } }, result.status === "signup-required" ? "signup" : "login")}>로그인 / 가입</Button>}</MobileLayout>}>
    <div ref={shell} hidden={!!detail}><MobileLayout header={<Header title="참여한 투표" onBack={onBack}/>} bottomNavigation={<BottomNavigation activeItem="my" onNavigate={item => { store.close(); navigation.navigate({ destination: bottomNavigationDestinations[item], origin: { id: "myVotes" } }); }}/>}>
      <p className="text-caption">진행 상태</p>
      <div className="grid grid-cols-3 gap-internal" aria-label="투표 진행 상태">{(Object.keys(voteFilters) as VoteFilter[]).map(filter => <button key={filter} type="button" aria-pressed={state.filter === filter} className={`min-w-0 rounded-chip px-page py-internal text-caption ${state.filter === filter ? "bg-[#0F7662] text-white" : "bg-[#F3F4F6] text-[#4B5563]"}`} onClick={() => store.filter(filter)}>{voteFilters[filter]}</button>)}</div>
      {["idle", "loading"].includes(state.phase) && <Notice role="status">참여한 투표 불러오는 중</Notice>}
      {state.phase === "error" && <Notice tone="error"><p>투표 기록을 불러오지 못했습니다. 선택한 조건을 유지했습니다.</p><Button onClick={() => void store.load()}>다시 시도</Button></Notice>}
      {state.phase === "unavailable" && <Notice>개인 투표 Service 연결 대기 · 실 API 연동 대기</Notice>}
      {state.phase === "ready" && (!items.length ? <Notice>해당 조건의 참여한 투표가 없습니다.</Notice> : items.map(row => <section key={row.postId} className="flex flex-col gap-section" aria-label="개인 투표 기록">
        {row.availability === "available" ? <>
          <PostCard post={row.post} onOpen={id => open(id, true)}/>
          <Notice><VoteSummaryContent summary={voteSummary(row.snapshot, row.post.vote.status === "ENDED")}/></Notice>
          <Notice>{row.post.vote.status === "OPEN" ? "진행 중" : "종료"} · {row.timingLabel}{row.reminderLabel ? ` · ${row.reminderLabel}` : ""}{row.snapshot ? ` · ${row.snapshot.options.reduce((n, option) => n + option.count, 0)}명 참여` : ""}</Notice>
        </> : <><Notice tone="warning">접근할 수 없는 투표 · 개인 참여 기록 유지<p>참여 시각: {row.participatedAt}</p></Notice><Button variant="secondary" onClick={() => open(row.postId, false)}>접근 불가 안내</Button></>}
      </section>))}
    </MobileLayout></div>
    {detail && (inaccessible ? <MobileLayout header={<Header title="접근 불가 안내" onBack={returnToList}/>}><Notice tone="warning">삭제되었거나 현재 접근할 수 없는 게시물입니다. 개인 참여 기록은 유지됩니다.</Notice><Button variant="secondary" onClick={returnToList}>목록으로</Button></MobileLayout> : renderDetail?.(detail, returnToList))}
  </AccessGuard>;
}

export function MyVotesHost({ children, currentDestination, renderDetail }: { children: ReactNode; currentDestination: Destination | null; renderDetail?: PersonalDetailRenderer }) {
  const context = useMyVotes();
  if (!context) return children;
  return <HostContent {...context} currentDestination={currentDestination} renderDetail={renderDetail}>{children}</HostContent>;
}
function HostContent({ children, store, subjectKey, currentDestination, renderDetail }: { children: ReactNode; store: MyVotesStore; subjectKey: string | null; currentDestination: Destination | null; renderDetail?: PersonalDetailRenderer }) {
  const state = useMyVotesState(store);
  const navigation = useNavigation();
  useEffect(() => {
    if (currentDestination?.id === "myVotes") { store.open(); void store.load(); }
    else store.close();
  }, [store, currentDestination?.id]);
  return <><div hidden={state.visible}>{children}</div>{state.visible && <MyVotesScreen store={store} subjectKey={subjectKey} renderDetail={renderDetail} onBack={() => { store.close(); navigation.navigate({ destination: { id: "me" }, origin: { id: "myVotes" } }, true); }}/>}</>;
}
