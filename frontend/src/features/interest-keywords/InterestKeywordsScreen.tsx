"use client";
import { useEffect, useRef, useSyncExternalStore } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
import { AccessGuard } from "../../lib/navigation";
import { interestKeywords, type InterestKeyword, type KeywordState, type KeywordStore } from "./model";

function TopicChipGroup({ selected, disabled, onToggle }: { selected: readonly InterestKeyword[]; disabled: boolean; onToggle: (keyword: InterestKeyword) => void }) {
  return <div role="group" aria-label="추천 관심 키워드" className="grid shrink-0 grid-cols-3 gap-x-internal gap-y-section">{interestKeywords.map(keyword => {
    const checked = selected.includes(keyword);
    return <button key={keyword} type="button" aria-pressed={checked} aria-label={keyword} disabled={disabled || (!checked && selected.length === 4)} onClick={() => onToggle(keyword)} className={`rounded-chip px-page py-internal text-caption disabled:cursor-not-allowed ${checked ? "bg-selected text-surface" : "bg-[#F3F4F6] text-[#4B5563]"}`}><span aria-hidden="true">{checked ? "☑" : "□"} </span>{keyword}</button>;
  })}</div>;
}
export function InterestKeywordsView({ state, onToggle, onSave, onRetryLoad, onCancel }: { state: KeywordState; onToggle: (keyword: InterestKeyword) => void; onSave: () => void; onRetryLoad: () => void; onCancel: () => void }) {
  const editable = ["ready", "success", "save-error"].includes(state.phase);
  return <MobileLayout header={<Header title="관심 키워드" onBack={onCancel}/> }>
    <Notice>{state.draft.length === 4 ? "최대 4개까지 선택할 수 있습니다." : "추천에 사용할 키워드를 최대 4개 선택해 주세요."}</Notice>
    <p className="text-caption" role="status">선택 {state.draft.length}/4</p>
    {(state.phase === "idle" || state.phase === "loading") && <Notice role="status">관심 키워드 불러오는 중</Notice>}
    {state.phase === "load-error" && <><Notice tone="error" role="alert">관심 키워드를 불러오지 못했습니다.</Notice><Button onClick={onRetryLoad}>다시 조회</Button></>}
    <TopicChipGroup selected={state.draft} disabled={!editable} onToggle={onToggle}/>
    <Button disabled={!editable} aria-busy={state.phase === "pending"} onClick={onSave}>{state.phase === "pending" ? "저장 중" : state.phase === "save-error" ? "다시 시도" : "저장"}</Button>
    {state.phase === "save-error" ? <Notice tone="error" role="alert">저장에 실패했습니다. 기존 선택을 유지했습니다. 다시 시도해 주세요.</Notice> : <Notice>선택한 키워드는 추천에만 사용됩니다.</Notice>}
    {state.phase === "success" && <Notice role="status">관심 키워드가 저장되었습니다.</Notice>}
    {state.phase === "pending" && <Button variant="secondary" onClick={onCancel}>취소</Button>}
  </MobileLayout>;
}
function Content({ store, visible, onReturn }: { store: KeywordStore; visible: boolean; onReturn: () => void }) {
  const state = useSyncExternalStore(store.subscribe, store.getState, store.getState);
  const shell = useRef<HTMLDivElement>(null);
  useEffect(() => { if (visible && store.getState().phase === "idle") void store.load(); }, [store, visible]);
  useEffect(() => {
    const main = shell.current?.querySelector("main"); if (!main || !visible) return;
    main.scrollTop = store.getState().scroll; const record = () => store.setScroll(main.scrollTop);
    main.addEventListener("scroll", record); return () => main.removeEventListener("scroll", record);
  }, [store, visible]);
  async function save() { const success = await (state.phase === "save-error" ? store.retry() : store.save()); if (success) onReturn(); }
  return <div ref={shell}><InterestKeywordsView state={state} onToggle={store.toggle} onSave={() => void save()} onRetryLoad={() => void store.load()} onCancel={() => { store.cancel(); onReturn(); }}/></div>;
}
export function InterestKeywordsScreen(props: { store: KeywordStore; visible: boolean; onReturn: () => void }) {
  return <AccessGuard destination={{ id: "interestKeywords" }} fallback={(result, retry) => <MobileLayout header={<Header title="관심 키워드" onBack={props.onReturn}/> }><Notice>{result.status === "loading" ? "인증 상태 확인 중" : result.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>{retry && <Button onClick={() => void retry()}>다시 확인</Button>}</MobileLayout>}><Content {...props}/></AccessGuard>;
}
