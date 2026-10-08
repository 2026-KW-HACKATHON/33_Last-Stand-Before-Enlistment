import type { PostDisplayModel } from "./model";

/** Implemented by a Mock now and an API adapter only after the GET contract is agreed. */
export interface PostService {
  getPost(postId: string, signal?: AbortSignal): Promise<PostDisplayModel>;
}
