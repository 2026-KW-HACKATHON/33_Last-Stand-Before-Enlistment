export const reactionTypes = ["EMPATHY", "NEEDED", "CURIOUS"] as const;
export type ReactionType = (typeof reactionTypes)[number];
export type ReactionSnapshot = { counts: Record<ReactionType, number>; selected: readonly ReactionType[]; total: number };
export type ReactionState = { kind: "loading" } | { kind: "ready"; snapshot: ReactionSnapshot } | { kind: "error"; message: string };
export const reactionLabels: Record<ReactionType, string> = { EMPATHY: "공감해요", NEEDED: "필요해요", CURIOUS: "궁금해요" };
export function snapshot(counts: Record<ReactionType, number>, selected: readonly ReactionType[]): ReactionSnapshot { return { counts, selected, total: reactionTypes.reduce((sum, type) => sum + counts[type], 0) }; }
