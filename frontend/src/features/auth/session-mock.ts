import type { SessionState } from "../../lib/navigation";
import type { AuthSessionAdapter } from "./session-adapter";

export type SessionMockMode = "guest" | "signup-incomplete" | "member" | "loading-error" | "capabilities-loading" | "capabilities-error";

/** Development adapter only; it never represents Privy claims or a backend response DTO. */
export function createMockAuthSessionAdapter(mode: SessionMockMode): AuthSessionAdapter {
  return {
    source: "mock",
    async resolve(): Promise<SessionState> {
      if (mode === "loading-error") throw new Error("Mock session failure");
      if (mode === "guest") return { status: "guest" };
      if (mode === "signup-incomplete") return { status: "signup-incomplete" };
      if (mode === "capabilities-loading") return { status: "member", capabilities: { status: "loading" } };
      if (mode === "capabilities-error") return { status: "member", capabilities: { status: "error" } };
      return { status: "member", capabilities: { status: "ready", grants: [] } };
    },
  };
}
