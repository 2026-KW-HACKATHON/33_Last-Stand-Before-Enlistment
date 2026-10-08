"use client";

import { useEffect, useState } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import type { AgendaPostDisplay } from "../post/model";
import type { SummaryState } from "./model";
import type { SummaryService } from "./service";

type Props = { post: AgendaPostDisplay; service: SummaryService };

export function SummaryPanel({ post, service }: Props) {
  const [state, setState] = useState<SummaryState>({ kind: "loading" });
  const [showSource, setShowSource] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    void service.getSummary(post).then((result) => {
      if (!active) return;
      if (result.status === "SUCCEEDED") setState({ kind: "success", summary: result.summary });
      else setState({ kind: "fallback", message: result.message });
    }).catch((error: unknown) => {
      if (active) setState({ kind: "error", message: error instanceof Error ? error.message : "요약을 불러오지 못했습니다." });
    });
    return () => { active = false; };
  }, [attempt, post, service]);
  const retry = () => { setState({ kind: "loading" }); setAttempt((current) => current + 1); };

  return <section className="rounded-card border border-border bg-background p-section" aria-label="AI 안건 요약">
    <div className="flex items-center justify-between gap-3"><h2 className="text-card-title">AI 안건 요약</h2><Button variant="secondary" className="min-h-0 px-3 py-1 text-caption" onClick={() => setShowSource((current) => !current)}>{showSource ? "AI 요약 보기" : "원문 보기"}</Button></div>
    {showSource ? <Source post={post} /> : <SummaryContent state={state} post={post} onRetry={retry} />}
  </section>;
}

function SummaryContent({ state, post, onRetry }: { state: SummaryState; post: AgendaPostDisplay; onRetry: () => void }) {
  if (state.kind === "loading") return <p className="mt-3 text-body text-secondary" aria-busy="true">AI 요약을 불러오는 중입니다.</p>;
  if (state.kind === "success") return <><p className="mt-3 whitespace-pre-wrap text-body text-text">{state.summary}</p><SourceLink post={post} /></>;
  if (state.kind === "fallback") return <><Notice className="mt-3">{state.message}</Notice><Source post={post} /></>;
  return <><Notice tone="error" className="mt-3">{state.message}</Notice><Source post={post} /><Button className="mt-3" onClick={onRetry}>요약 다시 시도</Button></>;
}

function Source({ post }: { post: AgendaPostDisplay }) { return <div className="mt-3 rounded-notice bg-soft p-section"><p className="text-caption text-secondary">원문</p><p className="mt-1 whitespace-pre-wrap text-body text-text">{post.content}</p><SourceLink post={post} /></div>; }
function SourceLink({ post }: { post: AgendaPostDisplay }) { return post.referenceLink ? <a className="mt-3 inline-flex text-button text-primary underline" href={post.referenceLink.href} target="_blank" rel="noreferrer">{post.referenceLink.label}</a> : null; }
