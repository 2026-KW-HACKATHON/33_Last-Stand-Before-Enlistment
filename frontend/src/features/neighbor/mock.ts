import type { CompletedRegion, NeighborService } from "./model";
export const neighborScenarios = ["completed", "empty", "other-region", "three-regions", "loading", "error-retry"] as const;
export type NeighborScenario = typeof neighborScenarios[number];
export const mockTargetRegion = { id: "mock-wolgye1", name: "월계1동" };
export const mockOtherRegion = { id: "mock-hagye2", name: "하계2동" };
export function mockCompletedRegions(scenario: NeighborScenario): readonly CompletedRegion[] {
 if (scenario === "empty") return [];
 if (scenario === "other-region") return [mockOtherRegion];
 if (scenario === "three-regions") return [mockTargetRegion, mockOtherRegion, { id: "mock-hagye1", name: "하계1동" }];
 return [mockTargetRegion];
}
/** Dev/test only. Does not write session, browser storage, or server permissions. */
export function createMockNeighborService(scenario: NeighborScenario): NeighborService {
 let attempts = 0;
 return { async getCompletedRegions(signal) {
  attempts++;
  await new Promise<void>((resolve, reject) => {
   if (signal.aborted) { reject(new Error("Aborted")); return; }
   const abort = () => { clearTimeout(timer); reject(new Error("Aborted")); };
   const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, scenario === "loading" ? 60000 : 150);
   signal.addEventListener("abort", abort, { once: true });
  });
  if (scenario === "error-retry" && attempts === 1) throw new Error("Mock query failure");
  return mockCompletedRegions(scenario);
 } };
}
