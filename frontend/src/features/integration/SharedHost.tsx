"use client";
import { useMemo } from "react";
import { useFeatureServices } from "./provider";
import { PostHost } from "./PostHost";
import { Notice } from "../../components/ui/Notice";
export function SharedHost({ postId, token }: { postId: string; token: string | null }) {
  const services = useFeatureServices();
  const scoped = useMemo(() => {
    if (!services || !token) return null;
    try { return services.createSharedServices(postId, token); } catch { return null; }
  }, [services, postId, token]);
  if (!scoped) return <Notice tone="warning">{!services ? "공유 API 연결 준비 중입니다." : "유효한 공유 링크가 필요합니다."}</Notice>;
  return <PostHost key={`${postId}:${token}`} postId={postId} services={scoped} />;
}
