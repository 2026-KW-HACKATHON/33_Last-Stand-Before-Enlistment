"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { createDraftMockService } from "@/features/post-editor/draft";
import { emptyPostForm, type PostFormState } from "@/features/post-editor/model";
import { PostEditorScreen } from "@/features/post-editor/PostEditorScreen";
import { createVoteReminderMockService, type ReminderMockMode } from "@/features/post-editor/reminder";

type PreviewState = "off" | "on" | "existing" | "ended";

function voteForm(ended = false): PostFormState {
  const form = emptyPostForm("VOTE");
  return {
    ...form,
    title: "공원 야간 개방 시간 투표",
    content: "투표 양식과 다른 입력은 알림 저장 실패 후에도 유지됩니다.",
    referenceLink: "https://example.invalid/vote-reference",
    vote: { question: "공원 야간 개방 시간을 늘릴까요?", options: ["찬성", "반대"], endsAt: "2026-10-30T18:00", status: ended ? "ENDED" : "OPEN" },
  };
}

export function PostEditorPreview() {
  const [preview, setPreview] = useState<PreviewState>("off");
  const [reminderMode, setReminderMode] = useState<ReminderMockMode>("success");
  const ended = preview === "ended";
  const existing = preview === "existing";
  const initial = voteForm(ended);
  const reminderInitial = preview === "on"
    ? { enabled: true, remindAt: "2026-10-30T17:00" }
    : existing ? { enabled: true, remindAt: "", hadExistingReminder: true, endsAtChanged: true }
      : { enabled: false };

  return <div className="min-h-dvh bg-background p-4">
    <div className="mx-auto mb-3 flex max-w-mobile flex-wrap gap-2 rounded-card border p-3">
      <Button variant={preview === "off" ? "primary" : "secondary"} onClick={() => setPreview("off")}>알림 미선택</Button>
      <Button variant={preview === "on" ? "primary" : "secondary"} onClick={() => setPreview("on")}>알림 설정</Button>
      <Button variant={preview === "existing" ? "primary" : "secondary"} onClick={() => setPreview("existing")}>기존 예약 변경</Button>
      <Button variant={preview === "ended" ? "primary" : "secondary"} onClick={() => setPreview("ended")}>종료 투표</Button>
      <Button variant={reminderMode === "success" ? "primary" : "secondary"} onClick={() => setReminderMode("success")}>알림 Mock 성공</Button>
      <Button variant={reminderMode === "error" ? "primary" : "secondary"} onClick={() => setReminderMode("error")}>알림 Mock 실패</Button>
    </div>
    <PostEditorScreen
      key={`${preview}-${reminderMode}`}
      mode={ended ? "edit" : "create"}
      initial={initial}
      draftService={createDraftMockService("success")}
      reminderService={createVoteReminderMockService(reminderMode)}
      reminderInitial={reminderInitial}
    />
  </div>;
}
