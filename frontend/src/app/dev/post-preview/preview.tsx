"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { PostCard } from "@/features/post/PostCard";
import { PostDetailScreen } from "@/features/post/PostDetail";
import { postFixtures } from "@/features/post/mock";
import type { PostDetailState, PostDisplayModel } from "@/features/post/model";
import { createSummaryMockService, type SummaryMockMode } from "@/features/summary/mock";
import { SummaryPanel } from "@/features/summary/SummaryPanel";
import { CommentPanel } from "@/features/comment/CommentPanel";
import { createCommentMockService, type CommentMockMode } from "@/features/comment/mock";
import { createEvaluationMockService, type EvaluationMockMode } from "@/features/comment/evaluation-mock";
import { createReactionMockService, type ReactionMockMode } from "@/features/reaction/mock";
import { ReactionControls } from "@/features/reaction/ReactionControls";
import { createVoteMockService, type VoteMockMode } from "@/features/vote/mock";
import { VotePanel } from "@/features/vote/VotePanel";
import { SessionProvider, useSession } from "@/lib/navigation";
import { NeighborProvider } from "@/features/neighbor/provider";
import { useResidentParticipation } from "@/features/neighbor/participation";
import { createMockResidentParticipation, residentParticipationScenarios, type ResidentParticipationScenario } from "@/features/neighbor/participation-mock";
import { mockTargetRegion } from "@/features/neighbor/mock";

const choices = ["agenda-photo", "agenda-anonymous", "activity", "activity-ended", "vote", "vote-ended"] as const;
const choiceLabels: Record<(typeof choices)[number], string> = { "agenda-photo": "사진 있는 지역 안건", "agenda-anonymous": "익명 지역 안건", activity: "지역 활동 정보", "activity-ended": "종료된 활동", vote: "진행 중 투표", "vote-ended": "종료된 투표" };
const detailStateLabels = { success: "정상", restricted: "참여 제한", loading: "불러오는 중", error: "오류", unavailable: "삭제·미공개" } as const;
const summaryModeLabels: Record<SummaryMockMode, string> = { success: "요약 성공", "provider-error": "제공자 오류", timeout: "시간 초과", quota: "요청 한도", "source-too-short": "짧은 원문" };
const commentModeLabels: Record<CommentMockMode, string> = { success: "정상", empty: "댓글 없음", "load-error": "조회 오류", "create-error": "등록 실패", forbidden: "금칙어 오류" };
const evaluationModeLabels: Record<EvaluationMockMode, string> = { success: "평가 정상", "load-error": "평가 조회 오류", "set-error": "평가 실패" };
const voteModeLabels: Record<VoteMockMode, string> = { success: "투표 정상", "load-error": "조회 오류", "submit-error": "제출 실패 후 재시도", "unknown-result": "결과 불명 후 재조회", submitted: "기존 투표 있음" };

