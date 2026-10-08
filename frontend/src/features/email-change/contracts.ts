/** Client ports only. No Privy SDK API, HTTP endpoint, token or wire DTO is defined here. */
export type EmailChangeFailure = "invalid-email" | "same-email" | "duplicate" | "send-failed" | "mismatch" | "expired" | "cancelled" | "failed" | "unavailable";
export type Failure = { kind: "failure"; reason: EmailChangeFailure };
/** Opaque in-memory handle owned by the adapter, never persisted or displayed. */
export type Confirmation = object;
export interface EmailChangeService {
  source: "mock" | "provider";
  begin(subjectKey: string, email: string, signal: AbortSignal): Promise<{ kind: "pending"; confirmation: Confirmation } | Failure>;
  confirm(confirmation: Confirmation, signal: AbortSignal): Promise<{ kind: "verified" } | Failure>;
  change(confirmation: Confirmation, signal: AbortSignal): Promise<{ kind: "changed"; subjectKey: string; registeredEmail: string } | Failure>;
  release(confirmation: Confirmation): void;
}
