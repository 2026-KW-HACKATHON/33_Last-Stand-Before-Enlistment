"use client";
import { useMemo, useState } from "react";
import { MobileLayout } from "@/components/layout/MobileLayout";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { PostDetailStateView } from "@/features/post/PostDetail";
import { postFixtures } from "@/features/post/mock";
import { createShareMockService, type ShareMockMode } from "@/features/share/mock";
import { ShareActions } from "@/features/share/ShareActions";
import { GuestMemberGate } from "@/features/share/GuestMemberGate";
import { CommentPanel } from "@/features/comment/CommentPanel";
import { createCommentMockService } from "@/features/comment/mock";
import { createEvaluationMockService } from "@/features/comment/evaluation-mock";

export function SharedPreview() {
  const [mode, setMode] = useState<"ready" | "loading" | "error" | "unavailable">("ready"); const [copyMode, setCopyMode] = useState<ShareMockMode>("success"); const [loginPrompt, setLoginPrompt] = useState(false); const [returned, setReturned] = useState(false);
  const service = useMemo(() => createShareMockService(copyMode), [copyMode]); const post = postFixtures["agenda-photo"];
  const commentService = useMemo(() => createCommentMockService("success"), []); const evaluationService = useMemo(() => createEvaluationMockService("success"), []);
  const state = mode === "loading" ? { kind: "loading" as const } : mode === "error" ? { kind: "error" as const, message: "공유 게시물을 불러오지 못했습니다.", onRetry: () => setMode("ready") } : mode === "unavailable" ? { kind: "unavailable" as const, message: "삭제되었거나 더 이상 볼 수 없는 공유 게시물입니다." } : { kind: "success" as const, post: { ...post, viewer: { mode: "guest" as const }, capabilities: { ...post.capabilities, canBookmark: false, canReact: false, canVote: false, canReport: false } } };
  return <><div className="mx-auto max-w-mobile p-page"><div className="flex flex-wrap gap-2"><Button onClick={() => setMode("ready")}>공유 상세</Button><Button variant="secondary" onClick={() => setMode("loading")}>Loading</Button><Button variant="secondary" onClick={() => setMode("error")}>Error</Button><Button variant="secondary" onClick={() => setMode("unavailable")}>Unavailable</Button><Button variant={copyMode === "success" ? "primary" : "secondary"} onClick={() => setCopyMode(copyMode === "success" ? "error" : "success")}>복사 {copyMode}</Button></div></div><MobileLayout header={<Header title="공유 게시물" onBack={() => {}} />}><PostDetailStateView state={state} slots={{ share: mode === "ready" ? <ShareActions postId={post.id} service={service} /> : undefined }} />{mode === "ready" && <section className="space-y-3"><GuestMemberGate onLogin={() => { setLoginPrompt(true); }} /><CommentPanel postId={post.id} service={commentService} evaluationService={evaluationService} permission={{ canCreate: true, viewerContext: "shared-guest" }} canEvaluate={false} evaluationRestrictionMessage="댓글 평가는 로그인 후 이용할 수 있습니다." />{loginPrompt && <Notice>returnTo: 공유 상세을 보존했습니다. 로그인 후 원 상세로 돌아오며 행동은 자동 실행되지 않습니다.<Button className="mt-2" onClick={() => { setReturned(true); setLoginPrompt(false); }}>로그인 복귀 Mock</Button>{returned && <p className="mt-2">원 상세로 복귀했습니다. 회원 기능은 다시 눌러야 합니다.</p>}</Notice>}</section>}</MobileLayout></>;
}
