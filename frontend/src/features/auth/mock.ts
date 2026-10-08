import type { LoginFailure, OtpLoginService } from "./contracts";
import type { SessionState } from "../../lib/navigation";

export const initialLoginSessions = {
  guest: { status: "guest" },
  loading: { status: "loading" },
  error: { status: "error" },
  member: { status: "member", capabilities: { status: "ready", grants: [] } },
} as const satisfies Record<string, SessionState>;

/** Development fixtures, not accounts, codes, Privy policy, wire DTOs or permission grants. */
export const loginScenarios = {
  member: "기존 회원",
  newcomer: "로컬 가입 미완료",
  "send-failure": "코드 요청 실패 → 재시도",
  "verify-failure": "인증 실패 → 재시도",
  expired: "만료 → 재요청",
  limited: "Provider 요청 제한",
  cancelled: "Provider 취소",
  "session-failure": "회원 조회 실패 → 재시도",
  deleted: "원 게시물 이용 불가",
  "availability-error": "원 게시물 조회 실패 → 재시도",
} as const;
export type LoginScenario = keyof typeof loginScenarios;
const failure = (reason: LoginFailure["reason"], retryable = true): LoginFailure => ({ kind: "failure", reason, retryable });

export function createMockLoginService(scenario: LoginScenario, delayMs = 0): OtpLoginService {
  let sends = 0, verifications = 0, resolutions = 0;
  async function wait(signal: AbortSignal) {
    signal.throwIfAborted();
    if (!delayMs) return;
    await new Promise<void>((resolve, reject) => {
      const abort = () => { clearTimeout(timer); reject(new DOMException("Cancelled", "AbortError")); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delayMs);
      signal.addEventListener("abort", abort, { once: true });
    });
    signal.throwIfAborted();
  }
  return {
    source: "mock",
    async sendCode(_email, signal) {
      await wait(signal); sends++;
      if (scenario === "send-failure" && sends === 1) return failure("failed");
      if (scenario === "limited") return failure("rate-limited", false);
      return { kind: "sent" };
    },
    async verifyCode(_email, _code, signal) {
      await wait(signal); verifications++;
      if (scenario === "cancelled" && verifications === 1) return failure("cancelled");
      if (scenario === "verify-failure" && verifications === 1) return failure("failed");
      if (scenario === "expired" && sends === 1) return failure("expired");
      return { kind: "verified" };
    },
    async resolveSession(_target, signal) {
      await wait(signal); resolutions++;
      if (scenario === "session-failure" && resolutions === 1) return failure("failed");
      const session = scenario === "newcomer" ? { status: "signup-incomplete" as const }
        : { status: "member" as const, capabilities: { status: "ready" as const, grants: [] } };
      return { kind: "resolved", session, availability: scenario === "deleted" ? "unavailable"
        : scenario === "availability-error" && resolutions === 1 ? "error" : "available" };
    },
  };
}
