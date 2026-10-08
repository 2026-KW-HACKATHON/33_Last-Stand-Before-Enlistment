import type { ShareService } from "./service";
export type ShareMockMode = "success" | "error";
export function createShareMockService(mode: ShareMockMode = "success"): ShareService { return { async copy() { if (mode === "error") throw new Error("링크를 복사하지 못했습니다."); } }; }
