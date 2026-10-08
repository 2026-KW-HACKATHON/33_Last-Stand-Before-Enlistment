"use client";
/* eslint-disable @next/next/no-img-element -- backend image host is intentionally not fixed before #13/#74. */

import type { ReactNode } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { useNavigation } from "../../lib/navigation";
import { activityStatusLabel, authorLabel, isParticipationLinkAvailable, postTypeLabel, type PostDetailState, type PostDisplayModel } from "./model";

export type PostDetailSlots = {
  summary?: ReactNode;
  reaction?: ReactNode;
  comment?: ReactNode;
  vote?: ReactNode;
  share?: ReactNode;
  recommendation?: ReactNode;
};
export type BookmarkBinding = {
  isBookmarked: boolean;
  pending?: boolean;
  feedback?: string;
  onToggle?: () => void;
};
export type PostDetailProps = {
  post: PostDisplayModel;
  slots?: PostDetailSlots;
  bookmark?: BookmarkBinding;
  onReport?: (postId: string) => void;
  className?: string;
};

function Meta({ post }: { post: PostDisplayModel }) {
  return <>
    <div className="flex flex-wrap items-center gap-1.5 text-caption text-secondary">
      <span className="rounded-chip bg-soft px-2 py-0.5 leading-none text-primary">{postTypeLabel(post.type)}</span>
      <span>{post.metadata.regionName}</span><span aria-hidden="true">·</span><span>{post.metadata.topic}</span>
    </div>
    <div className="mt-3 flex items-center justify-between gap-2 text-caption text-secondary">
      <span>{authorLabel(post)}{post.author.badge === "institution" ? " · 기관" : ""}{post.author.institutionName ? ` · ${post.author.institutionName}` : ""}</span>
      <span>{post.metadata.createdAtLabel}{post.metadata.updatedAtLabel ? ` · 수정 ${post.metadata.updatedAtLabel}` : ""}</span>
    </div>
  </>;
}

function ActivityExtension({ post }: { post: Extract<PostDisplayModel, { type: "LOCAL_ACTIVITY" }> }) {
  const activeLink = isParticipationLinkAvailable(post);
  return <section aria-label="지역 활동 정보" className="rounded-card border border-border bg-background p-section">
    <h2 className="text-card-title">활동 정보</h2>
    <dl className="mt-2 grid grid-cols-[72px_1fr] gap-y-1 text-body">
      <dt className="text-secondary">출처</dt><dd>{post.activity.source}</dd>
      <dt className="text-secondary">일정</dt><dd>{post.activity.scheduleLabel}</dd>
      <dt className="text-secondary">장소</dt><dd>{post.activity.location}</dd>
      <dt className="text-secondary">상태</dt><dd>{activityStatusLabel(post.activity.status)}</dd>
      <dt className="text-secondary">문의</dt><dd>{post.activity.contactEmail ?? "주최자 문의 정보를 확인할 수 없습니다."}</dd>
    </dl>
    {post.activity.participationLink && (activeLink
      ? <a className="mt-3 inline-flex text-button text-primary underline" href={post.activity.participationLink.href} target="_blank" rel="noreferrer">{post.activity.participationLink.label}</a>
      : <Notice tone="info" className="mt-3">종료 또는 취소된 활동은 외부 참여 경로를 제공하지 않습니다.</Notice>)}
  </section>;
}

function VoteExtension({ post, slot }: { post: Extract<PostDisplayModel, { type: "VOTE" }>; slot?: ReactNode }) {
  return <section aria-label="투표 정보" className="rounded-card border border-border bg-background p-section">
    <div className="flex items-center justify-between gap-2"><h2 className="text-card-title">투표</h2><span className="text-caption text-primary">{post.vote.status === "OPEN" ? "진행 중" : "종료"}</span></div>
    <p className="mt-2 text-body text-secondary">{post.vote.participationLabel}</p>
    {slot && <div className="mt-3">{slot}</div>}
  </section>;
}

