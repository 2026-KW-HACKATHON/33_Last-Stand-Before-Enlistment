import type { ReactionSnapshot, ReactionType } from "./model";
export type ReactionService = { get(postId: string): Promise<ReactionSnapshot>; set(postId: string, type: ReactionType, selected: boolean): Promise<ReactionSnapshot> };
