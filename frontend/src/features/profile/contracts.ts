import type { SignupProfile, SignupRegion } from "../signup/contracts";
export type ProfileValue = SignupProfile & { activityRegion: SignupRegion | null; photoUrl?: string };
export type FailureReason = "failed" | "nickname" | "region" | "forbidden" | "unavailable" | "unknown";
export type Result<T> = { kind: "success"; value: T } | { kind: "failure"; reason: FailureReason; retryable: boolean };
/** Client ports, not Backend DTOs. A real adapter binds the verified principal. */
export interface ProfileService {
  source: "mock" | "api";
  load(signal: AbortSignal): Promise<Result<ProfileValue | null>>;
  save(value: ProfileValue, signal: AbortSignal): Promise<Result<ProfileValue>>;
  search(query: string, signal: AbortSignal): Promise<Result<SignupRegion[]>>;
  locate(signal: AbortSignal): Promise<{ kind: "success"; value: SignupRegion[] } | { kind: "denied" | "failed" | "unavailable" }>;
  /** Explicit agreed policy. Absence disables new photo selection. */
  photoPolicy?: { accept: string; validate(file: File): string | null };
}
