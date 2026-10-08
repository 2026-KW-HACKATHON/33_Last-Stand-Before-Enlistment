import type { ApiClient } from "../../lib/api/types";
import { decodeNoContent } from "../../lib/api/response";
import type { PostFormState } from "../post-editor/model";
import type { PhotoService, PostEditorService } from "../post-editor/service";
import { ApiError } from "../../lib/api/error";
import { activityStates, postDisplay, topicCode } from "./post-decoder";
import { array, bool, count, data, enumValue, id, inputId, nullableText, object, text, timestamp, url } from "./wire";

export function photoView(value: unknown) {
  const row = object(value);
  const status = enumValue(row.status, ["LEGACY", "UPLOADING", "UNLINKED", "LINKED", "DELETE_PENDING", "DELETED"] as const);
  const preview = nullableText(row.url);
  if (preview) url(preview);
  if (row.contentType !== null) enumValue(row.contentType, ["image/jpeg", "image/png"] as const);
  if (row.sizeBytes !== null) count(row.sizeBytes);
  timestamp(row.createdAt);
  for (const key of ["uploadedAt", "uploadAuthorizationExpiresAt", "cleanupEligibleAt", "linkExpiresAt", "deleteRequestedAt", "deletedAt"]) if (row[key] !== null) timestamp(row[key]);
  const canAttach = bool(row.canAttach), deletionCompleted = bool(row.deletionCompleted);
  if ((canAttach && status !== "UNLINKED") || deletionCompleted !== (status === "DELETED")) throw new Error("Invalid photo lifecycle");
  return { fileId: id(row.fileId), status, url: preview, canAttach, deletionCompleted };
}
export function createEditorServices(client: ApiClient) {
  const pendingDeletions = new Map<string, readonly string[]>();
  const photoService: PhotoService & { get(fileId: string, signal?: AbortSignal): ReturnType<typeof read>; cancel(fileId: string): ReturnType<typeof read> } = {
    async process(photo) {
      if (!photo.file || !["image/jpeg", "image/png"].includes(photo.file.type) || photo.file.size < 1 || photo.file.size > 10_000_000) throw new ApiError("validation", "10MB 이하 JPG/PNG 파일이 필요합니다.");
      // Once reserved, retries use the same tracked ID; never silently reserve a second upload.
      let fileId = photo.fileId;
      if (!fileId) {
        const reserved = await data(client, "photo-uploads", value => {
          const row = object(value), upload = object(row.upload), fileId = id(row.fileId);
          if (row.status !== "UPLOADING" || upload.method !== "PUT" || upload.bodyMode !== "RAW" || upload.url !== `/api/v1/photo-uploads/${fileId}/content`) throw new Error("Unexpected upload destination");
          timestamp(row.createdAt); timestamp(row.cleanupEligibleAt); timestamp(upload.expiresAt);
          const headers = object(upload.headers);
          if (Object.keys(headers).length !== 1 || headers["Content-Type"] !== photo.file!.type) throw new Error("Unexpected upload headers");
          return fileId;
        }, { method: "POST", json: { originalName: photo.file.name, contentType: photo.file.type, sizeBytes: photo.file.size } });
        fileId = reserved;
        // Expose reservation identity even if binary transfer fails so the UI can retry/clean it.
        photo.fileId = fileId;
        await data(client, `photo-uploads/${inputId(fileId)}/content`, value => { const row = photoView(value); if (row.fileId !== fileId) throw new Error("Wrong uploaded file"); return row; }, { method: "PUT", body: photo.file, headers: { "Content-Type": photo.file.type } });
      } else {
        const current = await read(fileId);
        if (current.canAttach) return { ...photo, fileId, status: "success", previewUrl: current.url };
        if (current.status !== "UPLOADING") throw new ApiError("conflict", "연결할 수 없는 사진입니다. 제거 후 다시 선택해 주세요.");
        // An interrupted transfer may already have reached the server. Complete first; never replay bytes blindly.
      }
      const completed = await data(client, `photo-uploads/${inputId(fileId)}/complete`, photoView, { method: "POST", json: {} });
      if (!completed.canAttach || completed.fileId !== fileId) throw new Error("Photo not attachable");
      return { ...photo, fileId, status: "success", previewUrl: completed.url };
    },
    get: read,
    async cancel(fileId) { return data(client, `photo-uploads/${inputId(fileId)}`, value => { const result = photoView(value); if (result.fileId !== fileId) throw new Error("Wrong canceled file"); return result; }, { method: "DELETE" }); },
  };
  function read(fileId: string, signal?: AbortSignal) { return data(client, `photo-uploads/${inputId(fileId)}`, value => { const result = photoView(value); if (result.fileId !== fileId) throw new Error("Wrong file"); return result; }, { signal }); }
  function photos(form: PostFormState, edit: boolean) {
    if (form.photos.length > 10 || form.photos.reduce((sum, photo) => sum + photo.size, 0) > 10_000_000) throw new ApiError("validation", "사진은 최대 10장, 합계 10MB입니다.");
    return form.photos.map(photo => {
      if (edit && photo.photoId) return { photoId: inputId(photo.photoId) };
      if (!photo.fileId || !["success", "ready"].includes(photo.status)) throw new ApiError("validation", "사진 업로드를 완료해 주세요.");
      return { fileId: inputId(photo.fileId) };
    });
  }
  function payload(form: PostFormState, edit: boolean) {
    if (form.referenceLink || form.anonymous) throw new ApiError("validation", "참고 링크·익명 작성은 현재 서버 계약에서 지원하지 않습니다.");
    const refs = photos(form, edit);
    const common = { title: form.title, content: form.content, ...(edit ? { photoOrder: refs } : { type: form.type, photoFileIds: refs.map(ref => ref.fileId) }) };
    if (edit && form.type === "VOTE") return { ...common, details: { endsAt: timestamp(form.vote?.endsAt) } };
    const base = { ...common, topic: topicCode(form.topic), regionId: inputId(form.regionId) };
    if (form.type === "LOCAL_ACTIVITY") {
      if (!form.activity) throw new Error("Missing activity");
      const status = (Object.keys(activityStates) as (keyof typeof activityStates)[]).find(key => activityStates[key] === form.activity!.status);
      if (!status) throw new Error("Unknown activity status");
      return { ...base, details: { source: form.activity.source, schedule: form.activity.schedule, place: form.activity.location, activityStatus: status, ...(form.activity.link ? { externalParticipationUrl: url(form.activity.link) } : edit ? { externalParticipationUrl: null } : {}) } };
    }
    if (form.type === "VOTE") {
      if (!form.vote) throw new Error("Missing vote");
      return { ...base, details: { question: form.vote.question, options: form.vote.options, endsAt: timestamp(form.vote.endsAt) } };
    }
    return base;
  }
  const postEditorService: PostEditorService & { loadForm(postId: string, signal?: AbortSignal): Promise<{ form: PostFormState; canEdit: boolean; canDelete: boolean }> } = {
    async create(form) {
      const postId = await data(client, "posts", value => id(object(value).postId), { method: "POST", json: payload(form, false) });
      // Do not retry POST if the follow-up read fails; the post already exists.
      try { const post = await data(client, `posts/${postId}`, postDisplay); return { postId, post }; }
      catch { return { postId }; }
    },
    async update(postId, form) {
      const post = await client.request(`posts/${inputId(postId)}`, { method: "PATCH", json: payload(form, true), decode: body => {
        const envelope = object(body), post = postDisplay(envelope.data);
        if (envelope.meta !== undefined) {
          const deletion = object(object(envelope.meta).photoDeletion);
          if (deletion.status !== "PENDING") throw new Error("Invalid deletion metadata");
          pendingDeletions.set(postId, array(deletion.fileIds).map(id));
        }
        return post;
      } });
      if (post.id !== postId) throw new Error("Wrong updated post");
      return { postId, post };
    },
    async remove(postId) {
      const files = await data(client, `posts/${inputId(postId)}`, value => array(object(value).images).flatMap(value => { const image = object(value); return image.fileId === undefined ? [] : [id(image.fileId)]; }));
      await client.request(`posts/${inputId(postId)}`, { method: "DELETE", decode: decodeNoContent });
      pendingDeletions.set(postId, files);
    },
    async loadForm(postId, signal) {
      return data(client, `posts/${inputId(postId)}`, value => {
        const row = object(value), post = postDisplay(row);
        if (post.id !== postId) throw new Error("Wrong editor post");
        const form: PostFormState = { type: post.type, regionId: post.metadata.regionId!, topic: post.metadata.topic, title: post.title, content: post.content, referenceLink: "", anonymous: false,
          photos: array(row.images).map(value => { const image = object(value); return { id: id(image.photoId), photoId: id(image.photoId), fileId: image.fileId === undefined ? undefined : id(image.fileId), name: post.title, type: text(image.contentType), size: count(image.sizeBytes), previewUrl: url(image.url), status: "ready" as const }; }),
        };
        if (post.type === "LOCAL_ACTIVITY") { const activity = object(row.activity); form.activity = { source: text(activity.source), schedule: text(activity.schedule), location: text(activity.place), status: post.activity.status, link: nullableText(activity.externalParticipationUrl) ?? "" }; }
        if (post.type === "VOTE") { const vote = object(row.vote); form.vote = { question: text(vote.question), options: array(vote.options).map(value => text(object(value).content)), endsAt: timestamp(vote.endsAt), status: post.vote.status }; }
        return { form, canEdit: post.capabilities.canEdit, canDelete: post.capabilities.canDelete };
      }, { signal });
    },
  };
  return { photoService, postEditorService, getPendingPhotoDeletions: (postId: string) => pendingDeletions.get(postId) ?? [] };
}
