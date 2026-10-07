"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { PostCard } from "@/features/post/PostCard";
import { PostDetailScreen } from "@/features/post/PostDetail";
import { postFixtures } from "@/features/post/mock";
import type { PostDetailState } from "@/features/post/model";

const choices = ["agenda-photo", "agenda-anonymous", "activity", "activity-ended", "vote", "vote-ended"] as const;

/** Development-only FE2 fixture harness. It is not a product route or a real Bookmark Service. */
export function PostPreview() {
  const [selected, setSelected] = useState<(typeof choices)[number]>("agenda-photo");
  const [bookmarked, setBookmarked] = useState(false);
  const [stateKind, setStateKind] = useState<"success" | "restricted" | "loading" | "error" | "unavailable">("success");
  const post = postFixtures[selected];
  const state = useMemo<PostDetailState>(() => {
    if (stateKind === "loading") return { kind: "loading" };
    if (stateKind === "error") return { kind: "error", message: "Mock 조회 오류입니다.", onRetry: () => setStateKind("success") };
    if (stateKind === "unavailable") return { kind: "unavailable" };
    if (stateKind === "restricted") return { kind: "restricted", post: { ...post, viewer: { ...post.viewer, participationRestriction: "현재 탐색 지역에서는 열람만 가능합니다." } }, message: "현재 탐색 지역에서는 참여 기능이 제한됩니다." };
    return { kind: "success", post };
  }, [post, stateKind]);
  return <div className="min-h-dvh bg-background p-4">
    <div className="mx-auto mb-4 max-w-mobile rounded-card border border-border bg-surface p-3">
      <p className="text-caption text-secondary">#53 개발 전용 Post Preview</p>
      <div className="mt-2 flex flex-wrap gap-2">{choices.map((choice) => <Button key={choice} variant={selected === choice ? "primary" : "secondary"} className="min-h-0 px-3 py-1 text-caption" onClick={() => { setSelected(choice); setBookmarked(false); setStateKind("success"); }}>{choice}</Button>)}</div>
      <div className="mt-2 flex flex-wrap gap-2">{(["success", "restricted", "loading", "error", "unavailable"] as const).map((kind) => <button key={kind} type="button" onClick={() => setStateKind(kind)} className="text-caption text-primary underline">{kind}</button>)}</div>
      <div className="mt-3"><PostCard post={post} onOpen={() => setStateKind("success")} /></div>
    </div>
    <PostDetailScreen state={state} bookmark={{ isBookmarked: bookmarked, feedback: bookmarked ? "북마크 상태는 FE1 Service가 연결되면 저장됩니다." : undefined, onToggle: () => setBookmarked((current) => !current) }} onReport={() => window.alert("신고 입력·제출은 #100에서 연결합니다.")} />
  </div>;
}
