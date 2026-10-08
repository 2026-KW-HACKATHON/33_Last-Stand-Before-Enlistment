"use client";

import { useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { VoteValuePill, VoteValueSummary } from "../../components/ui/VoteValue";
import type { VoteSnapshot, VoteState } from "./model";
import type { VoteService } from "./service";

type ViewerMode = "member" | "guest";

export function VotePanel({ postId, status, service, canVote, viewerMode, onInvalidate }: {
  postId: string;
  status: "OPEN" | "ENDED";
  service: VoteService;
  canVote: boolean;
  viewerMode: ViewerMode;
  onInvalidate?: (postId: string) => void;
}) {
  const [state, setState] = useState<VoteState>({ kind: "loading" });
  const [draftOptionId, setDraftOptionId] = useState<string>();
  const [confirmOptionId, setConfirmOptionId] = useState<string>();
  const [pending, setPending] = useState(false);
  const [failedOptionId, setFailedOptionId] = useState<string>();
  const [message, setMessage] = useState("");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    void service.get(postId).then((snapshot) => {
      if (!active) return;
      setState({ kind: "ready", snapshot });
      setDraftOptionId(snapshot.submittedOptionId);
      setFailedOptionId(undefined);
      setConfirmOptionId(undefined);
      setMessage("");
    }).catch((error: unknown) => {
      if (active) setState({ kind: "error", message: error instanceof Error ? error.message : "투표 결과를 불러오지 못했습니다." });
    });
    return () => { active = false; };
  }, [attempt, postId, service]);

  const refreshAfterUncertainResult = async (): Promise<VoteSnapshot | undefined> => {
    try {
      const refreshed = await service.get(postId);
      setState({ kind: "ready", snapshot: refreshed });
      return refreshed;
    } catch {
      return undefined;
    }
  };

  const submit = async (optionId: string) => {
    if (state.kind !== "ready" || pending || status === "ENDED" || !canVote) return;
    setPending(true);
    setFailedOptionId(undefined);
    setMessage("");
    try {
      const snapshot = await service.submit(postId, optionId);
      setState({ kind: "ready", snapshot });
      setDraftOptionId(snapshot.submittedOptionId);
      setConfirmOptionId(undefined);
      setMessage("투표가 저장되었습니다.");
      onInvalidate?.(postId);
    } catch (error: unknown) {
      const refreshed = await refreshAfterUncertainResult();
      setFailedOptionId(optionId);
      setMessage(refreshed
        ? `${error instanceof Error ? error.message : "투표 제출에 실패했습니다."} 현재 결과를 다시 확인했습니다. 다시 시도해 주세요.`
        : `${error instanceof Error ? error.message : "투표 제출에 실패했습니다."} 결과를 다시 확인한 뒤 다시 시도해 주세요.`);
    } finally {
      setPending(false);
    }
  };

  if (state.kind === "loading") return <Notice aria-busy="true">투표 결과를 불러오는 중입니다.</Notice>;
  if (state.kind === "error") return <><Notice tone="error">{state.message}</Notice><Button className="mt-2" onClick={() => setAttempt((value) => value + 1)}>다시 시도</Button></>;

  const snapshot = state.snapshot;
  const submitted = snapshot.options.find((option) => option.id === snapshot.submittedOptionId);
  const totalVotes = snapshot.options.reduce((total, option) => total + option.count, 0);
  const hasChangedSelection = Boolean(draftOptionId && snapshot.submittedOptionId && draftOptionId !== snapshot.submittedOptionId);
  const selectionAllowed = status === "OPEN" && canVote && !pending;
  const choose = (optionId: string) => {
    if (!selectionAllowed) return;
    setDraftOptionId(optionId);
    setMessage("");
    setFailedOptionId(undefined);
    if (snapshot.submittedOptionId && optionId !== snapshot.submittedOptionId) setConfirmOptionId(optionId);
    else setConfirmOptionId(undefined);
  };

  return <section aria-label="투표 참여" className="rounded-card border border-border bg-surface p-3 shadow-card">
    <div className="flex items-center justify-between gap-2">
      <h3 className="text-card-title">공개 투표</h3>
      <span className="text-caption text-secondary">총 {totalVotes}명 참여</span>
    </div>
    <div className="mt-3 space-y-2" aria-label="공개 투표 결과">
      {snapshot.options.map((option) => {
        const percentage = totalVotes === 0 ? 0 : Math.round((option.count / totalVotes) * 100);
        const isDraft = draftOptionId === option.id;
        const isSubmitted = snapshot.submittedOptionId === option.id;
        return <button key={option.id} type="button" aria-pressed={isDraft} disabled={!selectionAllowed} onClick={() => choose(option.id)} className={`relative isolate w-full overflow-hidden rounded-notice border bg-surface px-3 py-2 text-left disabled:cursor-not-allowed ${isDraft ? "border-primary" : "border-border"}`}>
          <span className="absolute inset-y-0 left-0 -z-10 bg-soft transition-[width]" style={{ width: `${percentage}%` }} />
          <span className="flex min-w-0 items-center gap-2">{isSubmitted && <span aria-label="내 선택" className="inline-flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-semibold text-surface">✓</span>}<VoteValuePill>{option.label} {option.count}표 · {percentage}%</VoteValuePill></span>
        </button>;
      })}
    </div>
    <div className="mt-3 text-caption text-secondary">{submitted ? <VoteValueSummary summary={`내가 제출한 선택: ${submitted.label} · ${submitted.count}표 · ${totalVotes === 0 ? 0 : Math.round((submitted.count / totalVotes) * 100)}%`}/> : "선택지를 고른 뒤 투표를 제출해 주세요."}</div>
    {status === "ENDED" && <Notice tone="info">종료된 투표입니다. 결과만 확인할 수 있습니다.</Notice>}
    {status === "OPEN" && viewerMode === "guest" && <Notice tone="info">투표 결과는 볼 수 있습니다. 투표하려면 회원으로 참여해 주세요.</Notice>}
    {status === "OPEN" && viewerMode !== "guest" && !canVote && <Notice tone="info">현재 권한으로는 투표에 참여할 수 없습니다.</Notice>}
    {status === "OPEN" && canVote && !confirmOptionId && <Button className="mt-3 w-full" disabled={!draftOptionId || pending || draftOptionId === snapshot.submittedOptionId} onClick={() => draftOptionId && void submit(draftOptionId)}>{pending ? "투표 저장 중" : snapshot.submittedOptionId ? "선택 변경" : "투표 제출"}</Button>}
    {hasChangedSelection && confirmOptionId && <Notice tone="warning" className="mt-3"><p>기존 투표를 새 선택으로 변경할까요?</p><div className="mt-2 flex gap-2"><Button disabled={pending} onClick={() => void submit(confirmOptionId)}>{pending ? "변경 중" : "변경 확인"}</Button><Button variant="secondary" disabled={pending} onClick={() => { setDraftOptionId(snapshot.submittedOptionId); setConfirmOptionId(undefined); }}>취소</Button></div></Notice>}
    {message && <Notice tone={failedOptionId ? "error" : "info"} role="status" className="mt-3">{message}{failedOptionId && <Button variant="secondary" className="ml-2 min-h-0 px-2 py-1" disabled={pending} onClick={() => void submit(failedOptionId)}>다시 시도</Button>}</Notice>}
  </section>;
}
