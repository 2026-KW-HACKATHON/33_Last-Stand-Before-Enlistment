"use client";
import { useEffect, useRef, useSyncExternalStore } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Notice } from "../../components/ui/Notice";
import { SettingRow } from "../../components/ui/SettingRow";
import { AccessGuard } from "../../lib/navigation";
import type { SignupRegion } from "../signup/contracts";
import type { InterestRegionState, InterestRegionStore } from "./model";

export function InterestRegionsView({ state, onQuery, onToggle, onLoad, onSearch, onRetry, onBack, onCancel }: {
  state: InterestRegionState; onQuery: (query: string) => void; onToggle: (region: SignupRegion) => void;
  onLoad: () => void; onSearch: () => void; onRetry: () => void; onBack: () => void; onCancel: () => void;
}) {
  const rows = Array.from(new Map([...state.saved, ...state.candidates].map(region => [region.reference, region])).values());
  const pending = state.mutation === "pending";
  return <MobileLayout header={<Header title="관심 지역" onBack={onBack}/> }>
    <Notice>추가·삭제는 즉시 반영됩니다. 0개도 가능합니다.</Notice>
    <Input label="지역 검색" placeholder="관심 지역 찾기" value={state.query} onChange={event => onQuery(event.currentTarget.value)}/>
    {(state.phase === "idle" || state.phase === "loading") && <Notice role="status">관심 지역 불러오는 중</Notice>}
    {state.phase === "error" && <><Notice tone="error" role="alert">관심 지역을 불러오지 못했습니다. 기존 저장값은 유지됩니다.</Notice><Button onClick={onLoad}>목록 다시 조회</Button></>}
    {state.searchPhase === "loading" && <Notice role="status">지역 검색 중</Notice>}
    {state.searchPhase === "error" && <><Notice tone="error" role="alert">지역 검색에 실패했습니다.</Notice><Button onClick={onSearch}>검색 다시 시도</Button></>}
    {state.phase === "ready" && state.saved.length === 0 && <Notice>저장된 관심 지역이 없습니다. 0개도 가능합니다.</Notice>}
    {state.searchPhase === "ready" && state.candidates.length === 0 && <Notice>검색 결과가 없습니다.</Notice>}
    {rows.map(region => {
      const selected = state.saved.some(saved => saved.reference === region.reference);
      const action = selected ? "제거" : "추가";
      return <SettingRow key={region.reference} label={region.label} value={`${action}  ›`} aria-label={`${region.label} ${action}`} aria-pressed={selected} disabled={state.phase !== "ready" || pending} pending={pending && state.pending?.reference === region.reference} onClick={() => onToggle(region)}/>;
    })}
    {pending && <><Notice role="status">관심 지역 저장 중</Notice><Button variant="secondary" onClick={onCancel}>변경 취소</Button></>}
    {state.mutation === "error" && <><Notice tone="error" role="alert">저장에 실패했습니다. 이전 관심 지역을 유지합니다.</Notice><Button onClick={onRetry}>저장 다시 시도</Button></>}
    {state.mutation === "success" && <Notice role="status">관심 지역이 반영되었습니다.</Notice>}
    <Button variant="secondary" onClick={onBack}>돌아가기</Button>
  </MobileLayout>;
}
function Content({ store, visible, onBack }: { store: InterestRegionStore; visible: boolean; onBack: () => void }) {
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  const shell = useRef<HTMLDivElement>(null);
  useEffect(() => { if (visible && store.getState().phase === "idle") void store.load(); }, [store, visible]);
  useEffect(() => { if (!visible) return; const timer = setTimeout(() => { void store.search(); }, 200); return () => clearTimeout(timer); }, [store, visible, state.query]);
  useEffect(() => {
    const main = shell.current?.querySelector("main");
    if (!main || !visible) return;
    main.scrollTop = store.getState().scroll;
    const record = () => store.setScroll(main.scrollTop);
    main.addEventListener("scroll", record);
    return () => main.removeEventListener("scroll", record);
  }, [store, visible]);
  return <div ref={shell}><InterestRegionsView state={state} onQuery={store.setQuery} onToggle={region => void store.toggle(region)} onLoad={() => void store.load()} onSearch={() => void store.search()} onRetry={() => void store.retrySave()} onCancel={store.cancel} onBack={onBack}/></div>;
}
export function InterestRegionsScreen(props: { store: InterestRegionStore; visible: boolean; onBack: () => void }) {
  return <AccessGuard destination={{ id: "interestRegions" }} fallback={(result, retry) => <MobileLayout header={<Header title="관심 지역" onBack={props.onBack}/>}><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}<Button variant="secondary" onClick={props.onBack}>돌아가기</Button></MobileLayout>}><Content {...props}/></AccessGuard>;
}
