"use client";
import { useState } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import type { ShareService } from "./service";
export function ShareActions({ postId, service }: { postId: string; service: ShareService }) {
  const [pending, setPending] = useState(false); const [failed, setFailed] = useState(false); const [message, setMessage] = useState("");
  const copy = async () => { if (pending) return; setPending(true); setFailed(false); try { await service.copy(postId); setMessage("공유 링크를 복사했습니다."); } catch (error) { setFailed(true); setMessage(error instanceof Error ? error.message : "링크를 복사하지 못했습니다."); } finally { setPending(false); } };
  return <section aria-label="공유"><Button variant="secondary" disabled={pending} onClick={() => void copy()}>{pending ? "복사 중" : "링크 복사"}</Button>{message && <Notice role="status" tone={failed ? "error" : "info"} className="mt-2">{message}{failed && <Button variant="secondary" className="ml-2 min-h-0 px-2 py-1" onClick={() => void copy()}>다시 시도</Button>}</Notice>}</section>;
}
