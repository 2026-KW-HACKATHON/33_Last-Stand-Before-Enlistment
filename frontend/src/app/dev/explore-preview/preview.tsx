"use client";

import { useState } from "react";
import { BoardScreen, HomeScreen } from "@/features/explore/ExploreScreens";
import { explorePreviewContext } from "@/features/explore/mock";
import { applyExploreRegion, type BoardPage, type ExploreContext, type ExploreRegion, type HomeContent, type LoadState } from "@/features/explore/model";
import { postFixtures } from "@/features/post/mock";
import { Button } from "@/components/ui/Button";
import { SearchScreen } from "@/features/search/SearchScreen";
import { createSearchMockService, type SearchMockMode } from "@/features/search/mock";

const previewPosts = Object.values(postFixtures);
const homeData: HomeContent = { regionPosts: previewPosts.slice(0, 3), openVotes: previewPosts.filter((post) => post.type === "VOTE" && post.vote.status === "OPEN") };
const boardData: BoardPage = { posts: previewPosts, pageInfo: {} };
function forRegion<T extends HomeContent | BoardPage>(data: T, region: ExploreRegion): T {
  const posts = "regionPosts" in data ? data.regionPosts : data.posts;
  const mapped = posts.map((post) => ({ ...post, metadata: { ...post.metadata, regionName: region.name } }));
  return ("regionPosts" in data ? { ...data, regionPosts: mapped, openVotes: data.openVotes.map((post) => ({ ...post, metadata: { ...post.metadata, regionName: region.name } })) } : { ...data, posts: mapped }) as T;
}

export function ExplorePreview() {
  const [screen, setScreen] = useState<"home" | "board" | "search">("home");
  const [searchMode, setSearchMode] = useState<SearchMockMode>("success");
  const [context, setContext] = useState<ExploreContext>(explorePreviewContext);
  const home: LoadState<HomeContent> = { kind: "success", data: forRegion(homeData, context.currentExploreRegion) };
  const board: LoadState<BoardPage> = { kind: "success", data: forRegion(boardData, context.currentExploreRegion) };
  const changeRegion = (region: ExploreRegion) => setContext((current) => applyExploreRegion(current, region));
  const regionNames = Object.fromEntries(context.exploreRegionCandidates.map((region) => [region.id, region.name]));
  const search = <SearchScreen context={context} service={createSearchMockService(previewPosts, regionNames, searchMode)} onCancel={() => setScreen("board")} />;
  return <div className="min-h-dvh bg-background p-4"><div className="mx-auto mb-4 flex max-w-mobile gap-2 rounded-card border border-border bg-surface p-3"><Button className="min-h-0 px-3 py-1 text-caption" variant={screen === "home" ? "primary" : "secondary"} onClick={() => setScreen("home")}>Home</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={screen === "board" ? "primary" : "secondary"} onClick={() => setScreen("board")}>Board</Button><Button className="min-h-0 px-3 py-1 text-caption" variant="secondary" onClick={() => setSearchMode((current) => current === "success" ? "error" : "success")}>Search {searchMode}</Button></div>{screen === "search" ? search : screen === "home" ? <HomeScreen context={context} state={home} onExploreRegionChange={changeRegion} onSearch={() => setScreen("search")} /> : <BoardScreen context={context} state={board} onExploreRegionChange={changeRegion} onSearch={() => setScreen("search")} onRecommendations={() => window.alert("Recommendation is connected in #97.")} onNewRecommendation={() => window.alert("Refresh is connected in #97.")} />}</div>;
}
