import type { NavigationEntry, SessionState, TargetAvailability } from "../../lib/navigation";

/** FE service results only. These are not Backend DTOs or Privy token claims. */
export type LoginFailure = {
  kind: "failure";
  reason: "failed" | "expired" | "cancelled" | "rate-limited" | "unavailable";
  retryable: boolean;
};
export type LocalLoginSession = Extract<SessionState, { status: "member" | "signup-incomplete" }>;
export type LoginResolution = { kind: "resolved"; session: LocalLoginSession; availability: TargetAvailability } | LoginFailure;

export interface OtpLoginService {
  readonly source: "provider" | "mock";
  commitSession?(): void;
  cancel?(): void;
  sendCode(email: string, signal: AbortSignal): Promise<{ kind: "sent" } | LoginFailure>;
  verifyCode(email: string, code: string, signal: AbortSignal): Promise<{ kind: "verified" } | LoginFailure>;
  /** Separate local membership/availability lookup after Provider verification. No implicit grants. */
  resolveSession(target: NavigationEntry | null, signal: AbortSignal): Promise<LoginResolution>;
}
