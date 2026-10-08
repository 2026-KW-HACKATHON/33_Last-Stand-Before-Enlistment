"use client";
import { use, useMemo } from "react";
import { SharedPostScreen } from "@/features/share/SharedPostScreen";
import { createShareMockService } from "@/features/share/mock";
export default function SharedPostPage({ params }: { params: Promise<{ postId: string }> }) { const { postId } = use(params); const service = useMemo(() => createShareMockService(), []); return <SharedPostScreen postId={postId} shareService={service} />; }
