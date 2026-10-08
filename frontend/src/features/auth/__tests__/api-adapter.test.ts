import { test } from "node:test";
import assert from "node:assert/strict";
import { createAuthRuntime, createPrivyPortBridge, decodeMembership, type PrivyPort } from "../api-adapter";
import { safeReturnDestination, safeSharedHref } from "../../../lib/navigation/safe-return";
const at = "2026-10-09T09:00:00+09:00";
const region = { id: 1, name: "등록 지역" };
function fixture(status: string = "COMPLETED") {
  const requests: Request[] = [];
  let authenticated = false, principal = "did:privy:synthetic-a", token: () => Promise<string | null> = async () => "synthetic-test-access";
  let failSignupRead = false, writes = 0, verified = 0;
  const port: PrivyPort = { ready: true, authenticated, principal,
    getAccessToken: () => token(), sendCode: async () => {}, loginWithCode: async () => { verified++; authenticated = true; port.authenticated = true; },
    logout: async () => { authenticated = false; port.authenticated = false; },
  };
  const runtime = createAuthRuntime("https://api.example.invalid/api/v1", () => ({ ...port, authenticated, principal }), async request => {
    requests.push(request); const path = new URL(request.url).pathname;
    if (path.endsWith("auth/sign-up")) { writes++; status = "COMPLETED"; return Response.json({ data: { registrationStatus: status, member: { id: 23, registrationCompletedAt: at } } }, { status: 201 }); }
    if (path.endsWith("auth/login")) {
      if (failSignupRead) throw new Error("Synthetic lost read response");
      return Response.json({ data: { registrationStatus: status, member: status === "NOT_REGISTERED" ? null : { id: 23, registrationCompletedAt: status === "INCOMPLETE" ? null : at } } });
    }
    if (path.endsWith("users/me/neighbor-verifications")) return Response.json({ data: { maxVerifiedRegions: 3, verifiedRegions: [{ ...region, verifiedAt: at }] } });
    if (path.endsWith("institution-verifications")) return Response.json({ data: { id: 23, institutionVerified: false, institutionVerification: { status: "NOT_SUBMITTED", isActive: false, institutionId: null, institutionName: null, responsibleRegion: null, completedAt: null, validUntil: null } } });
    if (path.endsWith("users/me")) return Response.json({ data: { id: 23, email: "fixture@example.invalid", profile: { nickname: "이웃", bio: null, profileImageUrl: null, residentAttributes: ["RESIDENT"], activityRegion: region } } });
    if (path.includes("/posts/")) return Response.json({ code: "POST_NOT_FOUND", message: "fixture", details: [], traceId: "fixture" }, { status: 404 });
    return Response.json({ data: [] });
  });
  return { runtime, port, requests, authenticate: () => { authenticated = true; }, switchPrincipal: () => { principal = "did:privy:synthetic-b"; }, setToken: (fn: typeof token) => { token = fn; }, loseSignupRead: () => { failSignupRead = true; }, counts: () => ({ writes, verified }) };
}
const signal = () => new AbortController().signal;
for (const [registrationStatus, member, expected] of [
  ["NOT_REGISTERED", null, "signup-incomplete"],
  ["INCOMPLETE", { id: 23, registrationCompletedAt: null }, "signup-incomplete"],
  ["COMPLETED", { id: 23, registrationCompletedAt: at }, "member"],
] as const) test(`local ${registrationStatus} is independent of Privy account creation`, () => assert.equal(decodeMembership({ registrationStatus, member }).status, expected));
test("rejects inconsistent membership and timezone-free completion", () => {
  for (const row of [{ registrationStatus: "NOT_REGISTERED", member: { id: 23 } }, { registrationStatus: "INCOMPLETE", member: { id: 23, registrationCompletedAt: at } }, { registrationStatus: "COMPLETED", member: { id: "23", registrationCompletedAt: at } }, { registrationStatus: "COMPLETED", member: { id: 23, registrationCompletedAt: "2026-10-09" } }]) assert.throws(() => decodeMembership(row));
});
test("guest session resolves without membership requests or fabricated local ID", async () => {
  const f = fixture(); await f.runtime.refresh(); assert.equal(f.runtime.getSnapshot().session.status, "guest"); assert.equal(f.requests.length, 0); assert.equal(f.runtime.getSnapshot().subjectKey, null);
});
test("OTP authentication holds root identity until accepted navigation commit", async () => {
  const f = fixture(); await f.runtime.refresh();
  await f.runtime.loginService.sendCode("fixture@example.invalid", signal()); await f.runtime.loginService.verifyCode("fixture@example.invalid", "synthetic", signal());
  f.runtime.providerChanged(); const result = await f.runtime.loginService.resolveSession(null, signal());
  assert.equal(result.kind, "resolved"); assert.equal(f.runtime.getSnapshot().session.status, "guest");
  f.runtime.commitSession(); assert.equal(f.runtime.getSnapshot().session.status, "member"); assert.equal(f.runtime.getSnapshot().subjectKey, "23");
  assert.equal(f.requests[0].headers.get("Authorization"), "Bearer synthetic-test-access"); assert.deepEqual(await f.requests[0].json(), {});
  assert.equal(f.requests[0].credentials, "omit");
});
test("region capability grants come only from completed-region API", async () => {
  const f = fixture(); f.authenticate(); await f.runtime.refresh(); const session = f.runtime.getSnapshot().session;
  assert.equal(session.status, "member"); if (session.status === "member" && session.capabilities.status === "ready") {
    assert.deepEqual(session.capabilities.grants, [{ capability: { kind: "neighbor-region", regionId: "1" }, allowed: true }, { capability: { kind: "institution" }, allowed: false }]);
  }
});
test("deleted return target yields unavailable without replaying a mutation", async () => {
  const f = fixture(); f.authenticate(); const result = await f.runtime.loginService.resolveSession({ destination: { id: "post", params: { postId: "101" } } },signal());
  assert.equal(result.kind, "resolved"); if (result.kind === "resolved") assert.equal(result.availability, "unavailable");
  assert.equal(f.requests.filter(row => row.method !== "GET" && !row.url.endsWith("auth/login")).length, 0);
});
test("missing authenticated access token fails closed and does not call transport as guest", async () => {
  const f = fixture(); f.authenticate(); f.setToken(async () => null); await f.runtime.refresh(); assert.equal(f.runtime.getSnapshot().session.status, "error"); assert.equal(f.requests.length, 0);
});
test("identity change while token refresh is pending cannot send the previous principal's token", async () => {
  const f = fixture(); f.authenticate(); const token = Promise.withResolvers<string | null>(); f.setToken(() => token.promise);
  const request = f.runtime.refresh(); f.switchPrincipal(); token.resolve("synthetic-old-token"); await request;
  assert.equal(f.runtime.getSnapshot().session.status, "error"); assert.equal(f.requests.length, 0);
});
const draft = { agreements: { terms: true, privacy: true, marketing: false }, profile: { nickname: "새회원", bio: "", attributes: ["학생"] }, activityRegion: { reference: "1", label: "등록 지역" } };
test("signup sends exact agreement/profile wire and never email, token, role or userId fields", async () => {
  const f = fixture("NOT_REGISTERED"); f.authenticate(); const result = await f.runtime.signupService.complete(draft,signal()); assert.equal(result.kind, "completed");
  const request = f.requests.find(row => row.url.endsWith("auth/sign-up"))!;
  assert.deepEqual(await request.json(), { agreements: { termsOfService: true, privacyCollection: true, marketing: false }, profile: { nickname: "새회원", bio: null, residentAttributes: ["STUDENT"], activityRegionId: 1 } });
  assert.equal(f.counts().writes, 1); f.runtime.commitSession(); assert.equal(f.runtime.getSnapshot().subjectKey, "23");
});
test("confirmed signup with lost follow-up read blocks blind duplicate signup", async () => {
  const f = fixture("NOT_REGISTERED"); f.authenticate(); f.loseSignupRead(); const result = await f.runtime.signupService.complete(draft,signal());
  assert.deepEqual(result, { kind: "failure", reason: "unknown", retryable: false }); assert.equal(f.counts().writes, 1);
});
test("logout requires matching local identity and uses only current-device SDK logout", async () => {
  const f = fixture(); f.authenticate(); await f.runtime.refresh();
  assert.equal((await f.runtime.logoutService.endCurrentDevice("999",signal())).kind, "failure");
  assert.equal((await f.runtime.logoutService.endCurrentDevice("23",signal())).kind, "logged-out"); assert.equal(f.runtime.getSnapshot().session.status, "guest");
});
test("cancelled SDK verification logs out the late verified identity and cannot resolve success", async () => {
  const f = fixture(); const done = Promise.withResolvers<void>(); f.port.loginWithCode = async () => { await done.promise; f.authenticate(); };
  const controller = new AbortController(); const pending = f.runtime.loginService.verifyCode("fixture@example.invalid","synthetic",controller.signal);
  controller.abort(); done.resolve(); assert.equal((await pending).kind, "failure"); await f.runtime.refresh(); assert.equal(f.runtime.getSnapshot().session.status, "guest");
});
test("allowed internal return paths exclude external, encoded redirects, auth loops and queries", () => {
  assert.deepEqual(safeReturnDestination("/posts/101"), { id: "post", params: { postId: "101" } });
  for (const path of ["https://evil.test", "//evil.test", "/\\evil.test", "/login", "/signup", "/shared/posts/101?token=x", "/posts/101?next=https://evil.test", "/posts/%2F%2Fevil.test", "/posts/9007199254740992", "/unknown"]) assert.equal(safeReturnDestination(path),null,path);
});
test("share cancellation preserves only the exact scoped internal share URL in memory", () => {
  assert.equal(safeSharedHref("/shared/posts/101?token=synthetic","101"), "/shared/posts/101?token=synthetic");
  for (const path of ["https://evil.test/shared/posts/101?token=x", "//evil.test/shared/posts/101?token=x", "/shared/posts/102?token=x", "/shared/posts/101?token=x&next=y", "/shared/posts/101?token=x&token=y", "/shared/posts/101?token=x#fragment"]) assert.equal(safeSharedHref(path,"101"),null,path);
});
test("SDK result waits for the React provider's authenticated principal before membership lookup", async () => {
  const f = fixture(); const bridge = createPrivyPortBridge({ ...f.port, authenticated: false, principal: null });
  let settled = false; const waiting = bridge.waitForAuthentication().then(() => { settled = true; });
  await Promise.resolve(); assert.equal(settled,false);
  bridge.update({ ...f.port, authenticated: true, principal: "did:privy:synthetic-a" }); await waiting; assert.equal(settled,true);
});
test("account change after lookup cannot commit a stale resolved local member", async () => {
  const f = fixture(); f.authenticate(); const result = await f.runtime.loginService.resolveSession(null,signal()); assert.equal(result.kind,"resolved");
  f.switchPrincipal(); f.runtime.commitSession(); assert.notEqual(f.runtime.getSnapshot().session.status,"member"); f.runtime.dispose();
});
