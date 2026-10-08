import type { PostFormState } from "./model";

/** FE-only draft boundary. It is not a server DTO or a recovery policy. */
export type DraftResult = { id: string };
export type DraftService = { save(form: PostFormState): Promise<DraftResult> };
export type DraftMockMode = "success" | "error";
export function createDraftMockService(mode: DraftMockMode = "success"): DraftService { return { async save() { if (mode === "error") throw new Error("Mock draft save failed."); return { id: "draft-mock-01" }; } }; }
