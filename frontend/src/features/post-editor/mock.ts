import { postFixtures } from "../post/mock";
import type { PostDisplayModel } from "../post/model";
import type { PostFormState } from "./model";
import type { PhotoService, PostEditorService } from "./service";

export type EditorMockMode = "success" | "error";

function toPost(form: PostFormState, postId: string): PostDisplayModel {
  const seed = form.type === "LOCAL_AGENDA" ? postFixtures["agenda-photo"] : form.type === "LOCAL_ACTIVITY" ? postFixtures.activity : postFixtures.vote;
  const common = { ...seed, id: postId, title: form.title, content: form.content, metadata: { ...seed.metadata, topic: form.topic }, images: form.photos.map((photo) => ({ id: photo.id, url: photo.previewUrl ?? "", alt: photo.name })), referenceLink: form.referenceLink ? { label: "참고 자료", href: form.referenceLink } : undefined, anonymous: form.type === "LOCAL_AGENDA" && form.anonymous };
  if (form.type === "LOCAL_ACTIVITY" && seed.type === "LOCAL_ACTIVITY") return { ...common, activity: { ...seed.activity, source: form.activity?.source ?? "", scheduleLabel: form.activity?.schedule ?? "", location: form.activity?.location ?? "", status: form.activity?.status ?? "UPCOMING" } } as PostDisplayModel;
  if (form.type === "VOTE" && seed.type === "VOTE") return { ...common, vote: { ...seed.vote, status: form.vote?.status ?? "OPEN" } } as PostDisplayModel;
  return common as PostDisplayModel;
}
export function createPostEditorMockService(mode: EditorMockMode = "success"): PostEditorService { const run = async () => { if (mode === "error") throw new Error("Mock editor request failed."); }; return { async create(form) { await run(); return { postId: "mock-created-post", post: toPost(form, "mock-created-post") }; }, async update(postId, form) { await run(); return { postId, post: toPost(form, postId) }; }, async remove() { await run(); } }; }
export function createPhotoMockService(mode: EditorMockMode = "success"): PhotoService { return { async process() { if (mode === "error") throw new Error("Mock photo processing failed."); } }; }
