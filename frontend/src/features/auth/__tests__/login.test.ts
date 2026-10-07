import assert from "node:assert/strict";
import { test } from "node:test";
import { createNavigationStore } from "../../../lib/navigation/state";
import { createMockLoginService } from "../mock";
import { createLoginStore } from "../state";
import type { OtpLoginService } from "../contracts";

const shared = { destination: { id: "sharedPost" as const, params: { postId: "mock-post" } }, sharedContextRef: "test-context" };
function store(scenario: Parameters<typeof createMockLoginService>[0] = "member") {
  const login = createLoginStore(createMockLoginService(scenario));
  login.setEmail("example@example.test");
  return login;
}

test("no adapter cannot authenticate or invent a Backend endpoint", async () => {
  const login = createLoginStore(null);
  login.setEmail("example@example.test"); await login.send();
  assert.equal(login.getState().failure?.reason, "unavailable");
  assert.equal(login.getState().providerVerified, false);
  assert.equal(login.getState().resolution, null);
});
test("empty/invalid email remains at input; code shape is not a made-up OTP policy", async () => {
  const login = store(); login.setEmail("invalid"); await login.send();
  assert.equal(login.getState().phase, "email");
  login.setEmail("example@example.test"); await login.send();
  await login.verify("", null); assert.equal(login.getState().phase, "code");
  await login.verify("arbitrary-mock-input", null);
  assert.equal(login.getState().resolution?.session.status, "member");
  assert.deepEqual(login.getState().resolution?.session, { status: "member", capabilities: { status: "ready", grants: [] } });
});
test("local signup-incomplete is separate from Provider authentication and retains returnTo", async () => {
  const navigation = createNavigationStore(shared);
  navigation.beginAuthentication();
  const target = navigation.getState().returnTo?.target;
  assert.ok(target);
  const login = store("newcomer"); await login.send(); await login.verify("mock-input", target);
  assert.equal(login.getState().providerVerified, true);
  const resolution = login.getState().resolution; assert.ok(resolution);
  const result = navigation.completeAuthentication(resolution.session, resolution.availability);
  assert.equal(result.status, "ready");
  if (result.status === "ready") assert.equal(result.href, "/signup");
  assert.deepEqual(navigation.getState().returnTo?.target, target);
});
test("existing member returns to original post without any action payload or replay", async () => {
  const navigation = createNavigationStore(shared); navigation.beginAuthentication();
  const login = store(); await login.send(); await login.verify("mock-input", navigation.getState().returnTo?.target ?? null);
  const resolution = login.getState().resolution; assert.ok(resolution);
  const result = navigation.completeAuthentication(resolution.session, resolution.availability);
  assert.equal(result.status, "ready");
  if (result.status === "ready") assert.equal(result.href, "/posts/mock-post");
  assert.equal(navigation.getState().returnTo, null);
  assert.equal(navigation.getState().current?.sharedContextRef, "test-context");
});
test("without returnTo a member hands off to home", async () => {
  const login = store(); await login.send(); await login.verify("mock-input", null);
  const resolution = login.getState().resolution; assert.ok(resolution);
  const result = createNavigationStore().completeAuthentication(resolution.session, resolution.availability);
  assert.equal(result.status, "ready");
  if (result.status === "ready") assert.equal(result.href, "/home");
});
test("request failure preserves email and only explicit retry succeeds", async () => {
  const login = store("send-failure"); await login.send();
  assert.equal(login.getState().phase, "email"); assert.equal(login.getState().email, "example@example.test");
  assert.equal(login.getState().failure?.reason, "failed");
  await login.send(); assert.equal(login.getState().phase, "code");
});
test("verification failure keeps the challenge; retry does not issue another code", async () => {
  const login = store("verify-failure"); await login.send(); await login.verify("mock-input", null);
  assert.equal(login.getState().providerVerified, false); assert.equal(login.getState().phase, "code");
  await login.verify("retry-input", null); assert.equal(login.getState().phase, "success");
});
test("expired code cannot be resubmitted; resend recovers without a fabricated timer", async () => {
  const login = store("expired"); await login.send(); await login.verify("mock-input", null);
  assert.equal(login.getState().failure?.reason, "expired");
  await login.verify("again", null); assert.equal(login.getState().providerVerified, false);
  await login.send(); await login.verify("new-mock-input", null); assert.equal(login.getState().phase, "success");
});
test("Provider rate-limit result disables retries; no local OTP cooldown is assumed", async () => {
  const login = store("limited"); await login.send();
  assert.equal(login.getState().failure?.reason, "rate-limited");
  assert.equal(login.getState().failure?.retryable, false);
  await login.send(); assert.equal(login.getState().phase, "email");
});
test("local membership failure can retry independently of OTP verification", async () => {
  const login = store("session-failure"); await login.send(); await login.verify("mock-input", null);
  assert.equal(login.getState().providerVerified, true); assert.equal(login.getState().resolution, null);
  await login.retrySession(null); assert.equal(login.getState().phase, "success");
});
test("target lookup failure does not falsely complete; explicit retry recovers", async () => {
  const login = store("availability-error"); await login.send(); await login.verify("mock-input", null);
  assert.equal(login.getState().resolution, null); await login.retrySession(null);
  assert.equal(login.getState().phase, "success");
});
test("deleted target safely falls back to home", async () => {
  const login = store("deleted"), navigation = createNavigationStore(shared);
  navigation.beginAuthentication(); await login.send(); await login.verify("mock-input", navigation.getState().returnTo?.target ?? null);
  const resolution = login.getState().resolution; assert.ok(resolution);
  const result = navigation.completeAuthentication(resolution.session, resolution.availability);
  assert.equal(result.status, "ready");
  if (result.status === "ready") { assert.equal(result.notice, "unavailable"); assert.equal(result.href, "/home"); }
});
test("pending double submission is ignored and late completion cannot undo cancel", async () => {
  let complete: ((value: { kind: "sent" }) => void) | undefined, calls = 0;
  const service: OtpLoginService = { ...createMockLoginService("member"), sendCode: async () => {
    calls++; return new Promise(resolve => { complete = resolve; });
  } };
  const login = createLoginStore(service); login.setEmail("example@example.test");
  const pending = login.send(); await login.send(); assert.equal(calls, 1);
  assert.equal(login.getState().phase, "sending"); login.cancel();
  assert.ok(complete); complete({ kind: "sent" }); await pending;
  assert.equal(login.getState().phase, "email"); assert.equal(login.getState().failure?.reason, "cancelled");
  assert.equal(login.getState().email, "example@example.test");
});
test("cancel restores shared origin while change-email preserves the email only", async () => {
  const navigation = createNavigationStore(shared); navigation.beginAuthentication();
  const login = store(); await login.send(); login.changeEmail();
  assert.equal(login.getState().phase, "email"); assert.equal(login.getState().email, "example@example.test");
  login.cancel(); const result = navigation.cancelAuthentication();
  if (result.status === "ready") assert.equal(result.href, "/shared/posts/mock-post");
});
test("unknown service exceptions cannot expose account-existence or credential details", async () => {
  const service = { ...createMockLoginService("member"), sendCode: async () => { throw new Error("SECRET_ACCOUNT_DETAILS"); } };
  const login = createLoginStore(service); login.setEmail("example@example.test"); await login.send();
  assert.equal(login.getState().failure?.reason, "failed"); assert.ok(!JSON.stringify(login.getState()).includes("SECRET_ACCOUNT_DETAILS"));
  assert.ok(!Object.hasOwn(login.getState(), "code")); assert.ok(!Object.hasOwn(login.getState(), "token"));
});
