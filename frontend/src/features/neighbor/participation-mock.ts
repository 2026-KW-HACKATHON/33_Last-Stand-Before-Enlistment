import type { SessionState } from "../../lib/navigation";
import type { NeighborService } from "./model";
import { createMockNeighborService, mockOtherRegion, mockTargetRegion } from "./mock";

export const residentParticipationScenarios = [
  "completed-region",
  "uncompleted-region",
  "other-region",
  "institution-only",
  "capabilities-loading",
  "capabilities-error",
  "regions-error",
] as const;

export type ResidentParticipationScenario = (typeof residentParticipationScenarios)[number];

export type ResidentParticipationMock = {
  session: SessionState;
  subjectKey: string;
  service: NeighborService;
};

/** Dev-only adapter composition. It supplies assertions; the UI does not create them. */
export function createMockResidentParticipation(scenario: ResidentParticipationScenario): ResidentParticipationMock {
  if (scenario === "capabilities-loading") {
    return { session: { status: "member", capabilities: { status: "loading" } }, subjectKey: "mock-member", service: createMockNeighborService("completed") };
  }
  if (scenario === "capabilities-error") {
    return { session: { status: "member", capabilities: { status: "error" } }, subjectKey: "mock-member", service: createMockNeighborService("completed") };
  }

  const completed = scenario === "completed-region";
  const service = createMockNeighborService(
    scenario === "regions-error" ? "error-retry" : scenario === "other-region" ? "other-region" : completed ? "completed" : "empty",
  );
  return {
    session: {
      status: "member",
      capabilities: {
        status: "ready",
        grants: [
          { capability: { kind: "neighbor-region", regionId: mockTargetRegion.id }, allowed: completed },
          { capability: { kind: "neighbor-region", regionId: mockOtherRegion.id }, allowed: false },
          { capability: { kind: "institution" }, allowed: scenario === "institution-only" },
        ],
      },
    },
    subjectKey: "mock-member",
    service,
  };
}
