import type { ProfileService, ProfileValue } from "./contracts";
import { signupRegionFixtures } from "../signup/mock";
export const scenarios = ["success", "load-error", "empty", "save-error", "nickname", "forbidden", "unknown", "denied", "location-error", "no-candidates", "search-error"] as const;
export type Scenario = typeof scenarios[number];
export function createMockProfileService(scenario: Scenario = "success", delay = 250): ProfileService & { getSaved(): ProfileValue | null } {
  let saved: ProfileValue | null = scenario === "empty" ? null : { nickname: "동네이웃", bio: "우리 동네 이야기에 관심이 많아요.", attributes: ["거주자", "학생"], activityRegion: signupRegionFixtures[0], photoUrl: "/images/profile-preview.png" };
  let loads = 0, saves = 0;
  async function wait(signal: AbortSignal) { signal.throwIfAborted(); await new Promise<void>((resolve, reject) => { const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delay); const abort = () => { clearTimeout(timer); reject(new DOMException("Cancelled", "AbortError")); }; signal.addEventListener("abort", abort, { once: true }); }); signal.throwIfAborted(); }
  return { source: "mock", getSaved: () => structuredClone(saved),
    // Fixture-only acceptance, not a production file limit or wire policy.
    photoPolicy: { accept: "image/*", validate: file => file.type.startsWith("image/") ? null : "개발 미리보기에서는 이미지 파일을 선택해 주세요." },
    async load(signal) { await wait(signal); return scenario === "load-error" && loads++ === 0 ? { kind: "failure", reason: "failed", retryable: true } : { kind: "success", value: structuredClone(saved) }; },
    async save(value, signal) { await wait(signal); const reason = scenario === "save-error" && saves++ === 0 ? "failed" : scenario === "nickname" && value.nickname === "중복" ? "nickname" : scenario === "forbidden" ? "forbidden" : scenario === "unknown" ? "unknown" : null;
      if (reason) return { kind: "failure", reason, retryable: reason !== "unknown" && reason !== "forbidden" };
      saved = structuredClone(value); if (saved.photo?.kind === "remove") delete saved.photoUrl; return { kind: "success", value: structuredClone(saved) }; },
    async search(query, signal) { await wait(signal); return scenario === "search-error" ? { kind: "failure", reason: "failed", retryable: true } : { kind: "success", value: scenario === "no-candidates" ? [] : signupRegionFixtures.filter(r => r.label.includes(query.trim())) }; },
    async locate(signal) { await wait(signal); return scenario === "denied" ? { kind: "denied" } : scenario === "location-error" ? { kind: "failed" } : { kind: "success", value: scenario === "no-candidates" ? [] : signupRegionFixtures }; },
  };
}
