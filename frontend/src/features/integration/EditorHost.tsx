"use client";
import { useCallback } from "react";
import { PostEditorScreen } from "../post-editor/PostEditorScreen";
import { emptyPostForm } from "../post-editor/model";
import { PhotoDeletionStatus } from "./PhotoDeletionStatus";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
import { useFeatureServices } from "./provider";
import { useResource } from "./use-resource";
export function EditorHost({ postId }: { postId?: string }) {
  const services = useFeatureServices();
  const load = useCallback(async (signal: AbortSignal) => {
    const completed = await services!.neighborService.getCompletedRegions(signal);
    if (postId) return { ...await services!.postEditorService.loadForm(postId, signal), regions: completed };
    const result = await services!.profileService.load(signal);
    if (result.kind !== "success" || !result.value?.activityRegion) throw new Error("No activity region");
    const region = result.value.activityRegion.reference;
    return { form: { ...emptyPostForm(), regionId: completed.some(row => row.id === region) ? region : completed[0]?.id ?? "" }, canEdit: completed.length > 0, canDelete: false, regions: completed };
  }, [services, postId]);
  const { state, retry } = useResource(services ? load : null);
  if (state.kind !== "success") return <Notice>{state.kind === "loading" ? "작성 정보를 불러오는 중입니다." : state.message}<Button onClick={retry}>다시 조회</Button></Notice>;
  if (!state.data.canEdit) return <Notice>이 지역의 작성·수정 권한이 없습니다.</Notice>;
  return <PostEditorScreen key={postId ?? "new"} mode={postId ? "edit" : "create"} postId={postId} initial={state.data.form} service={services!.postEditorService} photoService={services!.photoService} canEdit={state.data.canEdit} canDelete={state.data.canDelete} regions={state.data.regions} renderDeleted={() => postId ? <PhotoDeletionStatus services={services!} postId={postId} /> : null} />;
}
