import type { AgendaPostDisplay } from "../post/model";

/** FE display state; this is not a backend summary DTO. */
export type SummaryResult =
  | { status: "SUCCEEDED"; summary: string }
  | { status: "SOURCE_TOO_SHORT"; message: string }
  | { status: "FAILED"; message: string };

export type SummaryState =
  | { kind: "loading" }
  | { kind: "success"; summary: string }
  | { kind: "fallback"; message: string }
  | { kind: "error"; message: string };

export type SummarySource = Pick<AgendaPostDisplay, "id" | "title" | "content" | "referenceLink">;
