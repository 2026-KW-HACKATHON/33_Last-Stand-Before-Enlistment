"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useNavigation } from "../../lib/navigation";
import { PostCard } from "../post/PostCard";
import { recommendationSnapshotReference, type RecommendationInput, type RecommendationResult, type RecommendationStatus } from "./model";
import type { RecommendationService } from "./service";

type RecommendationScreenProps = {
  input: RecommendationInput;
  service: RecommendationService;
  onBack: () => void;
  onOpenPost?: (postId: string, snapshotRef: string) => void;
  /** #55 owns the summary screen; this is intentionally only an entry boundary. */
  onOpenSummary?: (postId: string, snapshotRef: string) => void;
};

function inputKey(input: RecommendationInput) {
  return `${input.activityRegion.id}:${input.interestRegions.map((region) => region.id).join(",")}:${input.interestKeywords.join(",")}`;
}

export function RecommendationScreen({ input, service, onBack, onOpenPost, onOpenSummary }: RecommendationScreenProps) {
  const navigation = useNavigation();
  const [status, setStatus] = useState<RecommendationStatus>("loading");
  const [current, setCurrent] = useState<RecommendationResult | null>(null);
  const [message, setMessage] = useState("");
  const lastAction = useRef<"load" | "refresh">("load");
  const stableInputKey = inputKey(input);
  const inputDescription = useMemo(() => [
    `Activity region: ${input.activityRegion.name}`,
    `Interest regions: ${input.interestRegions.length ? input.interestRegions.map((region) => region.name).join(", ") : "Not set"}`,
    `Interest keywords: ${input.interestKeywords.length ? input.interestKeywords.join(", ") : "Not set"}`,
  ], [input]);

  const load = async (signal?: AbortSignal) => {
    lastAction.current = "load";
    setStatus("loading"); setMessage("");
    try {
      const result = await service.get(input, signal);
      if (signal?.aborted) return;
      setCurrent(result); setStatus(result ? "success" : "empty");
    } catch {
      if (signal?.aborted) return;
      setStatus("error"); setMessage("Recommendations could not be loaded. Please retry.");
    }
  };
  const refresh = async () => {
    if (!current) { await load(); return; }
    lastAction.current = "refresh";
    setStatus("refreshing"); setMessage("");
    try {
      const result = await service.refresh(input, current.post.id);
      if (result) { setCurrent(result); setStatus("success"); return; }
      setStatus("success"); setMessage("There is no alternative recommendation yet. The current recommendation remains available.");
    } catch {
      setStatus("error"); setMessage("A new recommendation could not be requested. The current recommendation remains available.");
    }
  };
  const retry = () => { if (lastAction.current === "refresh" && current) void refresh(); else void load(); };

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => { void load(controller.signal); }, 0);
    return () => { window.clearTimeout(timer); controller.abort(); };
    // A service boundary and the three product inputs are the complete reload contract.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [service, stableInputKey]);

  const openPost = () => {
    if (!current) return;
    const ref = recommendationSnapshotReference(current);
    navigation.registerSnapshot({ destination: { id: "board" }, kind: "list", ref });
    onOpenPost?.(current.post.id, ref);
    navigation.navigate({ destination: { id: "post", params: { postId: current.post.id } }, origin: { id: "board" }, sharedContextRef: ref });
  };
  const openSummary = () => {
    if (!current) return;
    const ref = recommendationSnapshotReference(current);
    navigation.registerSnapshot({ destination: { id: "board" }, kind: "list", ref });
    onOpenSummary?.(current.post.id, ref);
  };

  return <MobileLayout header={<Header title="Recommendations" onBack={onBack} />}>
    <section className="rounded-card bg-soft p-section" aria-label="Recommendation inputs"><h2 className="text-card-title">Recommendation criteria</h2><ul className="mt-2 space-y-1 text-caption text-secondary">{inputDescription.map((description) => <li key={description}>{description}</li>)}</ul></section>
    {status === "loading" && <div aria-busy="true" className="mt-4 space-y-3"><div className="h-36 animate-pulse rounded-card bg-disabled" /><div className="h-10 animate-pulse rounded-input bg-disabled" /></div>}
    {status === "empty" && <Notice className="mt-4">No recommendation is available for the current criteria.</Notice>}
    {status === "error" && <Notice tone="error" role="alert" className="mt-4"><p>{message}</p><Button className="mt-3" onClick={retry}>Retry</Button></Notice>}
    {current && <section className="mt-4" aria-live="polite"><p className="text-caption text-secondary">Why this recommendation</p><p className="mt-1 text-body text-text">{current.reason}</p><div className="mt-3"><PostCard post={current.post} onOpen={openPost} /></div><div className="mt-3 flex gap-2"><Button onClick={() => void refresh()} disabled={status === "refreshing"}>New recommendation</Button><Button variant="secondary" onClick={openSummary}>Open AI summary</Button></div>{status === "refreshing" && <p className="mt-2 text-caption text-secondary" aria-busy="true">Finding a new recommendation…</p>}{message && status === "success" && <Notice className="mt-3">{message}</Notice>}</section>}
  </MobileLayout>;
}
