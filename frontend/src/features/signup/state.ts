import type { NavigationEntry } from "../../lib/navigation";
import type { SignupAccess, SignupDraft, SignupFailure, SignupMember, SignupService } from "./contracts";

export const emptySignupDraft = (): SignupDraft => ({ agreements: { terms: false, privacy: false, marketing: false }, profile: { nickname: "", bio: "", attributes: [] }, activityRegion: null });
export const requiredAgreements = (draft: SignupDraft) => draft.agreements.terms && draft.agreements.privacy;
export const validProfile = (draft: SignupDraft) => Boolean(draft.profile.nickname.trim()) && Array.from(draft.profile.nickname.trim()).length <= 10 && Array.from(draft.profile.bio).length <= 50;
export type SignupState = {
  access: SignupAccess; draft: SignupDraft; step: "agreements" | "profile" | "region";
  phase: "editing" | "submitting" | "resolving" | "success";
  failure: SignupFailure | null; member: SignupMember | null; availability: "available" | "unavailable" | null;
};
export const signupBusy = (state: SignupState) => state.phase === "submitting" || state.phase === "resolving";
const failure = (reason: SignupFailure["reason"], retryable = true): SignupFailure => ({ kind: "failure", reason, retryable });

/** One root's in-memory draft. No OTP, credentials, storage, implicit permissions or auto resubmission. */
export function createSignupStore(service: SignupService | null) {
  let state: SignupState = { access: "loading", draft: emptySignupDraft(), step: "agreements", phase: "editing", failure: null, member: null, availability: null };
  let generation = 0;
  let operation: AbortController | null = null;
  const listeners = new Set<() => void>();
  function update(next: SignupState) { state = next; listeners.forEach(listener => listener()); }
  function interrupt() {
    operation?.abort(); generation++;
    if (signupBusy(state)) update({ ...state, phase: "editing", failure: failure(state.member ? "failed" : "unknown", Boolean(state.member)) });
  }
  function start(phase: "submitting" | "resolving") {
    operation?.abort(); operation = new AbortController(); const id = ++generation;
    update({ ...state, phase, failure: null });
    return { signal: operation.signal, current: () => id === generation && !operation?.signal.aborted };
  }
  async function resolve(target: NavigationEntry | null) {
    if (!service || !state.member || signupBusy(state) || state.access !== "verified-incomplete") return;
    const request = start("resolving");
    try {
      const availability = await service.resolveTarget(target, request.signal);
      if (!request.current()) return;
      if (availability === "loading" || availability === "error") return update({ ...state, phase: "editing", failure: failure("failed") });
      update({ ...state, phase: "success", availability });
    } catch { if (request.current()) update({ ...state, phase: "editing", failure: failure("failed") }); }
  }
  return {
    getState: () => state,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    setAccess(access: SignupAccess) {
      if (state.access === access) return;
      if (access !== "verified-incomplete") interrupt();
      update({ ...state, access });
    },
    setDraft(draft: SignupDraft) {
      if (state.access !== "verified-incomplete" || signupBusy(state) || state.member) return;
      const keepUnknown = state.failure?.reason === "unknown";
      update({ ...state, draft: structuredClone(draft), failure: keepUnknown ? state.failure : null });
    },
    next() {
      if (state.access !== "verified-incomplete" || signupBusy(state) || state.member) return;
      if (state.step === "agreements") {
        if (requiredAgreements(state.draft)) update({ ...state, step: "profile", failure: state.failure?.reason === "unknown" ? state.failure : null });
      } else if (state.step === "profile" && validProfile(state.draft)) update({ ...state, step: "region", failure: state.failure?.reason === "unknown" ? state.failure : null });
    },
    back() {
      if (signupBusy(state) || state.member) return;
      update({ ...state, step: state.step === "region" ? "profile" : "agreements" });
    },
    reviewProfile() { if (!signupBusy(state) && !state.member) update({ ...state, step: "profile" }); },
    async submit(target: NavigationEntry | null) {
      if (state.access !== "verified-incomplete" || signupBusy(state) || state.phase === "success" || state.failure?.retryable === false) return;
      if (state.member) return resolve(target);
      if (!requiredAgreements(state.draft)) return update({ ...state, step: "agreements" });
      if (!validProfile(state.draft)) return update({ ...state, step: "profile" });
      if (!state.draft.activityRegion?.reference) return update({ ...state, step: "region", failure: failure("region") });
      if (!service) return update({ ...state, failure: failure("unavailable", false) });
      const request = start("submitting");
      try {
        const result = await service.complete(structuredClone(state.draft), request.signal);
        if (!request.current()) return;
        if (result.kind === "failure") return update({ ...state, phase: "editing", failure: result });
        update({ ...state, phase: "editing", member: result.session });
        await resolve(target);
      } catch { if (request.current()) update({ ...state, phase: "editing", failure: failure("unknown", false) }); }
    },
    cancel: interrupt,
    /** #61 calls on account switch/logout; cancellation alone preserves the draft. */
    clear() { interrupt(); update({ access: "loading", draft: emptySignupDraft(), step: "agreements", phase: "editing", failure: null, member: null, availability: null }); },
    dispose() { operation?.abort(); generation++; },
  };
}
export type SignupStore = ReturnType<typeof createSignupStore>;
