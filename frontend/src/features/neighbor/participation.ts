"use client";

import { useSession } from "../../lib/navigation";
import { neighborParticipation } from "./model";
import { useNeighbor } from "./provider";

/**
 * Read-only participation decision for a post's region.
 *
 * This boundary never grants authority from an activity region, institution
 * status, or a client-side role toggle. It only combines the session adapter's
 * scoped grant with the completed-region adapter's result.
 */
export function useResidentParticipation(regionId: string) {
  const { session, retry: retrySession } = useSession();
  const { state, retry: retryRegions } = useNeighbor();
  const result = neighborParticipation(state, session, regionId);

  return {
    result,
    canParticipate: result.status === "allowed",
    retry: () => {
      void retrySession?.();
      void retryRegions();
    },
  };
}
