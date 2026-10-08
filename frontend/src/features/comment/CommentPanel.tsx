"use client";

import { useEffect, useMemo, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { TextArea } from "../../components/ui/TextArea";
import { emptyEvaluation, nextEvaluation, type EvaluationSnapshot, type EvaluationState, type EvaluationType } from "./evaluation";
import type { EvaluationService } from "./evaluation-service";
import type { CommentDisplay, CommentPermission, CommentThread } from "./model";
import type { CommentService } from "./service";

type ReplyTarget = { parentCommentId: string; targetAuthorName: string };
type FailedAction = { kind: "root" | "reply"; input: string; target?: ReplyTarget };
type FailedEvaluation = { commentId: string; requested: Exclude<EvaluationType, "NONE">; before: EvaluationSnapshot };
type Props = { postId: string; service: CommentService; evaluationService: EvaluationService; permission: CommentPermission; canEvaluate: boolean; evaluationRestrictionMessage?: string; onInvalidate?: (postId: string) => void };

export function CommentPanel({ postId, service, evaluationService, permission, canEvaluate, evaluationRestrictionMessage, onInvalidate }: Props) {
  const [threads, setThreads] = useState<readonly CommentThread[] | undefined>();
  const [evaluations, setEvaluations] = useState<EvaluationState>({});
  const [loadError, setLoadError] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [reloadVersion, setReloadVersion] = useState(0);
  const [rootInput, setRootInput] = useState("");
  const [replyTarget, setReplyTarget] = useState<ReplyTarget>();
  const [replyInput, setReplyInput] = useState("");
  const [pending, setPending] = useState(false);
  const [pendingEvaluations, setPendingEvaluations] = useState<string[]>([]);
  const [failedAction, setFailedAction] = useState<FailedAction>();
  const [failedEvaluation, setFailedEvaluation] = useState<FailedEvaluation>();
  const [feedback, setFeedback] = useState<string>();
  const [evaluationFeedback, setEvaluationFeedback] = useState<string>();

  useEffect(() => {
    let active = true;
    void Promise.all([service.list(postId), evaluationService.list(postId)]).then(([commentThreads, snapshots]) => {
      if (!active) return;
      setThreads(commentThreads);
      setEvaluations(Object.fromEntries(snapshots.map((snapshot) => [snapshot.commentId, snapshot])));
      setLoadError(undefined); setLoading(false);
    }).catch((error: unknown) => {
      if (!active) return;
      setLoadError(error instanceof Error ? error.message : "댓글을 불러오지 못했습니다."); setLoading(false);
    });
    return () => { active = false; };
  }, [evaluationService, postId, reloadVersion, service]);

  const retryLoad = () => { setThreads(undefined); setLoadError(undefined); setLoading(true); setReloadVersion((current) => current + 1); };
  const sortedThreads = useMemo(() => threads ? [...threads].sort((left, right) => {
    const likeDifference = (evaluations[right.root.id]?.likeCount ?? 0) - (evaluations[left.root.id]?.likeCount ?? 0);
    return likeDifference || right.root.createdAtOrder - left.root.createdAtOrder;
  }) : undefined, [evaluations, threads]);
  const append = (comment: CommentDisplay) => {
    setEvaluations((current) => ({ ...current, [comment.id]: emptyEvaluation(comment.id) }));
    setThreads((current = []) => comment.parentCommentId
      ? current.map((thread) => thread.root.id === comment.parentCommentId ? { ...thread, replies: [...thread.replies, comment] } : thread)
      : [...current, { root: comment, replies: [] }]);
  };
  const submit = async (action: FailedAction) => {
    if (pending || !action.input.trim() || !permission.canCreate) return;
    setPending(true); setFailedAction(undefined); setFeedback(undefined);
    try {
      const created = action.kind === "root"
        ? await service.create(postId, { content: action.input })
        : await service.createReply(postId, { content: action.input, parentCommentId: action.target!.parentCommentId, targetAuthorName: action.target!.targetAuthorName });
      append(created);
      if (action.kind === "root") setRootInput(""); else { setReplyInput(""); setReplyTarget(undefined); }
      setFeedback(action.kind === "root" ? "댓글이 등록되었습니다." : "답글이 등록되었습니다."); onInvalidate?.(postId);
    } catch (error) { setFailedAction(action); setFeedback(error instanceof Error ? error.message : "댓글을 등록하지 못했습니다."); }
    finally { setPending(false); }
  };
  const evaluate = async (commentId: string, requested: Exclude<EvaluationType, "NONE">, retry?: FailedEvaluation) => {
    if (pendingEvaluations.includes(commentId) || !canEvaluate) return;
    const before = retry?.before ?? evaluations[commentId] ?? emptyEvaluation(commentId);
    const optimistic = nextEvaluation(before, requested);
    setEvaluations((current) => ({ ...current, [commentId]: optimistic }));
    setPendingEvaluations((current) => [...current, commentId]); setFailedEvaluation(undefined); setEvaluationFeedback(undefined);
    try {
      const result = await evaluationService.set(commentId, optimistic.selected);
      setEvaluations((current) => ({ ...current, [commentId]: result }));
      setEvaluationFeedback(optimistic.selected === "NONE" ? "평가를 취소했습니다." : "평가가 반영되었습니다."); onInvalidate?.(postId);
    } catch (error) {
      setEvaluations((current) => ({ ...current, [commentId]: before }));
      setFailedEvaluation({ commentId, requested, before }); setEvaluationFeedback(error instanceof Error ? error.message : "평가를 반영하지 못했습니다.");
    } finally { setPendingEvaluations((current) => current.filter((id) => id !== commentId)); }
  };
  const beginReply = (parentCommentId: string, targetAuthorName: string) => { setReplyTarget({ parentCommentId, targetAuthorName }); setReplyInput(""); setFeedback(undefined); setFailedAction(undefined); };

  return <section className="rounded-card border border-border bg-background p-section" aria-label="댓글">
    <h2 className="text-card-title">댓글</h2>
    {loading && <p className="mt-3 text-body text-secondary" aria-busy="true">댓글을 불러오는 중입니다.</p>}
    {loadError && <Notice tone="error" className="mt-3"><p>{loadError}</p><Button className="mt-3" onClick={retryLoad}>다시 시도</Button></Notice>}
    {sortedThreads?.length === 0 && <p className="mt-3 text-body text-secondary">아직 댓글이 없습니다.</p>}
    {sortedThreads?.map((thread) => <CommentThreadView key={thread.root.id} thread={thread} evaluations={evaluations} canEvaluate={canEvaluate} evaluationPending={pendingEvaluations} disabled={!permission.canCreate || pending} onEvaluate={evaluate} onReply={beginReply} />)}
    {!canEvaluate && <Notice tone="info" className="mt-3">{evaluationRestrictionMessage ?? "현재 댓글 평가를 할 수 없습니다."}</Notice>}
    {evaluationFeedback && <Notice tone={failedEvaluation ? "error" : "info"} className="mt-3" role="status"><p>{evaluationFeedback}</p>{failedEvaluation && <Button className="mt-3" onClick={() => void evaluate(failedEvaluation.commentId, failedEvaluation.requested, failedEvaluation)}>다시 시도</Button>}</Notice>}
    {!permission.canCreate && <Notice tone="info" className="mt-3">{permission.restrictionMessage ?? "현재 댓글을 작성할 수 없습니다."}</Notice>}
    {permission.canCreate && <div className="mt-4 space-y-2">
      {replyTarget && <div className="rounded-notice bg-soft p-section"><p className="text-caption text-secondary">{replyTarget.targetAuthorName}님에게 답글</p><TextArea label="답글" value={replyInput} onChange={(event) => setReplyInput(event.target.value)} placeholder="답글을 입력해 주세요." disabled={pending} /><div className="mt-2 flex gap-2"><Button disabled={pending || !replyInput.trim()} onClick={() => void submit({ kind: "reply", input: replyInput, target: replyTarget })}>{pending ? "등록 중" : "답글 등록"}</Button><Button variant="secondary" disabled={pending} onClick={() => setReplyTarget(undefined)}>취소</Button></div></div>}
      <TextArea label="댓글" value={rootInput} onChange={(event) => setRootInput(event.target.value)} placeholder="댓글을 입력해 주세요." disabled={pending} />
      <Button disabled={pending || !rootInput.trim()} onClick={() => void submit({ kind: "root", input: rootInput })}>{pending ? "등록 중" : "댓글 등록"}</Button>
    </div>}
    {feedback && <Notice tone={failedAction ? "error" : "info"} className="mt-3" role="status"><p>{feedback}</p>{failedAction && <Button className="mt-3" onClick={() => void submit(failedAction)}>다시 시도</Button>}</Notice>}
  </section>;
}

function CommentThreadView({ thread, evaluations, canEvaluate, evaluationPending, disabled, onEvaluate, onReply }: { thread: CommentThread; evaluations: EvaluationState; canEvaluate: boolean; evaluationPending: readonly string[]; disabled: boolean; onEvaluate: (commentId: string, type: Exclude<EvaluationType, "NONE">) => void; onReply: (parentCommentId: string, targetAuthorName: string) => void }) {
  return <div className="mt-4 border-t border-border pt-3"><CommentItem comment={thread.root} evaluation={evaluations[thread.root.id] ?? emptyEvaluation(thread.root.id)} canEvaluate={canEvaluate} evaluationPending={evaluationPending.includes(thread.root.id)} disabled={disabled} onEvaluate={onEvaluate} onReply={() => onReply(thread.root.id, thread.root.authorName)} />
    {thread.replies.map((reply) => <div key={reply.id} className="ml-4 mt-3 border-l-2 border-soft pl-3"><CommentItem comment={reply} evaluation={evaluations[reply.id] ?? emptyEvaluation(reply.id)} canEvaluate={canEvaluate} evaluationPending={evaluationPending.includes(reply.id)} disabled={disabled} onEvaluate={onEvaluate} onReply={() => onReply(thread.root.id, reply.authorName)} /></div>)}
  </div>;
}

function CommentItem({ comment, evaluation, canEvaluate, evaluationPending, disabled, onEvaluate, onReply }: { comment: CommentDisplay; evaluation: EvaluationSnapshot; canEvaluate: boolean; evaluationPending: boolean; disabled: boolean; onEvaluate: (commentId: string, type: Exclude<EvaluationType, "NONE">) => void; onReply: () => void }) {
  return <article><div className="flex items-center justify-between gap-2 text-caption text-secondary"><span>{comment.authorName}</span><span>{comment.createdAtLabel}</span></div>{comment.targetAuthorName && <p className="mt-1 text-caption text-secondary">{comment.targetAuthorName}님에게 답글</p>}<p className="mt-1 whitespace-pre-wrap text-body">{comment.content}</p><div className="mt-3 flex items-center justify-between gap-3"><button type="button" className="text-caption text-primary underline disabled:text-muted" disabled={disabled} onClick={onReply}>답글</button><div className="flex items-center gap-1"><Button variant={evaluation.selected === "LIKE" ? "primary" : "secondary"} className="min-h-0 border-0 bg-transparent px-1 py-1 text-caption text-secondary" disabled={!canEvaluate || evaluationPending} onClick={() => void onEvaluate(comment.id, "LIKE")}>{evaluationPending ? "처리 중" : `좋아요 ${evaluation.likeCount}`}</Button><Button variant={evaluation.selected === "DISLIKE" ? "primary" : "secondary"} className="min-h-0 border-0 bg-transparent px-1 py-1 text-caption text-secondary" disabled={!canEvaluate || evaluationPending} onClick={() => void onEvaluate(comment.id, "DISLIKE")}>{evaluationPending ? "처리 중" : `싫어요 ${evaluation.dislikeCount}`}</Button></div></div></article>;
}
