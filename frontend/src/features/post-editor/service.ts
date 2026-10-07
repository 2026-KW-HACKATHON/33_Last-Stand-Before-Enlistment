import type { EditorResult, PhotoSelection, PostFormState } from "./model";
export type PostEditorService = { create(form: PostFormState): Promise<EditorResult>; update(postId: string, form: PostFormState): Promise<EditorResult>; remove(postId: string): Promise<void> };
export type PhotoService = { process(photo: PhotoSelection): Promise<void> };
