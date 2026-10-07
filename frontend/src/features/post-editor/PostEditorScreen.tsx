"use client";

import { useState } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Notice } from "../../components/ui/Notice";
import { TextArea } from "../../components/ui/TextArea";
import { useNavigation } from "../../lib/navigation";
import type { DraftService } from "./draft";
import { emptyPostForm, formErrors, type PostFormState } from "./model";
import type { VoteReminderInput, VoteReminderService } from "./reminder";
import type { PhotoService, PostEditorService } from "./service";
import { VoteReminderControls } from "./VoteReminderControls";

type Props = { mode: "create" | "edit"; postId?: string; initial?: PostFormState; service?: PostEditorService; draftService?: DraftService; reminderService?: VoteReminderService; reminderInitial?: Partial<VoteReminderInput>; photoService?: PhotoService; canEdit?: boolean; onCancel?: () => void; onInvalidate?: (id: string) => void };

export function PostEditorScreen({ mode, postId, initial, service, draftService, reminderService, reminderInitial, canEdit = true, onCancel, onInvalidate }: Props) {
  const nav = useNavigation();
  const [form, setForm] = useState(initial ?? emptyPostForm());
  const [draft, setDraft] = useState<"idle" | "pending" | "success" | "error">("idle");
  const [error, setError] = useState("");
  const change = (next: Partial<PostFormState>) => setForm((current) => ({ ...current, ...next }));
  const vote = form.type === "VOTE" ? form.vote : undefined;
  const initialEndsAt = initial?.type === "VOTE" ? initial.vote?.endsAt ?? "" : "";
  const voteEnded = mode === "edit" && vote?.status === "ENDED";
  const reminderState = { ...reminderInitial, endsAtChanged: Boolean(reminderInitial?.endsAtChanged || (reminderInitial?.hadExistingReminder && initialEndsAt && vote?.endsAt && initialEndsAt !== vote.endsAt)) };
  const saveDraft = async () => { if (!draftService) return; setDraft("pending"); try { await draftService.save(form); setDraft("success"); } catch { setDraft("error"); } };
  const submit = async () => { const issues = formErrors(form); if (issues.length) { setError(issues.join(" ")); return; } if (!service) { setError("실제 게시 API가 연결되지 않았습니다."); return; } const result = mode === "create" ? await service.create(form) : await service.update(postId!, form); nav.navigate({ destination: { id: "post", params: { postId: result.postId } }, origin: { id: mode === "create" ? "newPost" : "editPost", params: { postId: result.postId } } }); };
  const remove = async () => { if (!service || !postId) return; await service.remove(postId); onInvalidate?.(postId); const origin = nav.state.current?.origin; if (origin) nav.navigate({ destination: origin }, true); else nav.back(); };
  if (mode === "edit" && !canEdit) return <MobileLayout header={<Header title="게시물 수정" onBack={onCancel} />}><Notice tone="error">수정 권한이 없습니다.</Notice></MobileLayout>;
  if (voteEnded) return <MobileLayout header={<Header title="게시물 수정" onBack={onCancel} />}><Notice tone="warning">종료된 투표는 수정하거나 삭제할 수 없으며 알림 설정도 변경할 수 없습니다.</Notice></MobileLayout>;
  return <MobileLayout header={<Header title={mode === "create" ? "글쓰기" : "게시물 수정"} onBack={onCancel} />}><section className="flex flex-col gap-4">
    <Input label="제목" value={form.title} onChange={(event) => change({ title: event.currentTarget.value })} />
    <TextArea label="내용" value={form.content} onChange={(event) => change({ content: event.currentTarget.value })} />
    {vote && <>
      <Input label="투표 질문" value={vote.question} onChange={(event) => change({ vote: { ...vote, question: event.currentTarget.value } })} />
      <Input label="선택지 1" value={vote.options[0] ?? ""} onChange={(event) => change({ vote: { ...vote, options: [event.currentTarget.value, vote.options[1] ?? "", ...vote.options.slice(2)] } })} />
      <Input label="선택지 2" value={vote.options[1] ?? ""} onChange={(event) => change({ vote: { ...vote, options: [vote.options[0] ?? "", event.currentTarget.value, ...vote.options.slice(2)] } })} />
      <label className="flex flex-col gap-1">종료 일시<input type="datetime-local" value={vote.endsAt} onChange={(event) => change({ vote: { ...vote, endsAt: event.currentTarget.value } })} className="rounded-input border border-border p-3" /></label>
      <VoteReminderControls service={reminderService} endsAt={vote.endsAt} initial={reminderState} />
    </>}
    <Input label="참고 링크" value={form.referenceLink} onChange={(event) => change({ referenceLink: event.currentTarget.value })} />
    {draft === "success" && <Notice>임시 저장되었습니다. 공개 게시하지 않았습니다.</Notice>}{draft === "error" && <Notice tone="error">임시 저장에 실패했습니다. 입력은 유지됩니다.</Notice>}
    <Button variant="secondary" disabled={draft === "pending" || !draftService} onClick={() => void saveDraft()}>{draft === "pending" ? "임시 저장 중" : draft === "error" ? "임시 저장 다시 시도" : "임시 저장"}</Button>
    <Button onClick={() => void submit()}>게시하기</Button>{mode === "edit" && <Button variant="secondary" onClick={() => void remove()}>삭제</Button>}{error && <Notice tone="error">{error}</Notice>}
  </section></MobileLayout>;
}
