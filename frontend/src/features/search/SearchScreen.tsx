"use client";

import { useRef, useState, type ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useNavigation } from "../../lib/navigation";
import { boardTopics, type BoardTopicFilter, type BoardTypeFilter, type ExploreContext } from "../explore/model";
import { PostCard } from "../post/PostCard";
import { emptySearchFilters, searchSnapshotReference, type SearchFilters, type SearchResult, type SearchStatus } from "./model";
import type { SearchService } from "./service";

type SearchScreenProps = {
  context: ExploreContext;
  service: SearchService;
  onCancel: () => void;
  onOpenPost?: (postId: string, snapshotRef: string) => void;
};
const types: readonly { value: BoardTypeFilter; label: string }[] = [{ value: "ALL", label: "All" }, { value: "LOCAL_AGENDA", label: "Agenda" }, { value: "LOCAL_ACTIVITY", label: "Activity" }, { value: "VOTE", label: "Vote" }];
function Chip({ selected, children, onClick }: { selected: boolean; children: ReactNode; onClick: () => void }) { return <button type="button" onClick={onClick} className={`shrink-0 rounded-chip border px-3 py-1.5 text-caption ${selected ? "border-primary bg-primary text-surface" : "border-border bg-surface text-secondary"}`}>{children}</button>; }

export function SearchScreen({ context, service, onCancel, onOpenPost }: SearchScreenProps) {
  const navigation = useNavigation();
  const [filters, setFilters] = useState<SearchFilters>(() => emptySearchFilters(context.currentExploreRegion.id));
  const [status, setStatus] = useState<SearchStatus>("idle");
  const [result, setResult] = useState<SearchResult | null>(null);
  const [message, setMessage] = useState("");
  const scrollTop = useRef(0);
  const request = async () => {
    setStatus("loading"); setMessage("");
    try { const next = await service.search(filters); setResult(next); setStatus(next.posts.length ? "success" : "empty"); }
    catch { setStatus("error"); setMessage("Search results could not be loaded. Please retry."); }
  };
  const update = (next: Partial<SearchFilters>) => { setFilters((current) => ({ ...current, ...next })); setStatus("idle"); setResult(null); scrollTop.current = 0; };
  const open = (postId: string) => {
    const snapshot = { filters: { ...filters, regionId: context.currentExploreRegion.id }, result, scrollTop: scrollTop.current };
    const ref = searchSnapshotReference(snapshot);
    navigation.registerSnapshot({ destination: { id: "search" }, kind: "search", ref });
    onOpenPost?.(postId, ref);
    navigation.navigate({ destination: { id: "post", params: { postId } }, origin: { id: "search" }, sharedContextRef: ref });
  };
  return <MobileLayout header={<Header title="Search" onBack={onCancel} />}><div onScroll={(event) => { scrollTop.current = event.currentTarget.scrollTop; }}>
    <form onSubmit={(event) => { event.preventDefault(); void request(); }}><label className="sr-only" htmlFor="free-search">Search posts</label><input id="free-search" value={filters.query} onChange={(event) => update({ query: event.target.value })} className="w-full rounded-input border border-border bg-surface px-3 py-3 text-body" placeholder="Search regional stories" /><Button className="mt-2 w-full" type="submit" disabled={status === "loading"}>Search</Button></form>
    <section className="mt-4"><p className="text-caption text-secondary">Region</p><div className="mt-2 flex gap-2 overflow-x-auto pb-1">{context.exploreRegionCandidates.map((region) => <Chip key={region.id} selected={filters.regionId === region.id} onClick={() => update({ regionId: region.id })}>{region.name}</Chip>)}</div><p className="mt-3 text-caption text-secondary">Type</p><div className="mt-2 flex gap-2 overflow-x-auto pb-1">{types.map((item) => <Chip key={item.value} selected={filters.type === item.value} onClick={() => update({ type: item.value })}>{item.label}</Chip>)}</div><p className="mt-3 text-caption text-secondary">Topic</p><div className="mt-2 flex gap-2 overflow-x-auto pb-1"><Chip selected={filters.topic === "ALL"} onClick={() => update({ topic: "ALL" })}>All</Chip>{boardTopics.map((topic) => <Chip key={topic} selected={filters.topic === topic} onClick={() => update({ topic: topic as BoardTopicFilter })}>{topic}</Chip>)}</div></section>
    {status === "idle" && <Notice className="mt-4">Enter a term or set filters, then search.</Notice>}
    {status === "loading" && <div aria-busy="true" className="mt-4 space-y-3"><div className="h-32 animate-pulse rounded-card bg-disabled" /><div className="h-32 animate-pulse rounded-card bg-disabled" /></div>}
    {status === "error" && <Notice tone="error" role="alert" className="mt-4"><p>{message}</p><Button className="mt-3" onClick={() => void request()}>Retry</Button></Notice>}
    {status === "empty" && <Notice className="mt-4">No posts match these conditions.</Notice>}
    {status === "success" && <div className="mt-4 space-y-3">{result?.posts.map((post) => <PostCard key={post.id} post={post} onOpen={open} />)}</div>}
  </div></MobileLayout>;
}
