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
  return <div className="flex items-center justify-between gap-3 rounded-card bg-soft p-section"><div><p className="text-caption text-secondary">Explore region</p><strong className="text-card-title">{context.currentExploreRegion.name}</strong></div><Button variant="secondary" className="min-h-0 px-3 py-2 text-caption" onClick={onChangeRegion}>Change region</Button></div>;
}

/** B02/B04 selection is a draft. Cancel/back never calls onApply, so currentExploreRegion remains unchanged. */
function RegionPicker({ context, onApply, onClose }: { context: ExploreContext; onApply?: (region: ExploreRegion) => void; onClose: () => void }) {
  const [selectedId, setSelectedId] = useState(context.currentExploreRegion.id);
  const selected = context.exploreRegionCandidates.find((region) => region.id === selectedId) ?? context.currentExploreRegion;
  return <section role="dialog" aria-modal="true" aria-label="Select explore region" className="rounded-card border border-border bg-surface p-section shadow-card"><div className="flex items-center justify-between gap-3"><h2 className="text-section">Select explore region</h2><button type="button" className="text-caption text-secondary underline" onClick={onClose}>Cancel</button></div><p className="mt-2 text-caption text-secondary">Your default activity region is not changed.</p><div className="mt-3 space-y-2">{context.exploreRegionCandidates.map((region) => <button key={region.id} type="button" onClick={() => setSelectedId(region.id)} className={`flex w-full items-center justify-between rounded-input border px-3 py-3 text-left ${selectedId === region.id ? "border-primary bg-soft" : "border-border"}`}><span>{region.name}</span><span className="text-caption text-secondary">{region.neighborVerified ? "Verified" : "Read-only"}</span></button>)}</div><div className="mt-3 flex gap-2"><Button variant="secondary" className="min-h-0 flex-1 py-2" onClick={onClose}>Back</Button><Button className="min-h-0 flex-1 py-2" onClick={() => { onApply?.(selected); onClose(); }}>Apply</Button></div></section>;
}

function RegionNotice({ context }: { context: ExploreContext }) {
  return canParticipateInExploreRegion(context) ? null : <Notice tone="info">This region is read-only. Participation is available in a neighbor-verified region.</Notice>;
}

function SearchEntry({ onSearch }: { onSearch?: () => void }) { return <button type="button" onClick={onSearch} className="w-full rounded-input border border-border bg-surface px-3 py-3 text-left text-body text-secondary">Search regional stories</button>; }
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
    {state.kind === "error" && <Notice tone="error" role="alert"><p>{state.message}</p><Button className="mt-3" onClick={onRetry}>Retry</Button></Notice>}
    {state.kind === "empty" && <><Notice>No posts have been registered yet.</Notice><Button onClick={goBoard}>Open board</Button></>}
    {state.kind === "success" && <><Section title="Regional stories" action={<button type="button" className="text-caption text-primary underline" onClick={goBoard}>View all</button>}>{state.data.regionPosts.length ? <div className="space-y-3">{state.data.regionPosts.map((post) => <PostCard key={post.id} post={post} onOpen={open} />)}</div> : <Notice>No posts have been registered yet.</Notice>}</Section><Section title="Open votes">{state.data.openVotes.length ? <div className="space-y-3">{state.data.openVotes.map((post) => <PostCard key={post.id} post={post} onOpen={open} />)}</div> : <Notice>No votes are open.</Notice>}</Section><Section title="Issue map"><button type="button" className="w-full rounded-card border border-dashed border-border bg-surface p-5 text-center text-body text-secondary" onClick={openMap}>Open issue map</button></Section></>}
  </MobileLayout>;
}

const typeOptions: readonly { value: BoardTypeFilter; label: string }[] = [{ value: "ALL", label: "All" }, { value: "LOCAL_AGENDA", label: "Agenda" }, { value: "LOCAL_ACTIVITY", label: "Activity" }, { value: "VOTE", label: "Vote" }];
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
    <section><h1 className="text-section">Community board</h1><div className="mt-3 flex gap-2 overflow-x-auto pb-1">{typeOptions.map((option) => <Chip key={option.value} selected={filters.type === option.value} onClick={() => change({ type: option.value })}>{option.label}</Chip>)}</div><div className="mt-2 flex gap-2 overflow-x-auto pb-1"><Chip selected={filters.topic === "ALL"} onClick={() => change({ topic: "ALL" })}>All</Chip>{boardTopics.map((topic) => <Chip key={topic} selected={filters.topic === topic} onClick={() => change({ topic: topic as BoardTopicFilter })}>{topic}</Chip>)}</div></section>
    <section aria-label="Recommendation" className="rounded-card bg-soft p-section"><h2 className="text-card-title">Personal recommendation</h2><p className="mt-1 text-body text-secondary">Recommendation results and refresh are connected in #97.</p><div className="mt-2 flex gap-3"><button type="button" className="text-caption text-primary underline" onClick={openRecommendations}>View recommendation</button><button type="button" className="text-caption text-primary underline" onClick={refreshRecommendations}>Refresh</button></div></section>
    {state.kind === "loading" && <div aria-busy="true" className="space-y-3"><div className="h-36 animate-pulse rounded-card bg-disabled" /><div className="h-36 animate-pulse rounded-card bg-disabled" /></div>}
    {state.kind === "error" && <Notice tone="error" role="alert"><p>{state.message}</p><Button className="mt-3" onClick={onRetry}>Retry</Button></Notice>}
    {state.kind === "empty" && <Notice>No posts match these filters.</Notice>}
    {state.kind === "success" && <><div className="space-y-3">{posts.length ? posts.map((post: PostDisplayModel) => <PostCard key={post.id} post={post} onOpen={open} />) : <Notice>No posts match these filters.</Notice>}</div>{state.data.pageInfo.nextCursor && <Button variant="secondary" onClick={onLoadMore}>Load more</Button>}</>}
  </MobileLayout>;
}
