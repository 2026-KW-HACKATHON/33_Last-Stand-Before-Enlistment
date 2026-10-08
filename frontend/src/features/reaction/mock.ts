import { snapshot, type ReactionSnapshot } from "./model";
import type { ReactionService } from "./service";
export type ReactionMockMode = "success" | "load-error" | "register-error" | "cancel-error" | "zero";
const base = (): ReactionSnapshot => snapshot({ EMPATHY: 8, NEEDED: 3, CURIOUS: 1 }, []);
export function createReactionMockService(mode: ReactionMockMode = "success"): ReactionService {
  let value = mode === "zero" ? snapshot({ EMPATHY: 0, NEEDED: 0, CURIOUS: 0 }, []) : base();
  return { async get() { if (mode === "load-error") throw new Error("반응을 불러오지 못했습니다."); return value; }, async set(_postId, type, selected) { await new Promise((resolve) => setTimeout(resolve, 350)); if (selected && mode === "register-error") throw new Error("반응 등록에 실패했습니다."); if (!selected && mode === "cancel-error") throw new Error("반응 취소에 실패했습니다."); const chosen = new Set(value.selected); if (selected) chosen.add(type); else chosen.delete(type); value = snapshot({ ...value.counts, [type]: Math.max(0, value.counts[type] + (selected ? 1 : -1)) }, [...chosen]); return value; } };
}
