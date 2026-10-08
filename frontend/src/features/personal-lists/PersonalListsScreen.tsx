"use client";
import { useEffectEvent, useEffect, useRef, useState, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { BottomNavigation } from "../../components/layout/BottomNavigation";
import { Notice } from "../../components/ui/Notice";
import { VoteValueSummary } from "../../components/ui/VoteValue";
import { Button } from "../../components/ui/Button";
import { PostCard } from "../post/PostCard";
import { postTypeLabel } from "../post/model";
import { AccessGuard, bottomNavigationDestinations, useNavigation, type Destination, type NavigationEntry } from "../../lib/navigation";
import { participationLabels, selectItems, type PersonalListsStore, type TypeFilter } from "./model";
import { useListState, usePersonalLists } from "./provider";
export type PersonalDetailRenderer = (postId: string, onReturn: () => void) => ReactNode;
export function PersonalListsScreen({ store, subjectKey, onBack, renderDetail }: { store: PersonalListsStore; subjectKey: string | null; onBack: () => void; renderDetail?: PersonalDetailRenderer }) {
 const state = useListState(store); const navigation = useNavigation(); const shell = useRef<HTMLDivElement>(null); const [detail, setDetail] = useState<string | null>(null);
 const view = state.views[state.kind];
 useEffect(() => { if (state.visible && state.phase === "idle") void store.load(); }, [store, state.visible, state.phase]);
 useEffect(() => { const main = shell.current?.querySelector("main"); if (!main || !state.visible || detail) return; main.scrollTop = view.scroll; const save = () => store.scroll(main.scrollTop); main.addEventListener("scroll", save); return () => main.removeEventListener("scroll", save); }, [store, state.visible, state.kind, state.phase, view.filter, view.scroll, detail]); // scroll is captured on restore, not on every scroll event
 const items = subjectKey ? selectItems(state.records, subjectKey, state.kind, view.filter) : [];
 const destination: Destination = { id: state.kind };
 const filters: TypeFilter[] = ["ALL", "LOCAL_AGENDA", "LOCAL_ACTIVITY", "VOTE"];
 function open(postId: string) { if (renderDetail) { setDetail(postId); return; } store.close(); navigation.navigate({ destination: { id: "post", params: { postId } }, origin: destination }); }
 return <><div ref={shell} hidden={!!detail || !state.visible}><MobileLayout header={<Header title={state.kind === "myPosts" ? "내가 만든 게시물" : "참여한 게시물"} onBack={onBack}/>} bottomNavigation={<BottomNavigation activeItem="my" onNavigate={item => { store.close(); navigation.navigate({ destination: bottomNavigationDestinations[item], origin: destination }); }}/>}><AccessGuard destination={destination} fallback={(result, retry) => <><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}{["login-required", "signup-required"].includes(result.status) && <Button onClick={() => navigation.beginAuthentication({ destination, origin: { id: state.origin } }, result.status === "signup-required" ? "signup" : "login")}>로그인 / 가입</Button>}</>}>
 <p className="text-caption">유형</p><div className="grid grid-cols-4 gap-1" aria-label="게시물 유형">{filters.map(filter => <button key={filter} type="button" aria-pressed={view.filter === filter} className={`min-w-0 whitespace-nowrap rounded-chip px-1 py-internal text-caption ${view.filter === filter ? "bg-primary text-white" : "bg-[#F3F4F6] text-[#4B5563]"}`} onClick={() => store.filter(filter)}>{filter === "ALL" ? "전체" : postTypeLabel(filter)}</button>)}</div>
 {(state.phase === "idle" || state.phase === "loading") && <Notice role="status">게시물 불러오는 중</Notice>}
 {state.phase === "error" && <Notice tone="error"><p>목록을 불러오지 못했습니다. 선택한 유형을 유지했습니다.</p><Button onClick={() => void store.load()}>다시 시도</Button></Notice>}
 {state.phase === "unavailable" && <Notice>개인 목록 Service 연결 대기 · 실 API 연동 대기</Notice>}
 {state.phase === "ready" && (!items.length ? <Notice>{state.kind === "participations" ? "아직 참여한 게시물이 없습니다" : "작성한 게시물이 없습니다"}</Notice> : items.map(item => <section key={item.post.id} className="flex flex-col gap-section"><PostCard post={item.post} onOpen={open}/>{state.kind === "participations" && <><p className="text-caption">내 현재 참여</p>{item.actions.some(action => action !== "VOTE") && <div className="flex flex-wrap gap-1.5" aria-label="일반 참여 상태">{item.actions.filter(action => action !== "VOTE").map(action => <span key={action} className="whitespace-nowrap rounded-chip bg-soft px-page py-internal text-caption text-primary">{participationLabels[action]}</span>)}</div>}{item.actions.includes("VOTE") && <div className="flex flex-wrap gap-internal"><span className="rounded-chip bg-[#F3F4F6] px-page py-internal text-caption text-[#4B5563]">{participationLabels.VOTE}</span></div>}{item.voteSummary && <Notice><VoteValueSummary summary={item.voteSummary}/></Notice>}</>}</section>))}
 {state.phase === "ready" && <Button variant="secondary" onClick={() => { store.close(); navigation.navigate({ destination: { id: "board" }, origin: destination }); }}>게시판으로</Button>}
 </AccessGuard></MobileLayout></div>{detail && state.visible && renderDetail?.(detail, () => { setDetail(null); void store.load(); })}</>;
}
/** Keep I01/L01 mounted, with separate per-list filters/scroll. Only the Owner detail renderer is injected. */
export function PersonalListsHost({ children, currentDestination, renderDetail }: { children: ReactNode; currentDestination: Destination | null; renderDetail?: PersonalDetailRenderer }) {
 const context = usePersonalLists(); const navigation = useNavigation();
 if (!context) return children;
 return <HostContent {...context} currentDestination={currentDestination} renderDetail={renderDetail} onNavigate={navigation.navigate}>{children}</HostContent>;
}
function HostContent({ children, store, subjectKey, currentDestination, renderDetail, onNavigate }: { children: ReactNode; store: PersonalListsStore; subjectKey: string | null; currentDestination: Destination | null; renderDetail?: PersonalDetailRenderer; onNavigate: (entry: NavigationEntry) => unknown }) {
 const state = useListState(store); const previous = useRef(currentDestination?.id); const restoreSettings = useEffectEvent(() => onNavigate({ destination: { id: "settings" }, origin: { id: "settings" } }));
 useEffect(() => { if (currentDestination?.id === "myPosts" || currentDestination?.id === "participations") { store.open(currentDestination.id, store.getState().origin); void store.load(); } else if (previous.current === "myPosts" || previous.current === "participations") { store.close(); if (currentDestination?.id === "me" && store.getState().origin === "settings") restoreSettings(); } previous.current = currentDestination?.id; }, [store, currentDestination?.id]);
 return <><div hidden={state.visible}>{children}</div>{state.visible && <PersonalListsScreen store={store} subjectKey={subjectKey} renderDetail={renderDetail} onBack={() => { store.close(); if (currentDestination?.id === "myPosts" || currentDestination?.id === "participations") onNavigate({ destination: { id: state.origin }, origin: { id: state.origin } }); }}/>}</>;
}
