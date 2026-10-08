import type { VoteSnapshot } from "./model";

/** The real adapter can implement this after BE #25 agrees its contract. */
export type VoteService = {
  get(postId: string): Promise<VoteSnapshot>;
  submit(postId: string, optionId: string): Promise<VoteSnapshot>;
};
