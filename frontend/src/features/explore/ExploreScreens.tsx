"use client";

import { useState, type ReactNode } from "react";
import { BottomNavigation, type NavigationItem } from "../../components/layout/BottomNavigation";
import { Header, HeaderAction } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { bottomNavigationDestinations, headerDestinations, useNavigation } from "../../lib/navigation";
import { PostCard } from "../post/PostCard";
import type { PostDisplayModel } from "../post/model";
import { boardTopics, canParticipateInExploreRegion, type BoardFilters, type BoardPage, type BoardTopicFilter, type BoardTypeFilter, type ExploreContext, type ExploreRegion, type HomeContent, type LoadState } from "./model";

type Slots = { onSearch?: () => void; onMap?: () => void; onRecommendations?: () => void; onNewRecommendation?: () => void; onExploreRegionChange?: (region: ExploreRegion) => void; onOpenPost?: (postId: string, stateRef: string) => void };

function ExploreHeader({ origin }: { origin: "home" | "board" }) {
  const navigation = useNavigation();
  return <Header variant="brand" rightAction={<><HeaderAction action="settings" variant="main" onAction={() => navigation.navigate({ destination: headerDestinations.settings, origin: { id: origin } })} /><HeaderAction action="notification" variant="main" onAction={() => navigation.navigate({ destination: headerDestinations.notification, origin: { id: origin } })} /></>} />;
}

function Navigation({ active, origin }: { active: NavigationItem; origin: "home" | "board" }) {
  const navigation = useNavigation();
  return <BottomNavigation activeItem={active} onNavigate={(item) => navigation.navigate({ destination: bottomNavigationDestinations[item], origin: { id: origin } })} />;
}

function RegionControl({ context, onChangeRegion }: { context: ExploreContext; onChangeRegion: () => void }) {
  return <div className="flex items-center justify-between gap-3 rounded-card bg-soft p-section"><div><p className="text-caption text-secondary">탐색 지역</p><strong className="text-card-title">{context.currentExploreRegion.name}</strong></div><Button variant="secondary" className="min-h-0 px-3 py-2 text-caption" onClick={onChangeRegion}>지역 변경</Button></div>;
}

/** B02/B04 selection is a draft. Cancel/back never calls onApply, so currentExploreRegion remains unchanged. */
function RegionPicker({ context, onApply, onClose }: { context: ExploreContext; onApply?: (region: ExploreRegion) => void; onClose: () => void }) {
  const [selectedId, setSelectedId] = useState(context.currentExploreRegion.id);
  const selected = context.exploreRegionCandidates.find((region) => region.id === selectedId) ?? context.currentExploreRegion;
  return <section role="dialog" aria-modal="true" aria-label="탐색 지역 선택" className="rounded-card border border-border bg-surface p-section shadow-card"><div className="flex items-center justify-between gap-3"><h2 className="text-section">탐색 지역 선택</h2><button type="button" className="text-caption text-secondary underline" onClick={onClose}>취소</button></div><p className="mt-2 text-caption text-secondary">기본 활동 지역은 변경되지 않습니다.</p><div className="mt-3 space-y-2">{context.exploreRegionCandidates.map((region) => <button key={region.id} type="button" onClick={() => setSelectedId(region.id)} className={`flex w-full items-center justify-between rounded-input border px-3 py-3 text-left ${selectedId === region.id ? "border-primary bg-soft" : "border-border"}`}><span>{region.name}</span><span className="text-caption text-secondary">{region.neighborVerified ? "이웃 인증 완료" : "읽기 전용"}</span></button>)}</div><div className="mt-3 flex gap-2"><Button variant="secondary" className="min-h-0 flex-1 py-2" onClick={onClose}>뒤로</Button><Button className="min-h-0 flex-1 py-2" onClick={() => { onApply?.(selected); onClose(); }}>적용</Button></div></section>;
}

function RegionNotice({ context }: { context: ExploreContext }) {
  return canParticipateInExploreRegion(context) ? null : <Notice tone="info">이 지역은 읽기 전용입니다. 이웃 인증을 완료한 지역에서만 참여할 수 있습니다.</Notice>;
}

