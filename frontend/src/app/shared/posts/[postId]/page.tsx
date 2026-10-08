"use client";
import { useMemo } from "react";
import { SharedPostScreen } from "@/features/share/SharedPostScreen";
import { createShareMockService } from "@/features/share/mock";
export default function SharedPostPage({ params }: { params: { postId: string } }) { const service = useMemo(() => createShareMockService(), []); return <SharedPostScreen postId={params.postId} shareService={service} />; }
