import type { CommentDisplay, CommentThread } from "./model";
import type { CommentService } from "./service";

export type CommentMockMode = "success" | "empty" | "load-error" | "create-error" | "forbidden";

const seed: readonly CommentThread[] = [{
  root: { id: "comment-01", postId: "agenda-photo", content: "보행로 조명이 보완되면 저녁에도 더 안전하게 다닐 수 있을 것 같아요.", authorName: "김이웃", createdAtLabel: "방금 전", createdAtOrder: 2 },
  replies: [{ id: "reply-01", postId: "agenda-photo", parentCommentId: "comment-01", targetAuthorName: "김이웃", content: "저도 같은 의견입니다.", authorName: "이주민", createdAtLabel: "방금 전", createdAtOrder: 1 }],
}, {
  root: { id: "comment-02", postId: "agenda-photo", content: "보행 환경 개선 우선순위도 함께 논의하면 좋겠습니다.", authorName: "박주민", createdAtLabel: "방금 전", createdAtOrder: 3 },
  replies: [],
}];

const wait = () => new Promise((resolve) => setTimeout(resolve, 350));

/** Development fixture boundary. No API path or backend DTO is implied here. */
export function createCommentMockService(mode: CommentMockMode = "success"): CommentService {
  let threads = mode === "empty" ? [] : seed.map((thread) => ({ ...thread, replies: [...thread.replies] }));
  let sequence = 2;
  let createErrorPending = mode === "create-error";
  const failIfNeeded = async () => {
    await wait();
    if (mode === "forbidden") throw new Error("댓글 작성 권한이 없습니다.");
    if (createErrorPending) { createErrorPending = false; throw new Error("댓글을 등록하지 못했습니다. 다시 시도해 주세요."); }
  };
  return {
    async list(_postId, signal) {
      signal?.throwIfAborted();
      await wait();
      signal?.throwIfAborted();
      if (mode === "load-error") throw new Error("댓글을 불러오지 못했습니다. 다시 시도해 주세요.");
      return threads;
    },
    async create(postId, input) {
      await failIfNeeded();
      const comment: CommentDisplay = { id: `comment-${sequence++}`, postId, content: input.content.trim(), authorName: "나", createdAtLabel: "방금 전", createdAtOrder: Date.now() };
      threads = [...threads, { root: comment, replies: [] }];
      return comment;
    },
    async createReply(postId, input) {
      await failIfNeeded();
      const reply: CommentDisplay = { id: `reply-${sequence++}`, postId, parentCommentId: input.parentCommentId, targetAuthorName: input.targetAuthorName, content: input.content.trim(), authorName: "나", createdAtLabel: "방금 전", createdAtOrder: Date.now() };
      threads = threads.map((thread) => thread.root.id === input.parentCommentId ? { ...thread, replies: [...thread.replies, reply] } : thread);
      return reply;
    },
  };
}
