import { signupRegionFixtures } from "../signup/mock";
import type { SignupRegion } from "../signup/contracts";
import { normalizeRegions, type InterestRegionService } from "./model";

/** Development fixtures only. Never server region IDs or evidence of persistent API storage. */
export const interestRegionFixtures: SignupRegion[] = [
  ...signupRegionFixtures.slice(0, 2).map(region => ({ ...region, label: region.label.split(" ").at(-1)! })),
  { reference: "preview-hagye1", label: "하계1동" },
];
export const interestRegionScenarios = ["success", "empty", "load-error", "search-error", "save-error", "pending"] as const;
export type InterestRegionScenario = typeof interestRegionScenarios[number];
export function createMockInterestRegionService(scenario: InterestRegionScenario = "success", delayMs = 250): InterestRegionService & { getSaved(): SignupRegion[] } {
  let saved = scenario === "empty" ? [] : normalizeRegions([interestRegionFixtures[0]]);
  let loadCalls = 0, searchCalls = 0, saveCalls = 0;
  async function wait(signal: AbortSignal) {
    signal.throwIfAborted();
    await new Promise<void>((resolve, reject) => {
      const abort = () => { clearTimeout(timer); reject(new DOMException("Cancelled", "AbortError")); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delayMs);
      signal.addEventListener("abort", abort, { once: true });
    });
    signal.throwIfAborted();
  }
  return {
    source: "mock",
    getSaved: () => normalizeRegions(saved),
    async load(signal) { await wait(signal); if (scenario === "load-error" && ++loadCalls === 1) throw new Error("Mock load failure"); return normalizeRegions(saved); },
    async search(query, signal) { await wait(signal); if (scenario === "search-error" && ++searchCalls === 1) throw new Error("Mock search failure"); return interestRegionFixtures.filter(region => region.label.includes(query.trim())).map(region => ({ ...region })); },
    async save(regions, signal) {
      await wait(signal);
      if (scenario === "pending") await new Promise<void>((_, reject) => { signal.addEventListener("abort", () => reject(new DOMException("Cancelled", "AbortError")), { once: true }); });
      if (scenario === "save-error" && ++saveCalls === 1) throw new Error("Mock save failure");
      signal.throwIfAborted();
      saved = normalizeRegions(regions);
      return normalizeRegions(saved);
    },
  };
}
