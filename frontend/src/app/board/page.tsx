"use client";

import { BoardScreen } from "@/features/explore/ExploreScreens";
import type { BoardPage, ExploreContext } from "@/features/explore/model";

const unavailableContext: ExploreContext = { defaultActivityRegion: { id: "pending", name: "활동 지역", neighborVerified: false }, currentExploreRegion: { id: "pending", name: "탐색 지역", neighborVerified: false }, exploreRegionCandidates: [], neighborVerifiedRegions: [], institutionRegions: [] };
const unavailable = { kind: "error", message: "게시판 데이터를 연결할 준비 중입니다. 실제 API 연동 전에는 표시할 수 없습니다." } as const;
export default function BoardPage() { return <BoardScreen context={unavailableContext} state={unavailable satisfies import("@/features/explore/model").LoadState<BoardPage>} />; }
