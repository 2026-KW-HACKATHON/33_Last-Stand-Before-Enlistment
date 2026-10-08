/** FE-only vote display contract. It is not an API DTO. */
export type VoteOption = { id: string; label: string; count: number };

export type VoteSnapshot = {
  options: readonly VoteOption[];
  /** The option actually submitted by this viewer. Never infer it from totals. */
  submittedOptionId?: string;
};

export type VoteState =
  | { kind: "loading" }
  | { kind: "ready"; snapshot: VoteSnapshot }
  | { kind: "error"; message: string };
