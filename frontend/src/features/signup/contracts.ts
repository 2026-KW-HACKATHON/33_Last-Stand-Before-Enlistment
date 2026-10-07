import type { ComponentType } from "react";
import type { NavigationEntry, SessionState, TargetAvailability } from "../../lib/navigation";

/** Client form/props only, not Backend DTOs, region identifiers or role enums. #43 consumes these slots. */
export type SignupProfile = { nickname: string; bio: string; attributes: string[] };
export type SignupRegion = { reference: string; label: string };
export type SignupDraft = {
  agreements: { terms: boolean; privacy: boolean; marketing: boolean };
  profile: SignupProfile;
  activityRegion: SignupRegion | null;
};
export type ProfileStepProps = { value: SignupProfile; onChange: (value: SignupProfile) => void; disabled: boolean };
export type RegionStepProps = { value: SignupRegion | null; onChange: (value: SignupRegion | null) => void; disabled: boolean };
export type SignupEditors = { Profile: ComponentType<ProfileStepProps>; Region: ComponentType<RegionStepProps> };
export type SignupAccess = "loading" | "unverified" | "error" | "verified-incomplete" | "member";
export type SignupFailure = { kind: "failure"; reason: "failed" | "nickname" | "region" | "forbidden" | "unavailable" | "unknown"; retryable: boolean };
export type SignupMember = Extract<SessionState, { status: "member" }>;
export interface SignupService {
  readonly source: "mock" | "api";
  /** Adapter binds the verified principal; no email/token/userId is supplied by the form. */
  complete(draft: SignupDraft, signal: AbortSignal): Promise<{ kind: "completed"; session: SignupMember } | SignupFailure>;
  /** Retried independently after confirmed creation; never creates a second member. */
  resolveTarget(target: NavigationEntry | null, signal: AbortSignal): Promise<TargetAvailability>;
}
