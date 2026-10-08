import type { CommentCreateInput, CommentDisplay, CommentReplyInput, CommentThread } from "./model";

/** Real adapters implement this only after the API contract is agreed. */
export type CommentService = {
  list(postId: string, signal?: AbortSignal): Promise<readonly CommentThread[]>;
  create(postId: string, input: CommentCreateInput): Promise<CommentDisplay>;
  createReply(postId: string, input: CommentReplyInput): Promise<CommentDisplay>;
};