function SearchEntry({ onSearch }: { onSearch?: () => void }) { return <button type="button" onClick={onSearch} className="w-full rounded-input border border-border bg-surface px-3 py-3 text-left text-body text-secondary">지역 게시물 검색</button>; }
function Section({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) { return <section><div className="mb-2 flex items-center justify-between gap-3"><h2 className="text-section">{title}</h2>{action}</div>{children}</section>; }

export function HomeScreen({ context, state, onRetry, ...slots }: Slots & { context: ExploreContext; state: LoadState<HomeContent>; onRetry?: () => void }) {
  const navigation = useNavigation();
  const [selectingRegion, setSelectingRegion] = useState(false);
  const open = (postId: string) => { const ref = `home:${context.currentExploreRegion.id}`; navigation.registerSnapshot({ destination: { id: "home" }, kind: "list", ref }); slots.onOpenPost?.(postId, ref); navigation.navigate({ destination: { id: "post", params: { postId } }, origin: { id: "home" }, sharedContextRef: ref }); };
  const goBoard = () => navigation.navigate({ destination: { id: "board" }, origin: { id: "home" } });
  const openSearch = () => { navigation.navigate({ destination: { id: "search" }, origin: { id: "home" } }); slots.onSearch?.(); };
  const openMap = () => { navigation.navigate({ destination: { id: "map" }, origin: { id: "home" } }); slots.onMap?.(); };
  return <MobileLayout header={<ExploreHeader origin="home" />} bottomNavigation={<Navigation active="main" origin="home" />}>
    <RegionControl context={context} onChangeRegion={() => setSelectingRegion(true)} />{selectingRegion && <RegionPicker context={context} onApply={slots.onExploreRegionChange} onClose={() => setSelectingRegion(false)} />}<SearchEntry onSearch={openSearch} /><RegionNotice context={context} />
    {state.kind === "loading" && <div aria-busy="true" className="space-y-3"><div className="h-32 animate-pulse rounded-card bg-disabled" /><div className="h-32 animate-pulse rounded-card bg-disabled" /></div>}
    {state.kind === "error" && <Notice tone="error" role="alert"><p>{state.message}</p><Button className="mt-3" onClick={onRetry}>다시 시도</Button></Notice>}
    {state.kind === "empty" && <><Notice>등록된 게시물이 없습니다.</Notice><Button onClick={goBoard}>게시판 열기</Button></>}
    {state.kind === "success" && <><Section title="우리 동네 이야기" action={<button type="button" className="text-caption text-primary underline" onClick={goBoard}>전체 보기</button>}>{state.data.regionPosts.length ? <div className="space-y-3">{state.data.regionPosts.map((post) => <PostCard key={post.id} post={post} onOpen={open} />)}</div> : <Notice>등록된 게시물이 없습니다.</Notice>}</Section><Section title="진행 중인 투표">{state.data.openVotes.length ? <div className="space-y-3">{state.data.openVotes.map((post) => <PostCard key={post.id} post={post} onOpen={open} />)}</div> : <Notice>진행 중인 투표가 없습니다.</Notice>}</Section><Section title="이슈 지도"><button type="button" className="w-full rounded-card border border-dashed border-border bg-surface p-5 text-center text-body text-secondary" onClick={openMap}>이슈 지도 열기</button></Section></>}
  </MobileLayout>;
}

const typeOptions: readonly { value: BoardTypeFilter; label: string }[] = [{ value: "ALL", label: "전체" }, { value: "LOCAL_AGENDA", label: "지역 안건" }, { value: "LOCAL_ACTIVITY", label: "지역 활동 정보" }, { value: "VOTE", label: "투표" }];
function Chip({ selected, children, onClick }: { selected: boolean; children: ReactNode; onClick: () => void }) { return <button type="button" onClick={onClick} className={`shrink-0 rounded-chip border px-3 py-1.5 text-caption ${selected ? "border-primary bg-primary text-surface" : "border-border bg-surface text-secondary"}`}>{children}</button>; }

export function BoardScreen({ context, state, onLoadMore, onRetry, ...slots }: Slots & { context: ExploreContext; state: LoadState<BoardPage>; onRetry?: () => void; onLoadMore?: () => void }) {
  const navigation = useNavigation();
  const [selectingRegion, setSelectingRegion] = useState(false);
  const [selection, setSelection] = useState<Pick<BoardFilters, "type" | "topic">>({ type: "ALL", topic: "ALL" });
  const filters: BoardFilters = { regionId: context.currentExploreRegion.id, ...selection };
  const change = (next: Partial<Pick<BoardFilters, "type" | "topic">>) => setSelection((current) => ({ ...current, ...next }));
  const open = (postId: string) => { const ref = `board:${filters.regionId}:${filters.type}:${filters.topic}`; navigation.registerSnapshot({ destination: { id: "board" }, kind: "list", ref }); slots.onOpenPost?.(postId, ref); navigation.navigate({ destination: { id: "post", params: { postId } }, origin: { id: "board" }, sharedContextRef: ref }); };
  const openSearch = () => { navigation.navigate({ destination: { id: "search" }, origin: { id: "board" } }); slots.onSearch?.(); };
  const openRecommendations = () => { navigation.navigate({ destination: { id: "recommendations" }, origin: { id: "board" } }); slots.onRecommendations?.(); };
  const refreshRecommendations = () => { navigation.navigate({ destination: { id: "newRecommendation" }, origin: { id: "board" } }); slots.onNewRecommendation?.(); };
  // The real adapter returns an already-filtered page. Keeping this defensive client
  // filter makes the development fixture demonstrate the same contract without
  // adding a second mock endpoint or a guessed query string.
  const posts = state.kind === "success" ? state.data.posts.filter((post) =>
    (filters.type === "ALL" || post.type === filters.type) &&
    (filters.topic === "ALL" || post.metadata.topic === filters.topic),
  ) : [];
  return <MobileLayout header={<ExploreHeader origin="board" />} bottomNavigation={<Navigation active="main" origin="board" />}>
    <RegionControl context={context} onChangeRegion={() => setSelectingRegion(true)} />{selectingRegion && <RegionPicker context={context} onApply={slots.onExploreRegionChange} onClose={() => setSelectingRegion(false)} />}<SearchEntry onSearch={openSearch} /><RegionNotice context={context} />
    <section><h1 className="text-section">지역 게시판</h1><div className="mt-3 flex gap-2 overflow-x-auto pb-1">{typeOptions.map((option) => <Chip key={option.value} selected={filters.type === option.value} onClick={() => change({ type: option.value })}>{option.label}</Chip>)}</div><div className="mt-2 flex gap-2 overflow-x-auto pb-1"><Chip selected={filters.topic === "ALL"} onClick={() => change({ topic: "ALL" })}>전체</Chip>{boardTopics.map((topic) => <Chip key={topic} selected={filters.topic === topic} onClick={() => change({ topic: topic as BoardTopicFilter })}>{topic}</Chip>)}</div></section>
    <section aria-label="추천" className="rounded-card bg-soft p-section"><h2 className="text-card-title">맞춤 추천</h2><p className="mt-1 text-body text-secondary">추천 결과와 새 추천은 #97에서 연결됩니다.</p><div className="mt-2 flex gap-3"><button type="button" className="text-caption text-primary underline" onClick={openRecommendations}>추천 보기</button><button type="button" className="text-caption text-primary underline" onClick={refreshRecommendations}>새 추천</button></div></section>
    {state.kind === "loading" && <div aria-busy="true" className="space-y-3"><div className="h-36 animate-pulse rounded-card bg-disabled" /><div className="h-36 animate-pulse rounded-card bg-disabled" /></div>}
    {state.kind === "error" && <Notice tone="error" role="alert"><p>{state.message}</p><Button className="mt-3" onClick={onRetry}>다시 시도</Button></Notice>}
    {state.kind === "empty" && <Notice>조건에 맞는 게시물이 없습니다.</Notice>}
    {state.kind === "success" && <><div className="space-y-3">{posts.length ? posts.map((post: PostDisplayModel) => <PostCard key={post.id} post={post} onOpen={open} />) : <Notice>조건에 맞는 게시물이 없습니다.</Notice>}</div>{state.data.pageInfo.nextCursor && <Button variant="secondary" onClick={onLoadMore}>더 보기</Button>}</>}
  </MobileLayout>;
}
