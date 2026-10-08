"use client";
import { useMemo, useState } from "react";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Header } from "../../components/layout/Header";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useNavigation } from "../../lib/navigation";
import { PostDetailStateView } from "../post/PostDetail";
import { postFixtures } from "../post/mock";
import { ShareActions } from "./ShareActions";
import type { ShareService } from "./service";
import { SummaryPanel } from "../summary/SummaryPanel";
import { createSummaryMockService } from "../summary/mock";
import { CommentPanel } from "../comment/CommentPanel";
import { createCommentMockService } from "../comment/mock";
import { createEvaluationMockService } from "../comment/evaluation-mock";
import { VotePanel } from "../vote/VotePanel";
import { createVoteMockService } from "../vote/mock";

export function SharedPostScreen({ postId, shareService, mode = "ready" }: { postId: string; shareService: ShareService; mode?: "ready" | "loading" | "error" | "unavailable" }) {
  const navigation = useNavigation(); const post = postFixtures[postId];
  const [retry, setRetry] = useState(0);
  /** Preserve the opaque shared context during authentication; direct loads get a non-secret local reference. */
  const entry = navigation.state.current?.destination.id === "sharedPost" && navigation.state.current.destination.params.postId === postId
    ? navigation.state.current
    : { destination: { id: "sharedPost" as const, params: { postId } }, sharedContextRef: `shared:${postId}` };
  const state = useMemo(() => mode === "loading" ? { kind: "loading" as const } : mode === "error" ? { kind: "error" as const, message: retry ? "공유 게시물을 다시 불러오지 못했습니다." : "공유 게시물을 불러오지 못했습니다.", onRetry: () => setRetry(v => v + 1) } : !post || mode === "unavailable" ? { kind: "unavailable" as const, message: "삭제되었거나 더 이상 볼 수 없는 공유 게시물입니다." } : { kind: "success" as const, post: { ...post, viewer: { mode: "guest" as const }, capabilities: { ...post.capabilities, canBookmark: true, canReact: true, canVote: true, canReport: true } } }, [mode, post, retry]);
  const login = () => navigation.beginAuthentication(entry);
  const gate = <Notice tone="info"><p>회원 전용 기능입니다. 로그인 후 원 공유 상세로 돌아와 다시 눌러 주세요.</p><Button className="mt-2" onClick={login}>로그인·가입</Button></Notice>;
  const summary = state.kind === "success" && state.post.type === "LOCAL_AGENDA" ? <SummaryPanel post={state.post} service={createSummaryMockService()} /> : undefined;
  const comment = state.kind === "success" ? <CommentPanel postId={postId} service={createCommentMockService()} evaluationService={createEvaluationMockService()} permission={{ canCreate: true, viewerContext: "shared-guest" }} canEvaluate={false} evaluationRestrictionMessage="댓글 평가는 로그인 후 이용할 수 있습니다." /> : undefined;
  const vote = state.kind === "success" && state.post.type === "VOTE" ? <><VotePanel postId={postId} status={state.post.vote.status} service={createVoteMockService()} canVote={false} viewerMode="guest" /><Button className="mt-2" onClick={login}>투표하려면 로그인</Button></> : undefined;
  return <MobileLayout header={<Header title="공유 게시물" onBack={() => navigation.back()} />}><PostDetailStateView state={state} slots={{ share: state.kind === "success" ? <ShareActions postId={postId} service={shareService} /> : undefined, summary, comment, vote }} bookmark={state.kind === "success" ? { isBookmarked: false, onToggle: login } : undefined} onReport={login} />{state.kind === "success" && <section className="space-y-3"><div>{gate}</div></section>}</MobileLayout>;
}
