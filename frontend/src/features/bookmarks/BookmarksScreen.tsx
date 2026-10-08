"use client";
import { useEffect, useEffectEvent, useRef, useState, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { BottomNavigation } from "../../components/layout/BottomNavigation";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { AccessGuard, bottomNavigationDestinations, useNavigation, type Destination } from "../../lib/navigation";
import { PostCard } from "../post/PostCard";
import type { BookmarkBinding } from "../post/PostDetail";
import { bookmarkTopics, bookmarkTypes, selectBookmarks, type BookmarksStore, type BookmarkType } from "./model";
import { useBookmarks, useBookmarksState, useBookmarkBinding } from "./provider";

export type BookmarkDetailRenderer = (request: { postId: string; bookmark: BookmarkBinding; onReturn: () => void }) => ReactNode;
function SavedFeedback({ onDismiss }: { onDismiss: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { const element = dialog.current; element?.showModal(); return () => element?.close(); }, []);
  return <dialog ref={dialog} aria-label="북마크 저장 완료" onCancel={event => { event.preventDefault(); onDismiss(); }} className="m-auto w-[calc(100%-32px)] max-w-mobile rounded-card bg-white p-page backdrop:bg-black/30"><div className="flex flex-col gap-section"><p className="text-body">저장되었습니다</p><Button autoFocus onClick={onDismiss}>확인</Button></div></dialog>;
}
/** Compose FE1 state with FE2's existing detail single-button interface; no duplicate detail page. */
export function BookmarkDetailBridge({ postId, onReturn, renderDetail }: { postId: string; onReturn: () => void; renderDetail: BookmarkDetailRenderer }) {
  const context = useBookmarks();
  const binding = useBookmarkBinding(postId, { destination: { id: "post", params: { postId } }, origin: { id: "bookmarks" } });
  if (!context || !binding) return null;
  return <DetailContent {...context} postId={postId} binding={binding} onReturn={onReturn} renderDetail={renderDetail}/>;
}
function DetailContent({ store, postId, binding, onReturn, renderDetail }: { store: BookmarksStore; postId: string; binding: BookmarkBinding; onReturn: () => void; renderDetail: BookmarkDetailRenderer }) {
  const state = useBookmarksState(store); const row = state.details[postId];
  if (!row || row.phase === "loading") return <MobileLayout header={<Header title="북마크" onBack={onReturn}/>}><Notice role="status">북마크 상태 확인 중</Notice></MobileLayout>;
  if (row.phase === "error") return <MobileLayout header={<Header title="북마크" onBack={onReturn}/>}><Notice tone="error"><p>{row.feedback}</p><Button onClick={() => void store.read(postId)}>다시 시도</Button></Notice></MobileLayout>;
  if (row.phase === "unavailable" && row.value?.available === false) return <MobileLayout header={<Header title="북마크" onBack={onReturn}/>}><Notice>삭제되었거나 접근할 수 없는 게시물입니다. 북마크 목록에서 제외했습니다.</Notice><Button onClick={onReturn}>목록으로</Button></MobileLayout>;
  return <>{renderDetail({ postId, bookmark: binding, onReturn })}
    {row.saved && <SavedFeedback onDismiss={() => store.dismissSaved(postId)}/>}
  </>;
}
export function BookmarksScreen({ store, subjectKey, onBack, renderDetail }: { store: BookmarksStore; subjectKey: string | null; onBack: () => void; renderDetail?: BookmarkDetailRenderer }) {
  const state = useBookmarksState(store); const navigation = useNavigation(); const shell = useRef<HTMLDivElement>(null);
  const [detail, setDetail] = useState<string | null>(null);
  const items = subjectKey ? selectBookmarks(state) : [];
  const returnToList = () => { setDetail(null); void store.load(); };
  useEffect(() => {
    const main = shell.current?.querySelector("main"); if (!main || !state.visible || detail || state.phase !== "ready") return;
    main.scrollTop = store.getState().scroll; const save = () => store.scroll(main.scrollTop);
    main.addEventListener("scroll", save); return () => main.removeEventListener("scroll", save);
  }, [store, state.visible, state.phase, state.type, state.topic, detail]);
  function open(postId: string) {
    if (renderDetail) { setDetail(postId); return; }
    store.close(); navigation.navigate({ destination: { id: "post", params: { postId } }, origin: { id: "bookmarks" } });
  }
  const chip = (selected: boolean) => `min-w-0 rounded-chip px-2 py-internal text-caption ${selected ? "bg-[#0F7662] text-white" : "bg-[#F3F4F6] text-[#4B5563]"}`;
  return <AccessGuard destination={{ id: "bookmarks" }} fallback={(result, retry) => <MobileLayout header={<Header title="북마크" onBack={onBack}/>}><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}{["login-required", "signup-required"].includes(result.status) && <Button onClick={() => navigation.beginAuthentication({ destination: { id: "bookmarks" }, origin: { id: state.origin } }, result.status === "signup-required" ? "signup" : "login")}>로그인 / 가입</Button>}</MobileLayout>}>
    <div ref={shell} hidden={!!detail}><MobileLayout header={<Header title="북마크" onBack={onBack}/>} bottomNavigation={<BottomNavigation activeItem="my" onNavigate={item => { store.close(); navigation.navigate({ destination: bottomNavigationDestinations[item], origin: { id: "bookmarks" } }); }}/>}>
      <p className="text-caption">유형</p>
      <div className="grid grid-cols-3 gap-internal" aria-label="북마크 유형">{(Object.keys(bookmarkTypes) as BookmarkType[]).map(type => <button key={type} type="button" aria-pressed={state.type === type} className={chip(state.type === type)} onClick={() => store.filter(type, state.topic)}>{bookmarkTypes[type]}</button>)}</div>
      <p className="text-caption">주제</p>
      <div className="grid grid-cols-3 gap-internal" aria-label="북마크 주제">{bookmarkTopics.map(topic => <button key={topic} type="button" aria-pressed={state.topic === topic} className={chip(state.topic === topic)} onClick={() => store.filter(state.type, topic)}>{topic}</button>)}</div>
      {["idle", "loading"].includes(state.phase) && <Notice role="status">북마크 불러오는 중</Notice>}
      {state.phase === "error" && <Notice tone="error"><p>북마크를 불러오지 못했습니다. 선택한 조건을 유지했습니다.</p><Button onClick={() => void store.load()}>다시 시도</Button></Notice>}
      {state.phase === "unavailable" && <Notice>Bookmark Service 연결 대기 · 실 API 연동 대기</Notice>}
      {state.phase === "ready" && (!items.length ? <Notice>해당 조건의 북마크가 없습니다.</Notice> : items.map(post => <PostCard key={post.id} post={post} onOpen={open}/>))}
    </MobileLayout></div>
    {detail && renderDetail && <BookmarkDetailBridge postId={detail} onReturn={returnToList} renderDetail={renderDetail}/>}
  </AccessGuard>;
}
export function BookmarksHost({ children, currentDestination, renderDetail }: { children: ReactNode; currentDestination: Destination | null; renderDetail?: BookmarkDetailRenderer }) {
  const context = useBookmarks();
  if (!context) return children;
  return <HostContent {...context} currentDestination={currentDestination} renderDetail={renderDetail}>{children}</HostContent>;
}
function HostContent({ children, store, subjectKey, currentDestination, renderDetail }: { children: ReactNode; store: BookmarksStore; subjectKey: string | null; currentDestination: Destination | null; renderDetail?: BookmarkDetailRenderer }) {
  const state = useBookmarksState(store); const navigation = useNavigation();
  const previous = useRef(currentDestination?.id);
  const enter = useEffectEvent(() => {
    const entry = navigation.state.current;
    const origin = entry?.destination.id === "bookmarks" && (entry.origin?.id === "me" || entry.origin?.id === "settings") ? entry.origin.id : store.getState().origin;
    store.open(origin); void store.load();
  });
  const restoreSettings = useEffectEvent(() => navigation.navigate({ destination: { id: "settings" }, origin: { id: "settings" } }));
  useEffect(() => {
    if (currentDestination?.id === "bookmarks") enter();
    else { store.close(); if (previous.current === "bookmarks" && currentDestination?.id === "me" && store.getState().origin === "settings") restoreSettings(); }
    previous.current = currentDestination?.id;
  }, [store, currentDestination?.id]);
  return <><div hidden={state.visible}>{children}</div>{state.visible && <BookmarksScreen store={store} subjectKey={subjectKey} renderDetail={renderDetail} onBack={() => { store.close(); navigation.navigate({ destination: { id: state.origin }, origin: { id: state.origin } }, true); }}/>}</>;
}
