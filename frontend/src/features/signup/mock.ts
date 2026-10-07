import type { SignupDraft, SignupFailure, SignupService } from "./contracts";
import type { NavigationEntry } from "../../lib/navigation";

/** Development assumptions, never Provider proof, server DTOs, real IDs or stored accounts. */
export const signupScenarios = { success: "정상", failure: "가입 실패 → 재시도", nickname: "닉네임 중복", region: "지역 오류", forbidden: "권한 제한", unknown: "결과 불명", "target-error": "복귀 조회 실패", deleted: "복귀 대상 삭제", "already-linked": "이미 연결된 회원 확인" } as const;
export type SignupScenario = keyof typeof signupScenarios;
export const signupRegionFixtures = [
  { reference: "preview-wolgye1", label: "서울 노원구 월계1동" },
  { reference: "preview-wolgye2", label: "서울 노원구 월계2동" },
  { reference: "preview-wolgye3", label: "서울 노원구 월계3동" },
];
export const signupAttributeFixtures = ["거주자", "학생", "직장인", "상인"];
export const signupSharedEntry: NavigationEntry = { destination: { id: "sharedPost", params: { postId: "preview-post" } }, sharedContextRef: "preview-context" };
export const signupSessionFixtures = { guest: { status: "guest" }, loading: { status: "loading" }, error: { status: "error" }, incomplete: { status: "signup-incomplete" }, member: { status: "member", capabilities: { status: "ready", grants: [] } } } as const;

export function createMockSignupService(scenario: SignupScenario = "success", delayMs = 0): SignupService & { getSavedDrafts(): readonly SignupDraft[] } {
  const saved: SignupDraft[] = [];
  let calls = 0, targetCalls = 0;
  async function wait(signal: AbortSignal) {
    signal.throwIfAborted();
    if (delayMs) await new Promise<void>((resolve, reject) => {
      const abort = () => { clearTimeout(timer); reject(new DOMException("Cancelled", "AbortError")); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delayMs);
      signal.addEventListener("abort", abort, { once: true });
    });
    signal.throwIfAborted();
  }
  return {
    source: "mock",
    getSavedDrafts: () => structuredClone(saved),
    async complete(draft, signal) {
      await wait(signal); calls++;
      const reason: SignupFailure["reason"] | null = scenario === "failure" && calls === 1 ? "failed"
        : scenario === "nickname" && draft.profile.nickname === "중복" ? "nickname"
        : scenario === "region" && calls === 1 ? "region"
        : scenario === "forbidden" ? "forbidden" : scenario === "unknown" ? "unknown" : null;
      if (reason) return { kind: "failure", reason, retryable: reason !== "forbidden" && reason !== "unknown" };
      // Same bound mock principal, not a duplicate member creation or proof of Backend idempotency.
      if (!saved.length && scenario !== "already-linked") saved.push(structuredClone(draft));
      return { kind: "completed", session: signupSessionFixtures.member };
    },
    async resolveTarget(_target, signal) {
      await wait(signal); targetCalls++;
      return scenario === "target-error" && targetCalls === 1 ? "error" : scenario === "deleted" ? "unavailable" : "available";
    },
  };
}
