"use client";
import { useCallback, useRef, useState } from "react";
import { PostDetailScreen, type BookmarkBinding } from "../post/PostDetail";
import { CommentPanel } from "../comment/CommentPanel";
import { ReactionControls } from "../reaction/ReactionControls";
import { VotePanel } from "../vote/VotePanel";
import { SummaryPanel } from "../summary/SummaryPanel";
import { ShareActions } from "../share/ShareActions";
import { PhotoDeletionStatus } from "./PhotoDeletionStatus";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useNavigation } from "../../lib/navigation";
import { useFeatureServices } from "./provider";
import type { FeatureServices } from "./index";
import { useResource, errorMessage } from "./use-resource";

export function PostHost({ postId, services: supplied, bookmarkBinding, onReturn }: { postId: string; services?: FeatureServices; bookmarkBinding?: BookmarkBinding; onReturn?: () => void }) {
  const registered = useFeatureServices(), services = supplied ?? registered;
  const navigation = useNavigation();
  const load = useCallback((signal: AbortSignal) => services!.postService.getPost(postId, signal), [services, postId]);
  const { state, retry } = useResource(services ? load : null);
  const [bookmark, setBookmark] = useState<{ pending: boolean; message: string; value?: boolean }>({ pending: false, message: "" });
  const busy = useRef(false);
  const bookmarkLoad = useCallback((signal: AbortSignal) => services!.bookmarkService.get("", postId, signal), [services, postId]);
  const bookmarkState = useResource(services && state.kind === "success" && state.data.viewer.mode === "member" ? bookmarkLoad : null);
  if (state.kind !== "success" || !services) return <PostDetailScreen onBack={onReturn} state={state.kind === "loading" ? state : state.kind === "unavailable" ? state : { kind: "error", message: "message" in state ? state.message : "게시물 연결 준비 중입니다.", onRetry: retry }} />;
  const post = state.data, guest = post.viewer.mode === "guest";
  const login = () => navigation.beginAuthentication({ destination: { id: guest ? "sharedPost" : "post", params: { postId } } });
  const currentBookmark = bookmarkState.state.kind === "success" ? bookmarkState.state.data : undefined;
  const toggle = async () => {
    if (busy.current || !currentBookmark?.available) return;
    busy.current = true; setBookmark({ pending: true, message: "저장 중" });
    try {
      const result = await services.bookmarkService.set("", postId, !(bookmark.value ?? currentBookmark.isBookmarked), new AbortController().signal);
      setBookmark({ pending: false, value: result.isBookmarked, message: result.isBookmarked ? "저장되었습니다" : "북마크를 해제했습니다." });
      bookmarkState.retry();
    } catch (error) { setBookmark(current => ({ ...current, pending: false, message: `${errorMessage(error)} 기존 상태를 유지했습니다.` })); }
    finally { busy.current = false; }
  };
  return <>
    <PostDetailScreen onBack={onReturn} state={{ kind: "success", post }} bookmark={bookmarkBinding ?? (!guest ? { isBookmarked: bookmark.value ?? currentBookmark?.isBookmarked ?? false, pending: bookmark.pending, feedback: bookmark.message || (bookmarkState.state.kind === "error" ? bookmarkState.state.message : ""), onToggle: currentBookmark ? () => void toggle() : undefined } : undefined)} slots={{
      summary: post.type === "LOCAL_AGENDA" ? <SummaryPanel post={post} service={services.summaryService} /> : undefined,
      reaction: !guest ? <ReactionControls postId={postId} service={services.reactionService} canReact={post.capabilities.canReact} onInvalidate={retry} /> : undefined,
      comment: <CommentPanel postId={postId} service={services.commentService} evaluationService={services.evaluationService} permission={{ canCreate: post.capabilities.canComment, viewerContext: guest ? "shared-guest" : "member" }} canEvaluate={!guest && !!post.capabilities.canEvaluateComment} onInvalidate={retry} />,
      vote: post.type === "VOTE" ? <VotePanel postId={postId} status={post.vote.status} service={services.voteService} canVote={post.capabilities.canVote} viewerMode={post.viewer.mode} onInvalidate={retry} /> : undefined,
      share: post.capabilities.canShare ? <ShareActions postId={postId} service={services.shareService} /> : undefined,
    }} />
    <PhotoDeletionStatus postId={postId} services={services} />
    {post.capabilities.canEdit && <Button onClick={() => navigation.navigate({ destination: { id: "editPost", params: { postId } }, origin: { id: "post", params: { postId } } })}>게시물 수정·삭제</Button>}
    {guest && <Notice><p>반응·평가·투표·북마크는 로그인 후 다시 눌러 주세요.</p><Button onClick={login}>로그인·가입</Button></Notice>}
    {!guest && bookmarkState.state.kind === "error" && <Button variant="secondary" onClick={bookmarkState.retry}>북마크 상태 다시 조회</Button>}
  </>;
}
