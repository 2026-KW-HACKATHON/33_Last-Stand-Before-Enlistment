"use client";

import { useState } from "react";
import { BoardScreen, HomeScreen } from "@/features/explore/ExploreScreens";
import { explorePreviewContext } from "@/features/explore/mock";
import { applyExploreRegion, type BoardPage, type ExploreContext, type ExploreRegion, type HomeContent, type LoadState } from "@/features/explore/model";
import { postFixtures } from "@/features/post/mock";
import { Button } from "@/components/ui/Button";
import { SearchScreen } from "@/features/search/SearchScreen";
import { createSearchMockService, type SearchMockMode } from "@/features/search/mock";
import { RecommendationScreen } from "@/features/recommendation/RecommendationScreen";
import { createRecommendationMockService, type RecommendationMockMode } from "@/features/recommendation/mock";

const previewPosts = Object.values(postFixtures);
const homeData: HomeContent = { regionPosts: previewPosts.slice(0, 3), openVotes: previewPosts.filter((post) => post.type === "VOTE" && post.vote.status === "OPEN") };
const boardData: BoardPage = { posts: previewPosts, pageInfo: {} };
function forRegion<T extends HomeContent | BoardPage>(data: T, region: ExploreRegion): T {
  const posts = "regionPosts" in data ? data.regionPosts : data.posts;
  const mapped = posts.map((post) => ({ ...post, metadata: { ...post.metadata, regionName: region.name } }));
  return ("regionPosts" in data ? { ...data, regionPosts: mapped, openVotes: data.openVotes.map((post) => ({ ...post, metadata: { ...post.metadata, regionName: region.name } })) } : { ...data, posts: mapped }) as T;
}

export function ExplorePreview() {
  const [screen, setScreen] = useState<"home" | "board" | "search" | "recommendation">("home");
  const [searchMode, setSearchMode] = useState<SearchMockMode>("success");
  const [recommendationMode, setRecommendationMode] = useState<RecommendationMockMode>("success");
  const [context, setContext] = useState<ExploreContext>(explorePreviewContext);
  const home: LoadState<HomeContent> = { kind: "success", data: forRegion(homeData, context.currentExploreRegion) };
  const board: LoadState<BoardPage> = { kind: "success", data: forRegion(boardData, context.currentExploreRegion) };
  const changeRegion = (region: ExploreRegion) => setContext((current) => applyExploreRegion(current, region));
  const regionNames = Object.fromEntries(context.exploreRegionCandidates.map((region) => [region.id, region.name]));
  const search = <SearchScreen context={context} service={createSearchMockService(previewPosts, regionNames, searchMode)} onCancel={() => setScreen("board")} />;
  const recommendationInput = { activityRegion: context.defaultActivityRegion, interestRegions: context.exploreRegionCandidates.filter((region) => region.id !== context.defaultActivityRegion.id), interestKeywords: ["환경"] as const };
  const selectRecommendationMode = (mode: RecommendationMockMode) => { setRecommendationMode(mode); setScreen("recommendation"); };
  const recommendation = <><div className="mx-auto mb-3 flex max-w-mobile flex-wrap gap-2 rounded-card border border-border bg-surface p-3"><Button className="min-h-0 px-3 py-1 text-caption" variant={recommendationMode === "success" ? "primary" : "secondary"} onClick={() => selectRecommendationMode("success")}>Recommendation success</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={recommendationMode === "no-alternative" ? "primary" : "secondary"} onClick={() => selectRecommendationMode("no-alternative")}>No alternative</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={recommendationMode === "empty" ? "primary" : "secondary"} onClick={() => selectRecommendationMode("empty")}>Recommendation empty</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={recommendationMode === "error" ? "primary" : "secondary"} onClick={() => selectRecommendationMode("error")}>Recommendation error</Button></div><RecommendationScreen input={recommendationInput} service={createRecommendationMockService(previewPosts, recommendationMode)} onBack={() => setScreen("board")} onOpenSummary={() => window.alert("AI summary is owned by #55.")} /></>;
  return <div className="min-h-dvh bg-background p-4"><div className="mx-auto mb-4 flex max-w-mobile flex-wrap gap-2 rounded-card border border-border bg-surface p-3"><Button className="min-h-0 px-3 py-1 text-caption" variant={screen === "home" ? "primary" : "secondary"} onClick={() => setScreen("home")}>Home</Button><Button className="min-h-0 px-3 py-1 text-caption" variant={screen === "board" ? "primary" : "secondary"} onClick={() => setScreen("board")}>Board</Button><Button className="min-h-0 px-3 py-1 text-caption" variant="secondary" onClick={() => setSearchMode((current) => current === "success" ? "error" : "success")}>Search {searchMode}</Button><Button className="min-h-0 px-3 py-1 text-caption" variant="secondary" onClick={() => setRecommendationMode((current) => current === "success" ? "no-alternative" : current === "no-alternative" ? "empty" : current === "empty" ? "error" : "success")}>Recommendation {recommendationMode}</Button></div>{screen === "search" ? search : screen === "recommendation" ? recommendation : screen === "home" ? <HomeScreen context={context} state={home} onExploreRegionChange={changeRegion} onSearch={() => setScreen("search")} /> : <BoardScreen context={context} state={board} onExploreRegionChange={changeRegion} onSearch={() => setScreen("recommendation")} onNewRecommendation={() => setScreen("recommendation")} />}</div>;
}
