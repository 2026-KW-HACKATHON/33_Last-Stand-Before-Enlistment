"use client";
/* eslint-disable @next/next/no-img-element -- backend image host is intentionally not fixed before #13/#74. */

import type { MouseEvent } from "react";
import { authorLabel, postTypeLabel, type PostDisplayModel } from "./model";

export type PostCardProps = {
  post: PostDisplayModel;
  onOpen?: (postId: string) => void;
  className?: string;
};

export function openPost(onOpen: PostCardProps["onOpen"], postId: string) {
  onOpen?.(postId);
}

/** Reusable list/map/personal/search/recommendation card. It never owns bookmark state. */
export function PostCard({ post, onOpen, className = "" }: PostCardProps) {
  function open(event: MouseEvent<HTMLElement>) {
    if (event.currentTarget instanceof HTMLButtonElement) openPost(onOpen, post.id);
  }
  const firstImage = post.images[0];
  const body = <>
    <div className="flex flex-wrap gap-1 text-caption text-secondary">
      <span className="rounded-chip bg-soft px-2 py-0.5 text-primary">{postTypeLabel(post.type)}</span>
      <span>{post.metadata.regionName}</span><span aria-hidden="true">·</span><span>{post.metadata.topic}</span>
    </div>
    <h2 className="mt-2 text-card-title text-text">{post.title}</h2>
    <p className="mt-1 line-clamp-2 text-body text-secondary">{post.content}</p>
    {firstImage && <img src={firstImage.url} alt={firstImage.alt} className="mt-3 h-40 w-full rounded-card object-cover" />}
    <div className="mt-3 flex items-center justify-between gap-2 text-caption text-secondary">
      <span>{authorLabel(post)} · {post.metadata.createdAtLabel}</span>
      <span>반응 {post.reactionCount} · 댓글 {post.commentCount}</span>
    </div>
    {post.type === "LOCAL_ACTIVITY" && <p className="mt-2 text-caption text-primary">{post.activity.scheduleLabel} · {post.activity.status === "ENDED" ? "종료" : post.activity.status === "CANCELLED" ? "취소" : "진행 안내"}</p>}
    {post.type === "VOTE" && <p className="mt-2 text-caption text-primary">{post.vote.participationLabel}</p>}
  </>;
  const cardClass = `w-full rounded-card border border-border bg-surface p-section text-left shadow-card ${onOpen ? "cursor-pointer" : ""} ${className}`;
  return onOpen ? <button type="button" className={cardClass} onClick={open} aria-label={`${post.title} 상세 열기`}>{body}</button> : <article className={cardClass}>{body}</article>;
}