/** Detail composition only; reaction/comment/vote/share/summary/report flows remain feature-owned slots. */
export function PostDetail({ post, slots = {}, bookmark, onReport, className = "" }: PostDetailProps) {
  return <article className={`flex flex-col gap-section ${className}`}>
    <Meta post={post} />
    <div><h1 className="text-section text-text">{post.title}</h1><p className="mt-3 whitespace-pre-wrap text-body text-text">{post.content}</p></div>
    {post.images.map((image) => <img key={image.id} src={image.url} alt={image.alt} className="w-full rounded-card object-cover" />)}
    {post.referenceLink && <a href={post.referenceLink.href} target="_blank" rel="noreferrer" className="text-button text-primary underline">{post.referenceLink.label}</a>}
    {post.type === "LOCAL_ACTIVITY" && <ActivityExtension post={post} />}
    {post.type === "VOTE" && <VoteExtension post={post} slot={slots.vote} />}
    {slots.summary && <section aria-label="AI 요약 영역">{slots.summary}</section>}
    {post.viewer.participationRestriction && <Notice tone="info">{post.viewer.participationRestriction}</Notice>}
    {(slots.reaction || slots.recommendation || slots.share) && <section aria-label="참여 영역" className="flex flex-col gap-internal">{slots.reaction}{slots.recommendation}{slots.share}</section>}
    {post.adoptions.length > 0 && <section aria-label="기관 채택" className="rounded-card bg-soft p-section"><h2 className="text-card-title">기관 채택</h2>{post.adoptions.map((adoption) => <p key={`${adoption.institutionName}-${adoption.adoptedAtLabel}`} className="mt-1 text-body">{adoption.institutionName} · {adoption.adoptedAtLabel}</p>)}</section>}
    <section aria-label="게시물 행동" className="flex flex-wrap gap-internal border-t border-border pt-section">
      {post.capabilities.canBookmark && bookmark && <Button variant="secondary" disabled={bookmark.pending || !bookmark.onToggle} onClick={bookmark.onToggle}>{bookmark.isBookmarked ? "북마크 해제" : "북마크 저장"}</Button>}
      {post.capabilities.canReport && onReport && <button type="button" className="px-2 text-caption text-secondary underline" onClick={() => onReport(post.id)}>신고</button>}
      {bookmark?.feedback && <span role="status" className="self-center text-caption text-primary">{bookmark.feedback}</span>}
    </section>
    {slots.comment && <section aria-label="댓글 영역">{slots.comment}</section>}
  </article>;
}

export function PostDetailStateView({ state, slots, bookmark, onReport }: {
  state: PostDetailState; slots?: PostDetailSlots; bookmark?: BookmarkBinding; onReport?: (postId: string) => void;
}) {
  if (state.kind === "loading") return <div aria-busy="true" aria-label="게시물을 불러오는 중" className="animate-pulse space-y-3"><div className="h-5 w-1/3 rounded bg-disabled" /><div className="h-8 w-4/5 rounded bg-disabled" /><div className="h-28 rounded-card bg-disabled" /></div>;
  if (state.kind === "error") return <Notice tone="error" role="alert"><p>{state.message}</p>{state.onRetry && <Button className="mt-3" onClick={state.onRetry}>다시 시도</Button>}</Notice>;
  if (state.kind === "unavailable") return <Notice tone="warning" role="status">{state.message ?? "삭제되었거나 더 이상 볼 수 없는 게시물입니다."}</Notice>;
  return <>{state.kind === "restricted" && <Notice tone="info">{state.message}</Notice>}<PostDetail post={state.post} slots={slots} bookmark={bookmark} onReport={onReport} /></>;
}

/** Page owner shell. It consumes #40's navigation callback without redefining origin/snapshots. */
export function PostDetailScreen({ state, slots, bookmark, onReport, onBack }: {
  state: PostDetailState; slots?: PostDetailSlots; bookmark?: BookmarkBinding; onReport?: (postId: string) => void; onBack?: () => void;
}) {
  const navigation = useNavigation();
  return <MobileLayout header={<Header title="게시물 상세" onBack={onBack ?? (() => navigation.back())} />}>
    <PostDetailStateView state={state} slots={slots} bookmark={bookmark} onReport={onReport} />
  </MobileLayout>;
}