/** Development-only FE2 fixture harness. It is not a product route or a real Bookmark Service. */
function PostPreviewContent({ scenario, onScenarioChange }: { scenario: ResidentParticipationScenario; onScenarioChange: (scenario: ResidentParticipationScenario) => void }) {
  const [selected, setSelected] = useState<(typeof choices)[number]>("agenda-photo");
  const [bookmarked, setBookmarked] = useState(false);
  const [stateKind, setStateKind] = useState<"success" | "restricted" | "loading" | "error" | "unavailable">("success");
  const [summaryMode, setSummaryMode] = useState<SummaryMockMode>("success");
  const [commentMode, setCommentMode] = useState<CommentMockMode>("success");
  const [commentEnabled, setCommentEnabled] = useState(true);
  const [evaluationMode, setEvaluationMode] = useState<EvaluationMockMode>("success");
  const [evaluationEnabled, setEvaluationEnabled] = useState(true);
  const [reactionMode, setReactionMode] = useState<ReactionMockMode>("success");
  const [voteMode, setVoteMode] = useState<VoteMockMode>("success");
  const post = postFixtures[selected];
  const { session } = useSession();
  const participation = useResidentParticipation(mockTargetRegion.id);
  const viewer = session.status === "member" ? "member" : "guest";
  const visiblePost = useMemo<PostDisplayModel>(() => {
    const participationRestriction = participation.canParticipate ? undefined : "완료된 이웃 인증 지역에서만 주민 참여 기능을 사용할 수 있습니다.";
    return {
      ...post,
      viewer: { ...post.viewer, mode: viewer, participationRestriction },
      capabilities: {
        ...post.capabilities,
        canReact: participation.canParticipate,
        canComment: participation.canParticipate,
        canVote: participation.canParticipate,
      },
    } as PostDisplayModel;
  }, [participation.canParticipate, post, viewer]);
  const summaryService = useMemo(() => createSummaryMockService(summaryMode), [summaryMode]);
  const commentService = useMemo(() => createCommentMockService(commentMode), [commentMode]);
  const evaluationService = useMemo(() => createEvaluationMockService(evaluationMode), [evaluationMode]);
  const reactionService = useMemo(() => createReactionMockService(reactionMode), [reactionMode]);
  const voteService = useMemo(() => createVoteMockService(voteMode), [voteMode]);
  const state = useMemo<PostDetailState>(() => {
    if (stateKind === "loading") return { kind: "loading" };
    if (stateKind === "error") return { kind: "error", message: "Mock 조회 오류입니다.", onRetry: () => setStateKind("success") };
    if (stateKind === "unavailable") return { kind: "unavailable" };
    if (stateKind === "restricted") return { kind: "restricted", post: { ...visiblePost, viewer: { ...visiblePost.viewer, participationRestriction: "현재 탐색 지역에서는 열람만 가능합니다." } }, message: "현재 탐색 지역에서는 참여 기능이 제한됩니다." };
    return { kind: "success", post: visiblePost };
  }, [stateKind, visiblePost]);
  const summary = (state.kind === "success" || state.kind === "restricted") && visiblePost.type === "LOCAL_AGENDA"
    ? <SummaryPanel key={`${visiblePost.id}-${summaryMode}`} post={visiblePost} service={summaryService} />
    : undefined;
  const comment = (state.kind === "success" || state.kind === "restricted")
    ? <CommentPanel key={`${visiblePost.id}-${commentMode}-${evaluationMode}-${commentEnabled}-${evaluationEnabled}-${viewer}-${participation.canParticipate}`} postId={visiblePost.id} service={commentService} evaluationService={evaluationService} permission={{ canCreate: commentEnabled && participation.canParticipate, viewerContext: viewer === "guest" ? "shared-guest" : "member", restrictionMessage: "완료된 이웃 인증 지역에서만 댓글을 작성할 수 있습니다." }} canEvaluate={participation.canParticipate && evaluationEnabled} evaluationRestrictionMessage="완료된 이웃 인증 지역에서만 댓글 평가를 할 수 있습니다." onInvalidate={(postId) => window.alert(`댓글 목록 갱신 경계: ${postId}`)} />
    : undefined;
  const reaction = (state.kind === "success" || state.kind === "restricted")
    ? <ReactionControls key={`${visiblePost.id}-${reactionMode}-${viewer}-${participation.canParticipate}`} postId={visiblePost.id} service={reactionService} canReact={visiblePost.capabilities.canReact} onInvalidate={(postId) => window.alert(`반응 합계 갱신 경계: ${postId}`)} />
    : undefined;
  const vote = (state.kind === "success" || state.kind === "restricted") && visiblePost.type === "VOTE"
    ? <VotePanel key={`${visiblePost.id}-${voteMode}-${viewer}-${participation.canParticipate}`} postId={visiblePost.id} status={visiblePost.vote.status} service={voteService} canVote={visiblePost.capabilities.canVote} viewerMode={viewer} onInvalidate={(postId) => window.alert(`투표 결과 갱신 경계: ${postId}`)} />
    : undefined;
  return <div className="min-h-dvh bg-background p-4">
    <div className="mx-auto mb-4 max-w-mobile rounded-card border border-border bg-surface p-3">
      <p className="text-caption text-secondary">#53 게시물 상세 · #55 AI 요약 Preview</p>
      <div className="mt-2 flex flex-wrap gap-2">{choices.map((choice) => <Button key={choice} variant={selected === choice ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => { setSelected(choice); setBookmarked(false); setStateKind("success"); }}>{choiceLabels[choice]}</Button>)}</div>
      <div className="mt-2 flex flex-wrap gap-2">{(["success", "restricted", "loading", "error", "unavailable"] as const).map((kind) => <button key={kind} type="button" onClick={() => setStateKind(kind)} className="text-caption text-primary underline">{detailStateLabels[kind]}</button>)}</div>
      <div className="mt-2 flex flex-wrap items-center gap-2"><span className="text-caption text-secondary">주민 참여 권한 Mock</span>{residentParticipationScenarios.map((value) => <Button key={value} variant={scenario === value ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => onScenarioChange(value)}>{({ "completed-region": "완료 지역", "uncompleted-region": "미완료 지역", "other-region": "타 지역", "institution-only": "기관만 인증", "capabilities-loading": "권한 조회 중", "capabilities-error": "권한 조회 오류", "regions-error": "완료 지역 오류" } as const)[value]}</Button>)}</div>
      {participation.result.status === "loading" && <Notice className="mt-2" aria-busy="true">주민 참여 권한을 확인하고 있습니다.</Notice>}
      {participation.result.status === "error" && <Notice tone="error" className="mt-2">주민 참여 권한을 확인할 수 없습니다.<Button variant="secondary" className="ml-2 min-h-0 px-2 py-1" onClick={participation.retry}>다시 시도</Button></Notice>}
      {participation.result.status === "forbidden" && <Notice tone="info" className="mt-2">완료 지역·타 지역·기관 전용 상태에서는 주민 참여가 제한됩니다.</Notice>}
      <div className="mt-2 flex flex-wrap gap-2">{(["success", "load-error", "register-error", "cancel-error", "zero"] as const).map((mode) => <Button key={mode} variant={reactionMode === mode ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setReactionMode(mode)}>{({ success: "반응 정상", "load-error": "조회 오류", "register-error": "등록 실패", "cancel-error": "취소 실패", zero: "반응 0" } as const)[mode]}</Button>)}</div>
      {visiblePost.type === "LOCAL_AGENDA" && <div className="mt-2 flex flex-wrap gap-2">{(["success", "provider-error", "timeout", "quota", "source-too-short"] as const).map((mode) => <Button key={mode} variant={summaryMode === mode ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setSummaryMode(mode)}>{summaryModeLabels[mode]}</Button>)}</div>}
      <div className="mt-2 flex flex-wrap items-center gap-2"><span className="text-caption text-secondary">댓글 Mock</span>{(["success", "empty", "load-error", "create-error", "forbidden"] as const).map((mode) => <Button key={mode} variant={commentMode === mode ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setCommentMode(mode)}>{commentModeLabels[mode]}</Button>)}<Button variant={commentEnabled ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setCommentEnabled((current) => !current)}>{commentEnabled ? "댓글 작성 허용" : "댓글 작성 제한"}</Button></div>
      <div className="mt-2 flex flex-wrap items-center gap-2"><span className="text-caption text-secondary">평가 Mock</span>{(["success", "load-error", "set-error"] as const).map((mode) => <Button key={mode} variant={evaluationMode === mode ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setEvaluationMode(mode)}>{evaluationModeLabels[mode]}</Button>)}<Button variant={evaluationEnabled ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setEvaluationEnabled((current) => !current)}>{evaluationEnabled ? "평가 허용" : "평가 제한"}</Button></div>
      {visiblePost.type === "VOTE" && <div className="mt-2 flex flex-wrap items-center gap-2"><span className="text-caption text-secondary">투표 Mock</span>{(["success", "load-error", "submit-error", "unknown-result", "submitted"] as const).map((mode) => <Button key={mode} variant={voteMode === mode ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => setVoteMode(mode)}>{voteModeLabels[mode]}</Button>)}</div>}
      <div className="mt-3"><PostCard post={visiblePost} onOpen={() => setStateKind("success")} /></div>
    </div>
    <PostDetailScreen state={state} slots={{ summary, reaction, comment, vote }} bookmark={{ isBookmarked: bookmarked, feedback: bookmarked ? "북마크 상태는 FE1 Service가 연결되면 저장됩니다." : undefined, onToggle: () => setBookmarked((current) => !current) }} onReport={() => window.alert("신고 입력·제출은 #100에서 연결합니다.")} />
  </div>;
}

/** #62 harness: the mock adapters supply assertions; this screen cannot grant them itself. */
export function PostPreview() {
  const [scenario, setScenario] = useState<ResidentParticipationScenario>("completed-region");
  const context = useMemo(() => createMockResidentParticipation(scenario), [scenario]);
  return <SessionProvider session={context.session}><NeighborProvider subjectKey={context.subjectKey} service={context.service}>
    <PostPreviewContent scenario={scenario} onScenarioChange={setScenario} />
  </NeighborProvider></SessionProvider>;
}
