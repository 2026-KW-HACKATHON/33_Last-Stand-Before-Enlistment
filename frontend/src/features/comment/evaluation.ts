/** FE state, not a backend evaluation DTO. */
export type EvaluationType = "NONE" | "LIKE" | "DISLIKE";

export type EvaluationSnapshot = {
  commentId: string;
  selected: EvaluationType;
  likeCount: number;
  dislikeCount: number;
};

export type EvaluationState = Record<string, EvaluationSnapshot>;

export function emptyEvaluation(commentId: string): EvaluationSnapshot {
  return { commentId, selected: "NONE", likeCount: 0, dislikeCount: 0 };
}

export function nextEvaluation(current: EvaluationSnapshot, requested: Exclude<EvaluationType, "NONE">): EvaluationSnapshot {
  const selected = current.selected === requested ? "NONE" : requested;
  return {
    ...current,
    selected,
    likeCount: Math.max(0, current.likeCount + (selected === "LIKE" ? 1 : 0) - (current.selected === "LIKE" ? 1 : 0)),
    dislikeCount: Math.max(0, current.dislikeCount + (selected === "DISLIKE" ? 1 : 0) - (current.selected === "DISLIKE" ? 1 : 0)),
  };
}
