"use client";
import { useEffect, useRef, useSyncExternalStore } from "react";
import { Header } from "../../components/layout/Header";
import { BottomNavigation, type BottomNavigationProps } from "../../components/layout/BottomNavigation";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { AccessGuard, useNavigation } from "../../lib/navigation";
import { NotificationItem } from "./NotificationItem";
import { filteredNotifications, type NotificationState, type NotificationStore, type NotificationTab } from "./model";

const tabs: readonly [NotificationTab, string][] = [["all", "전체"], ["notification", "알림"], ["activity", "활동"]];
export function NotificationsView({ state, onTab, onSelect, onRetryLoad, onRetryItem, onBack, onReturn, onNavigate, targetError }: {
  state: NotificationState; onTab: (tab: NotificationTab) => void; onSelect: (id: string) => void;
  onRetryLoad: () => void; onRetryItem: () => void; onBack: () => void; onReturn: () => void;
  onNavigate: BottomNavigationProps["onNavigate"]; targetError?: boolean;
}) {
  const unavailable = state.phase === "deleted" || state.phase === "inaccessible";
  if (unavailable) return <MobileLayout header={<Header title={state.phase === "deleted" ? "삭제된 게시물 안내" : "접근할 수 없는 게시물 안내"} onBack={onReturn}/>}>
    <Notice>{state.phase === "deleted" ? "게시물이 삭제되었습니다." : "게시물에 접근할 수 없습니다."}</Notice>
    <Notice>{state.phase === "deleted" ? "삭제된 게시물의 내용을 다시 볼 수 없습니다." : "접근 권한을 확인할 수 없어 내용을 표시하지 않습니다."}</Notice>
    <Button variant="secondary" onClick={onReturn}>알림으로</Button>
  </MobileLayout>;
  const items = filteredNotifications(state);
  return <MobileLayout header={<Header title="알림 및 활동" onBack={onBack}/>} bottomNavigation={<BottomNavigation activeItem="notification" onNavigate={onNavigate}/>}>
    <p className="text-caption">보기</p>
    <div role="group" aria-label="알림 및 활동 필터" className="grid shrink-0 grid-cols-3 gap-internal">{tabs.map(([id, label]) => <button key={id} type="button" aria-pressed={state.tab === id} disabled={state.phase === "pending"} onClick={() => onTab(id)} className={`rounded-chip px-page py-internal text-caption ${state.tab === id ? "bg-selected text-surface" : "bg-[#F3F4F6] text-[#4B5563]"}`}>{state.tab === id && <span aria-hidden="true">● </span>}{label}</button>)}</div>
    {["idle", "loading"].includes(state.phase) ? <Notice role="status">알림 불러오는 중</Notice>
      : state.phase === "load-error" ? <><Notice tone="error" role="alert">알림을 불러오지 못했습니다.</Notice><Button onClick={onRetryLoad}>다시 조회</Button></>
      : <>{items.length === 0 && <Notice>아직 {state.tab === "activity" ? "활동" : "알림"}이 없습니다.</Notice>}{items.map(item => <NotificationItem key={item.id} item={item} disabled={state.phase === "pending"} pending={state.pendingId === item.id && state.phase === "pending"} onSelect={() => onSelect(item.id)}/>)}</>}
    {state.phase === "pending" && <><Notice role="status">읽음 상태와 관련 원본 확인 중</Notice><Button variant="secondary" onClick={onReturn}>취소</Button></>}
    {(state.phase === "read-error" || state.phase === "target-error") && <><Notice tone="error" role="alert">{state.phase === "read-error" ? "읽음 처리에 실패했습니다. 기존 읽음 상태를 유지했습니다." : "관련 원본을 확인하지 못했습니다."}</Notice><Button onClick={onRetryItem}>다시 시도</Button></>}
    {targetError && <Notice tone="error" role="alert">관련 화면을 열지 못했습니다. 항목을 다시 선택해 주세요.</Notice>}
  </MobileLayout>;
}
function Content({ store, visible, onBack, onTarget, onNavigate, targetError, handoffPending, onCancel }: {
  store: NotificationStore; visible: boolean; onBack: () => void; onTarget: (id?: string) => void;
  onNavigate: BottomNavigationProps["onNavigate"]; targetError: boolean; handoffPending: boolean; onCancel: () => void;
}) {
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  const shell = useRef<HTMLDivElement>(null);
  useEffect(() => { if (visible && store.getState().phase === "idle") void store.load(); }, [store, visible]);
  const unavailable = state.phase === "deleted" || state.phase === "inaccessible";
  const loading = state.phase === "loading";
  useEffect(() => {
    const main = shell.current?.querySelector("main"); if (!main || !visible || unavailable) return;
    main.scrollTop = store.getState().scroll;
    const record = () => store.setScroll(main.scrollTop); main.addEventListener("scroll", record);
    return () => main.removeEventListener("scroll", record);
  }, [store, visible, unavailable, state.tab, loading]);
  return <div ref={shell}><NotificationsView state={handoffPending ? { ...state, phase: "pending" } : state} onTab={store.setTab} onSelect={onTarget} onRetryLoad={() => void store.load()} onRetryItem={() => onTarget()} onBack={onBack} onReturn={onCancel} onNavigate={onNavigate} targetError={targetError}/></div>;
}
export function NotificationsScreen(props: Parameters<typeof Content>[0]) {
  const navigation = useNavigation();
  return <AccessGuard destination={{ id: "notifications" }} fallback={(result, retry) => <MobileLayout header={<Header title="알림 및 활동" onBack={props.onBack}/>}>
    <Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>
    {retry && <Button onClick={() => void retry()}>다시 확인</Button>}
    {result.status === "login-required" && <Button onClick={() => navigation.beginAuthentication({ destination: { id: "notifications" } })}>로그인</Button>}
    {result.status === "signup-required" && <Button onClick={() => navigation.beginAuthentication({ destination: { id: "notifications" } }, "signup")}>가입 계속하기</Button>}
  </MobileLayout>}><Content {...props}/></AccessGuard>;
}
