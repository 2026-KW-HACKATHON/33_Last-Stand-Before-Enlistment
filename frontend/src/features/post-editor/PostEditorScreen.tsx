"use client";

import { useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import { Header } from "../../components/layout/Header";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Notice } from "../../components/ui/Notice";
import { TextArea } from "../../components/ui/TextArea";
import { useNavigation } from "../../lib/navigation";
import type { DraftService } from "./draft";
import { emptyPostForm, formErrors, photoError, type PhotoSelection, type PostFormState } from "./model";
import type { VoteReminderInput, VoteReminderService } from "./reminder";
import type { PhotoService, PostEditorService } from "./service";
import { VoteReminderControls } from "./VoteReminderControls";

type Props = { mode: "create" | "edit"; postId?: string; initial?: PostFormState; service?: PostEditorService; draftService?: DraftService; reminderService?: VoteReminderService; reminderInitial?: Partial<VoteReminderInput>; photoService?: PhotoService; canEdit?: boolean; canDelete?: boolean; regions?: readonly { id: string; name: string }[]; renderDeleted?: () => ReactNode; onCancel?: () => void; onInvalidate?: (id: string) => void };

export function PostEditorScreen({ mode, postId, initial, service, draftService, reminderService, reminderInitial, photoService, canEdit = true, canDelete = true, regions, renderDeleted, onCancel, onInvalidate }: Props) {
  const nav = useNavigation();
  const busy = useRef(false);
  const [pending, setPending] = useState(false);
  const [deleted, setDeleted] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [form, setForm] = useState(initial ?? emptyPostForm());
  const [draft, setDraft] = useState<"idle" | "pending" | "success" | "error">("idle");
  const [error, setError] = useState("");
  const change = (next: Partial<PostFormState>) => setForm((current) => ({ ...current, ...next }));
  const vote = form.type === "VOTE" ? form.vote : undefined;
  const initialEndsAt = initial?.type === "VOTE" ? initial.vote?.endsAt ?? "" : "";
  const voteEnded = mode === "edit" && vote?.status === "ENDED";
  const reminderState = { ...reminderInitial, endsAtChanged: Boolean(reminderInitial?.endsAtChanged || (reminderInitial?.hadExistingReminder && initialEndsAt && vote?.endsAt && initialEndsAt !== vote.endsAt)) };
  const saveDraft = async () => { if (!draftService) return; setDraft("pending"); try { await draftService.save(form); setDraft("success"); } catch { setDraft("error"); } };
  const submit = async () => {
    if (busy.current) return;
    const issues = formErrors(form);
    if (issues.length) { setError(issues.join(" ")); return; }
    if (!service) { setError("실제 게시 API가 연결되지 않았습니다."); return; }
    busy.current = true; setPending(true); setError("");
    try {
      const result = mode === "create" ? await service.create(form) : await service.update(postId!, form);
      nav.navigate({ destination: { id: "post", params: { postId: result.postId } }, origin: { id: mode === "create" ? "newPost" : "editPost", params: { postId: result.postId } } });
    } catch { setError("게시물 저장을 확인하지 못했습니다. 입력은 유지됩니다. 목록을 재조회해 저장 여부를 확인한 뒤 다시 시도해 주세요."); }
    finally { busy.current = false; setPending(false); }
  };
  const remove = async () => {
    if (!service || !postId || !canDelete || busy.current) return;
    busy.current = true; setPending(true); setError("");
    try { await service.remove(postId); onInvalidate?.(postId); setDeleted(true); }
    catch { setError("삭제를 확인하지 못했습니다. 다시 조회해 주세요."); }
    finally { busy.current = false; setPending(false); }
  };
  const upload = async (selected: PhotoSelection) => {
    if (!photoService) return;
    selected.status = "pending";
    change({ photos: [...form.photos] });
    try { const uploaded = await photoService.process(selected); if (uploaded) Object.assign(selected, uploaded); selected.status = "success"; }
    catch { selected.status = "error"; setError("사진 업로드를 확인하지 못했습니다. 다시 확인하거나 제거 후 선택해 주세요."); }
    setForm(current => ({ ...current, photos: [...current.photos] }));
  };
  const selectPhotos = (files: FileList | null) => {
    if (!files) return;
    const selected = [...files].map(file => ({ id: crypto.randomUUID(), file, name: file.name, type: file.type, size: file.size, status: "selected" as const }));
    const combined = [...form.photos, ...selected], issue = photoError(combined);
    if (issue) { setError(issue); return; }
    change({ photos: combined });
  };
  const removePhoto = async (selected: PhotoSelection) => {
    if (selected.fileId && !selected.photoId && photoService?.cancel) {
      try { await photoService.cancel(selected.fileId); } catch { setError("사진 삭제 예약에 실패했습니다. 다시 시도해 주세요."); return; }
    }
    setForm(current => ({ ...current, photos: current.photos.filter(photo => photo.id !== selected.id) }));
  };
  if (deleted) return <MobileLayout header={<Header title="게시물 삭제" />}><Notice>게시물이 삭제되었습니다.</Notice>{renderDeleted?.()}<Button onClick={() => nav.navigate({ destination: { id: "board" } }, true)}>게시판으로</Button></MobileLayout>;
  if (mode === "edit" && !canEdit) return <MobileLayout header={<Header title="게시물 수정" onBack={onCancel} />}><Notice tone="error">수정 권한이 없습니다.</Notice></MobileLayout>;
  if (voteEnded) return <MobileLayout header={<Header title="게시물 수정" onBack={onCancel} />}><Notice tone="warning">종료된 투표는 수정하거나 삭제할 수 없으며 알림 설정도 변경할 수 없습니다.</Notice></MobileLayout>;
  return <MobileLayout header={<Header title={mode === "create" ? "글쓰기" : "게시물 수정"} onBack={onCancel} />}><section className="flex flex-col gap-4"><fieldset disabled={pending || form.photos.some(photo => photo.status === "pending")} className="flex flex-col gap-4">
    <label>게시물 유형<select aria-label="게시물 유형" disabled={mode === "edit"} value={form.type} onChange={event => change({ ...emptyPostForm(event.currentTarget.value as PostFormState["type"]), regionId: form.regionId, topic: form.topic, title: form.title, content: form.content, photos: form.photos })}><option value="LOCAL_AGENDA">지역 안건</option><option value="LOCAL_ACTIVITY">지역 활동 정보</option><option value="VOTE">투표</option></select></label>
    {regions ? <label>게시 지역<select aria-label="게시 지역" disabled={mode === "edit" && !!vote} value={form.regionId} onChange={event => change({ regionId: event.currentTarget.value })}>{regions.map(region => <option key={region.id} value={region.id}>{region.name}</option>)}</select></label> : <Input label="지역 ID" disabled={mode === "edit" && !!vote} value={form.regionId} onChange={event => change({ regionId: event.currentTarget.value })} />}
    <label>주제<select aria-label="주제" disabled={mode === "edit" && !!vote} value={form.topic} onChange={event => change({ topic: event.currentTarget.value })}>{["교통", "주거", "안전", "복지", "생활정보", "환경", "기타"].map(topic => <option key={topic}>{topic}</option>)}</select></label>
    <Input label="제목" value={form.title} onChange={(event) => change({ title: event.currentTarget.value })} />
    <TextArea label="내용" value={form.content} onChange={(event) => change({ content: event.currentTarget.value })} />
    {vote && <>
      <Input label="투표 질문" disabled={mode === "edit"} value={vote.question} onChange={(event) => change({ vote: { ...vote, question: event.currentTarget.value } })} />
      <Input label="선택지 1" disabled={mode === "edit"} value={vote.options[0] ?? ""} onChange={(event) => change({ vote: { ...vote, options: [event.currentTarget.value, vote.options[1] ?? "", ...vote.options.slice(2)] } })} />
      <Input label="선택지 2" disabled={mode === "edit"} value={vote.options[1] ?? ""} onChange={(event) => change({ vote: { ...vote, options: [vote.options[0] ?? "", event.currentTarget.value, ...vote.options.slice(2)] } })} />
      <label className="flex flex-col gap-1">종료 일시<input type="datetime-local" value={vote.endsAt.slice(0, 16)} onChange={(event) => change({ vote: { ...vote, endsAt: event.currentTarget.value ? `${event.currentTarget.value}:00+09:00` : "" } })} className="rounded-input border border-border p-3" /></label>
      {mode === "create" && vote.options.length < 10 && <Button variant="secondary" onClick={() => change({ vote: { ...vote, options: [...vote.options, ""] } })}>선택지 추가</Button>}
      {vote.options.slice(2).map((option, index) => <Input key={index} label={`선택지 ${index + 3}`} disabled={mode === "edit"} value={option} onChange={event => change({ vote: { ...vote, options: vote.options.map((value, i) => i === index + 2 ? event.currentTarget.value : value) } })} />)}
      <VoteReminderControls service={reminderService} endsAt={vote.endsAt} initial={reminderState} />
    </>}
    {form.activity && <>
      <Input label="출처" value={form.activity.source} onChange={event => change({ activity: { ...form.activity!, source: event.currentTarget.value } })} />
      <Input label="일정" value={form.activity.schedule} onChange={event => change({ activity: { ...form.activity!, schedule: event.currentTarget.value } })} />
      <Input label="장소" value={form.activity.location} onChange={event => change({ activity: { ...form.activity!, location: event.currentTarget.value } })} />
      <label>활동 상태<select aria-label="활동 상태" value={form.activity.status} onChange={event => change({ activity: { ...form.activity!, status: event.currentTarget.value as NonNullable<PostFormState["activity"]>["status"] } })}>{["UPCOMING", "ONGOING", "ENDED", "CANCELLED"].map(value => <option key={value}>{value}</option>)}</select></label>
      <Input label="외부 참여 링크" value={form.activity.link} onChange={event => change({ activity: { ...form.activity!, link: event.currentTarget.value } })} />
    </>}
    <label>사진 선택<input aria-label="게시물 사진 선택" type="file" accept="image/jpeg,image/png" multiple disabled={!photoService} onChange={event => { selectPhotos(event.currentTarget.files); event.currentTarget.value = ""; }} /></label>
    {form.photos.map(photo => <div key={photo.id}><PhotoPreview photo={photo} /><span>{photo.name} · {photo.status}</span>{!["success", "ready"].includes(photo.status) && <Button variant="secondary" disabled={photo.status === "pending"} onClick={() => void upload(photo)}>업로드 확인</Button>}<Button variant="secondary" disabled={photo.status === "pending"} onClick={() => void removePhoto(photo)}>사진 제거</Button></div>)}
    {draft === "success" && <Notice>임시 저장되었습니다. 공개 게시하지 않았습니다.</Notice>}{draft === "error" && <Notice tone="error">임시 저장에 실패했습니다. 입력은 유지됩니다.</Notice>}
    <Button variant="secondary" disabled={draft === "pending" || !draftService} onClick={() => void saveDraft()}>{draft === "pending" ? "임시 저장 중" : draft === "error" ? "임시 저장 다시 시도" : "임시 저장"}</Button>
    <Button disabled={pending || form.photos.some(photo => !["success", "ready"].includes(photo.status))} onClick={() => void submit()}>{pending ? "처리 중" : mode === "edit" ? "수정 저장" : "게시하기"}</Button>{mode === "edit" && canDelete && <Button variant="secondary" onClick={() => setConfirmDelete(true)}>삭제</Button>}{confirmDelete && <Notice tone="warning">게시물을 삭제할까요? 파일의 실제 삭제 완료는 별도 확인이 필요합니다.<Button disabled={pending} onClick={() => void remove()}>삭제 확인</Button><Button variant="secondary" disabled={pending} onClick={() => setConfirmDelete(false)}>취소</Button></Notice>}{error && <Notice tone="error">{error}</Notice>}
  </fieldset></section></MobileLayout>;
}

function PhotoPreview({ photo }: { photo: PhotoSelection }) {
  const [preview, setPreview] = useState<string | undefined>(photo.previewUrl);
  useEffect(() => {
    if (!photo.file) return;
    const value = URL.createObjectURL(photo.file);
    const timer = setTimeout(() => setPreview(value), 0);
    return () => { clearTimeout(timer); URL.revokeObjectURL(value); };
  }, [photo.file]);
  // eslint-disable-next-line @next/next/no-img-element -- file preview/validated public backend URL
  return preview ? <img src={preview} alt={photo.name} className="h-24 rounded-card object-cover" /> : null;
}
