import type { WithdrawalFailure, WithdrawalService } from "./model";
export type WithdrawalScenario = "normal" | "verification-error" | "expired" | "cancelled" | "withdrawal-error";
/** Isolated local fixture. Never changes FE2 data or the real DB/Privy account. */
export function createMockWithdrawalService(scenario: WithdrawalScenario = "normal", delay = 300) {
  const tickets = new Map<object, { subjectKey: string }>(); let failed = false;
  let data = { active: true, author: "시연 사용자", anonymousAuthor: "익명", publicRecords: 3, reactions: 7, evaluations: 4, votes: 2, memberLink: true, bookmarks: 2, settings: true, contactEmail: "demo@example.com" as string | null };
  async function wait(signal: AbortSignal) {
    await new Promise<void>((resolve, reject) => { if (signal.aborted) return reject(new Error("Aborted")); const abort = () => { clearTimeout(timer); signal.removeEventListener("abort", abort); reject(new Error("Aborted")); }; const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delay); signal.addEventListener("abort", abort, { once: true }); });
    if (signal.aborted) throw new Error("Aborted");
  }
  const service: WithdrawalService = {
    source: "mock",
    async verify(subjectKey, signal) {
      await wait(signal);
      if (!failed && ["verification-error", "expired", "cancelled"].includes(scenario)) { failed = true; return { kind: "failure", reason: scenario === "verification-error" ? "failed" : scenario as WithdrawalFailure["reason"] }; }
      const confirmation = {}; tickets.set(confirmation, { subjectKey }); return { kind: "verified", confirmation };
    },
    async withdraw(confirmation, signal) {
      await wait(signal); const ticket = tickets.get(confirmation);
      if (!ticket) return { kind: "failure", reason: "expired" };
      if (!failed && scenario === "withdrawal-error") { failed = true; return { kind: "failure", reason: "failed" }; }
      data = { ...data, active: false, author: "회원 탈퇴한 사용자", memberLink: false, bookmarks: 0, settings: false, contactEmail: null };
      tickets.delete(confirmation); return { kind: "withdrawn", subjectKey: ticket.subjectKey };
    },
    release(confirmation) { tickets.delete(confirmation); },
  };
  return { ...service, getFixture: () => ({ ...data }), outstandingConfirmations: () => tickets.size };
}
