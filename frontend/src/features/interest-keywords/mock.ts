import { validateKeywords, type InterestKeyword, type InterestKeywordService } from "./model";
export const keywordScenarios = ["success", "empty", "four", "load-error", "save-error", "pending"] as const;
export type KeywordScenario = typeof keywordScenarios[number];
/** One saved Mock source for load/save and recommendation input; no weights or board filter effects. */
export function createMockKeywordService(scenario: KeywordScenario = "success", delay = 250): InterestKeywordService & { getSaved(): InterestKeyword[] } {
  let saved: InterestKeyword[] = scenario === "empty" ? [] : scenario === "four" ? ["교통", "주거", "안전", "복지"] : ["교통", "주거"];
  let reads = 0, writes = 0;
  async function wait(signal: AbortSignal) {
    signal.throwIfAborted();
    await new Promise<void>((resolve, reject) => {
      const abort = () => { clearTimeout(timer); reject(new DOMException("Cancelled", "AbortError")); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, delay);
      signal.addEventListener("abort", abort, { once: true });
    });
    signal.throwIfAborted();
  }
  return {
    source: "mock", getSaved: () => [...saved],
    async load(signal) { await wait(signal); if (scenario === "load-error" && ++reads === 1) throw new Error("Mock load failure"); return [...saved]; },
    async save(value, signal) {
      await wait(signal);
      if (scenario === "pending") await new Promise<void>((_, reject) => signal.addEventListener("abort", () => reject(new DOMException("Cancelled", "AbortError")), { once: true }));
      if (scenario === "save-error" && ++writes === 1) throw new Error("Mock save failure");
      signal.throwIfAborted(); saved = validateKeywords(value); return [...saved];
    },
  };
}
