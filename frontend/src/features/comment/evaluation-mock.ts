import { emptyEvaluation, type EvaluationSnapshot, type EvaluationType } from "./evaluation";
import type { EvaluationService } from "./evaluation-service";

export type EvaluationMockMode = "success" | "load-error" | "set-error";

const seed: readonly EvaluationSnapshot[] = [
  { commentId: "comment-01", selected: "NONE", likeCount: 3, dislikeCount: 0 },
  { commentId: "comment-02", selected: "NONE", likeCount: 3, dislikeCount: 1 },
  { commentId: "reply-01", selected: "LIKE", likeCount: 1, dislikeCount: 0 },
];
const wait = () => new Promise((resolve) => setTimeout(resolve, 350));

/** Development fixture boundary. It does not define an API path or request DTO. */
export function createEvaluationMockService(mode: EvaluationMockMode = "success"): EvaluationService {
  const values = new Map(seed.map((value) => [value.commentId, { ...value }]));
  let setErrorPending = mode === "set-error";
  return {
    async list(_postId, signal) {
      signal?.throwIfAborted();
      await wait();
      signal?.throwIfAborted();
      if (mode === "load-error") throw new Error("평가 정보를 불러오지 못했습니다. 다시 시도해 주세요.");
      return [...values.values()];
    },
    async set(commentId, selected: EvaluationType) {
      await wait();
      if (setErrorPending) { setErrorPending = false; throw new Error("평가를 반영하지 못했습니다. 다시 시도해 주세요."); }
      const before = values.get(commentId) ?? emptyEvaluation(commentId);
      const next = {
        ...before,
        selected,
        likeCount: Math.max(0, before.likeCount + (selected === "LIKE" ? 1 : 0) - (before.selected === "LIKE" ? 1 : 0)),
        dislikeCount: Math.max(0, before.dislikeCount + (selected === "DISLIKE" ? 1 : 0) - (before.selected === "DISLIKE" ? 1 : 0)),
      };
      values.set(commentId, next);
      return next;
    },
  };
}
