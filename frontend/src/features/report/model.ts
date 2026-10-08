/** Client-side submission states; these do not describe a backend report lifecycle. */
export type ReportState = "idle" | "validation" | "pending" | "success" | "failure" | "duplicate";
