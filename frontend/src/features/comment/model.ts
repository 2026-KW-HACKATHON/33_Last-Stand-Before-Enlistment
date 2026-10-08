/** FE display contract. It is intentionally separate from a backend comment DTO. */
export type CommentDisplay = {
  id: string;
  postId: string;
  content: string;
  authorName: string;
  createdAtLabel: string;
  parentCommentId?: string;
  targetAuthorName?: string;
};

export type CommentThread = {
  root: CommentDisplay;
  replies: readonly CommentDisplay[];
};

export type CommentCreateInput = {
  content: string;
};

export type CommentReplyInput = CommentCreateInput & {
  parentCommentId: string;
  targetAuthorName: string;
};

export type CommentPermission = {
  canCreate: boolean;
  /** The caller supplies only an already validated context; this does not infer guest policy. */
  viewerContext?: "member" | "shared-guest";
  restrictionMessage?: string;
};
