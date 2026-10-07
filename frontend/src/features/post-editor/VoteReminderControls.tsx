"use client";

import { useState } from "react";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import type { VoteReminderInput, VoteReminderService } from "./reminder";

type Props = { service?: VoteReminderService; ended?: boolean; endsAt: string; initial?: Partial<VoteReminderInput> };

export function VoteReminderControls({ service, ended = false, endsAt, initial }: Props) {
  const [input, setInput] = useState<VoteReminderInput>({ enabled: false, remindAt: "", hadExistingReminder: false, endsAtChanged: false, ...initial });
  const [state, setState] = useState<"idle" | "pending" | "success" | "error">("idle");
  const [message, setMessage] = useState("");
  const save = async () => {
    if (!input.enabled) { setMessage("알림을 설정하지 않았습니다. 게시 및 수정은 계속할 수 있습니다."); setState("success"); return; }
    if (!input.remindAt) { setMessage("알림 시점을 입력해 주세요."); setState("error"); return; }
    if (endsAt && input.remindAt >= endsAt) { setMessage("알림 시점은 투표 종료 시각 이전이어야 합니다."); setState("error"); return; }
    if (!service) return;
    setState("pending"); setMessage("");
    try { await service.save(input); setMessage("Mock 알림 설정이 저장되었습니다. 실제 예약·발송은 연동 대기입니다."); setState("success"); }
    catch { setMessage("알림 저장에 실패했습니다. 현재 입력은 유지되며 다시 시도할 수 있습니다."); setState("error"); }
  };
  if (ended) return <Notice tone="warning">종료된 투표는 종료 전 알림을 수정할 수 없습니다.</Notice>;
  return <section className="rounded-card bg-soft p-section" aria-label="투표 종료 전 알림">
    <label className="flex items-center gap-2"><input type="checkbox" checked={input.enabled} onChange={(event) => { const enabled = event.currentTarget.checked; setInput((current) => ({ ...current, enabled })); setState("idle"); setMessage(""); }} />종료 전 알림 설정</label>
    {input.enabled && <label className="mt-3 block">알림 시점<input type="datetime-local" value={input.remindAt} onChange={(event) => { const remindAt = event.currentTarget.value; setInput((current) => ({ ...current, remindAt })); setState("idle"); setMessage(""); }} className="mt-1 w-full rounded-input border border-border p-3" /></label>}
    {input.hadExistingReminder && input.endsAtChanged && <Notice className="mt-3">종료 시각이 변경되어 기존 예약은 취소됩니다. 새 알림 시점은 직접 다시 설정해 주세요.</Notice>}
    {state === "success" && <Notice className="mt-3">{message}</Notice>}
    {state === "error" && <Notice tone="error" className="mt-3">{message}</Notice>}
    <Button className="mt-3" disabled={state === "pending" || !service} onClick={() => void save()}>{state === "pending" ? "알림 저장 중" : state === "error" ? "알림 설정 다시 시도" : "알림 설정 저장"}</Button>
  </section>;
}
