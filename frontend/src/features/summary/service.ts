import type { AgendaPostDisplay } from "../post/model";
import type { SummaryResult } from "./model";

/** Real adapters can implement this interface after the API contract is verified. */
export type SummaryService = {
  getSummary(post: AgendaPostDisplay, signal?: AbortSignal): Promise<SummaryResult>;
};
