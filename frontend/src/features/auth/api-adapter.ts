import { createApiClient } from "../../lib/api/client";
import { ApiError } from "../../lib/api/error";
import type { ApiTransport } from "../../lib/api/types";
import type { SessionState, NavigationEntry, TargetAvailability } from "../../lib/navigation";
import type { OtpLoginService, LoginFailure } from "./contracts";
import type { SignupService } from "../signup/contracts";
import type { CurrentDeviceLogoutService } from "../logout/model";
import { createFeatureServices } from "../integration";
import { data, object, id, timestamp, inputId } from "../integration/wire";

/** SDK-owned tokens only; no token copies, storage or local token verification. */
export type PrivyPort = {
  ready: boolean; authenticated: boolean; principal: string | null;
  getAccessToken(): Promise<string | null>;
  sendCode(email: string): Promise<void>;
  loginWithCode(code: string): Promise<void>;
  logout(): Promise<void>;
};
export function createPrivyPortBridge(initial: PrivyPort) {
  let port = initial;
  const listeners = new Set<() => void>();
  return { read: () => port, update: (next: PrivyPort) => { port = next; listeners.forEach(listener => listener()); },
    waitForAuthentication() {
      if (port.ready && port.authenticated && port.principal) return Promise.resolve();
      return new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => { listeners.delete(check); reject(new Error("Provider state did not settle")); }, 15000);
        const check = () => { if (port.ready && port.authenticated && port.principal) { clearTimeout(timeout); listeners.delete(check); resolve(); } };
        listeners.add(check); check();
      });
    },
  };
}
export type AuthSnapshot = { session: SessionState; subjectKey: string | null; principal?: string | null };
export function decodeMembership(value: unknown): { status: "signup-incomplete" | "member"; subjectKey: string | null } {
  const row = object(value);
  if (row.registrationStatus === "NOT_REGISTERED") {
    if (row.member !== null) throw new Error("Unexpected unregistered member");
    return { status: "signup-incomplete", subjectKey: null };
  }
  const member = object(row.member), subjectKey = id(member.id);
  if (row.registrationStatus === "INCOMPLETE" && member.registrationCompletedAt === null) return { status: "signup-incomplete", subjectKey };
  if (row.registrationStatus !== "COMPLETED") throw new Error("Unknown membership status");
  timestamp(member.registrationCompletedAt);
  return { status: "member", subjectKey };
}
function loginFailure(error: unknown): LoginFailure {
  const code = error && typeof error === "object" && "code" in error ? error.code : undefined;
  return { kind: "failure", reason: code === "too_many_requests" || error instanceof ApiError && error.kind === "rate-limit" ? "rate-limited" : code === "exited_auth_flow" ? "cancelled" : error instanceof ApiError && error.kind === "unavailable" ? "unavailable" : "failed", retryable: code !== "missing_or_invalid_privy_app_id" && code !== "allowlist_rejected" };
}
export function createAuthRuntime(baseUrl: string, getPort: () => PrivyPort, transport?: ApiTransport) {
  let snapshot: AuthSnapshot = { session: { status: "loading" }, subjectKey: null };
  let pending: AuthSnapshot | null = null, held = false, generation = 0, otpAttempt = 0;
  let refreshController: AbortController | null = null;
  const listeners = new Set<() => void>();
  const publish = (value: AuthSnapshot) => { snapshot = value; listeners.forEach(listener => listener()); };
  const client = createApiClient({ baseUrl, transport,
    mapError: body => {
      if (!body || typeof body !== "object") return {};
      const row = body as Record<string, unknown>;
      return { code: typeof row.code === "string" ? row.code : undefined, details: row.details, traceId: typeof row.traceId === "string" ? row.traceId : undefined };
    },
    async prepareRequest({ signal }) {
      signal?.throwIfAborted();
      const before = getPort();
      if (!before.ready) throw new Error("Authentication provider not ready");
      if (!before.authenticated) return { credentials: "omit" };
      const token = await before.getAccessToken(), after = getPort();
      signal?.throwIfAborted();
      if (!token || !after.authenticated || !before.principal || after.principal !== before.principal) throw new Error("Authentication changed");
      return { credentials: "omit", headers: { Authorization: `Bearer ${token}` } };
    },
  });
  const services = createFeatureServices(client);
  async function lookup(signal: AbortSignal): Promise<AuthSnapshot> {
    const principal = getPort().principal;
    if (!getPort().ready) return { session: { status: "loading" }, subjectKey: null };
    if (!getPort().authenticated) return { session: { status: "guest" }, subjectKey: null };
    const member = await data(client, "auth/login", decodeMembership, { signal, method: "POST", json: {} });
    if (getPort().principal !== principal || !getPort().authenticated) throw new Error("Membership identity changed");
    if (member.status === "signup-incomplete") return { session: { status: "signup-incomplete" }, subjectKey: member.subjectKey, principal };
    const [profile, neighbors, institution] = await Promise.all([
      services.profileService.load(signal), services.neighborService.getCompletedRegions(signal), services.institutionService.getQualification(signal),
    ]);
    signal.throwIfAborted();
    if (getPort().principal !== principal || institution.subjectId !== member.subjectKey || profile.kind !== "success" || !profile.value) throw new Error("Membership scope changed");
    const regionIds = new Set(neighbors.map(region => region.id));
    if (profile.value.activityRegion) regionIds.add(profile.value.activityRegion.reference);
    const grants: Extract<SessionState, { status: "member" }>["capabilities"] = { status: "ready", grants: [
      ...Array.from(regionIds, regionId => ({ capability: { kind: "neighbor-region" as const, regionId }, allowed: neighbors.some(region => region.id === regionId) })),
      { capability: { kind: "institution" as const }, allowed: institution.status === "completed" },
      ...(institution.status === "completed" ? institution.responsibleRegions.map(region => ({ capability: { kind: "responsible-region" as const, regionId: region.id }, allowed: true })) : []),
    ] };
    return { session: { status: "member", capabilities: grants }, subjectKey: member.subjectKey, principal };
  }
  async function refresh() {
    refreshController?.abort(); const controller = new AbortController(); refreshController = controller;
    const version = ++generation; held = false; pending = null;
    publish({ session: { status: "loading" }, subjectKey: snapshot.subjectKey, principal: snapshot.principal });
    try { const next = await lookup(controller.signal); if (version === generation && !controller.signal.aborted) publish(next); }
    catch { if (version === generation && !controller.signal.aborted) publish({ session: { status: "error" }, subjectKey: null }); }
  }
  async function availability(target: NavigationEntry | null, signal: AbortSignal): Promise<TargetAvailability> {
    if (!target || !("params" in target.destination)) return "available";
    try { await services.postService.getPost(target.destination.params.postId, signal); return "available"; }
    catch (error) { if (error instanceof ApiError && [403, 404, 410].includes(error.status ?? 0)) return "unavailable"; throw error; }
  }
  const commitSession = () => {
    if (!pending) return;
    if (pending.principal !== getPort().principal || !getPort().authenticated) { pending = null; void refresh(); return; }
    const next = pending; pending = null; held = false; publish(next);
  };
  const loginService: OtpLoginService = {
    source: "provider", commitSession,
    cancel() {
      pending = null; held = false;
      if (snapshot.session.status === "guest" && getPort().authenticated) void getPort().logout().then(refresh, refresh);
    },
    async sendCode(email, signal) {
      try { signal.throwIfAborted(); held = true; otpAttempt++; await getPort().sendCode(email); signal.throwIfAborted(); return { kind: "sent" }; }
      catch (error) { held = false; return loginFailure(error); }
    },
    async verifyCode(_email, code, signal) {
      const attempt = otpAttempt;
      try {
        signal.throwIfAborted(); await getPort().loginWithCode(code);
        if (signal.aborted) { if (attempt === otpAttempt) { await getPort().logout(); held = false; } return { kind: "failure", reason: "cancelled", retryable: true }; }
        return { kind: "verified" };
      } catch (error) { return loginFailure(error); }
    },
    async resolveSession(target, signal) {
      try {
        const next = await lookup(signal);
        if (next.session.status !== "member" && next.session.status !== "signup-incomplete") throw new Error("Not verified");
        const available = next.session.status === "member" ? await availability(target, signal) : "available";
        signal.throwIfAborted(); if (next.principal !== getPort().principal) throw new Error("Return scope changed"); pending = next;
        return { kind: "resolved", session: next.session, availability: available };
      } catch (error) { return loginFailure(error); }
    },
  };
  const attrs: Record<string, string> = { "거주자": "RESIDENT", "학생": "STUDENT", "직장인": "WORKER", "상인": "MERCHANT" };
  const signupService: SignupService = {
    source: "api", commitSession,
    async complete(draft, signal) {
      try {
        if (draft.profile.photo || draft.profile.attributes.some(value => !attrs[value]) || !draft.activityRegion) throw new Error("Unsupported signup fields");
        const row = await data(client, "auth/sign-up", decodeMembership, { signal, method: "POST", json: {
          agreements: { termsOfService: draft.agreements.terms, privacyCollection: draft.agreements.privacy, marketing: draft.agreements.marketing },
          profile: { nickname: draft.profile.nickname, bio: draft.profile.bio || null, residentAttributes: draft.profile.attributes.map(value => attrs[value]), activityRegionId: inputId(draft.activityRegion.reference) },
        } });
        if (row.status !== "member") throw new Error("Signup not completed");
        const next = await lookup(signal);
        if (next.session.status !== "member" || next.subjectKey !== row.subjectKey) throw new Error("Signup state mismatch");
        pending = next; return { kind: "completed", session: next.session };
      } catch (error) {
        return { kind: "failure", reason: error instanceof ApiError && error.code === "NICKNAME_ALREADY_IN_USE" ? "nickname" : error instanceof ApiError && error.code === "REGION_NOT_FOUND" ? "region" : error instanceof ApiError && [401,403].includes(error.status ?? 0) ? "forbidden" : error instanceof ApiError && error.status && error.status >= 400 && error.status < 500 ? "failed" : "unknown", retryable: error instanceof ApiError && error.status !== undefined && error.status >= 400 && error.status < 500 && error.status !== 401 && error.status !== 403 };
      }
    },
    resolveTarget: availability,
  };
  const logoutService: CurrentDeviceLogoutService = {
    source: "provider",
    async endCurrentDevice(subjectKey, signal) {
      if (subjectKey !== snapshot.subjectKey) return { kind: "failure" };
      try { signal.throwIfAborted(); await getPort().logout(); signal.throwIfAborted(); pending = null; held = false; generation++; refreshController?.abort(); publish({ session: { status: "guest" }, subjectKey: null }); return { kind: "logged-out", subjectKey }; }
      catch { return { kind: "failure" }; }
    },
  };
  return { client, services, loginService, signupService, logoutService, refresh, commitSession,
    getSnapshot: () => snapshot,
    subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    providerChanged() { if (!held || !getPort().authenticated && snapshot.principal) void refresh(); },
    dispose() { generation++; refreshController?.abort(); pending = null; },
  };
}
