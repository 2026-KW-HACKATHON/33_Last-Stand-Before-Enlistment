import type { EvaluationSnapshot, EvaluationType } from "./evaluation";

/** Real adapters implement this only after the comment-evaluation contract is confirmed. */
export type EvaluationService = {
  list(postId: string, signal?: AbortSignal): Promise<readonly EvaluationSnapshot[]>;
  set(commentId: string, selected: EvaluationType): Promise<EvaluationSnapshot>;
};
