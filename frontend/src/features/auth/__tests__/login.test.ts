import assert from "node:assert/strict";
import { test } from "node:test";
import { createNavigationStore } from "../../../lib/navigation/state";
import { createMockLoginService } from "../mock";
import { createLoginLifecycle, createLoginStore } from "../state";
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


test("account switch, guest transition and new attempt discard successful login results", async () => {
  for (const next of [{ guest: true, subjectKey: null, attempt: 1 }, { guest: false, subjectKey: "B", attempt: 1 }, { guest: false, subjectKey: "A", attempt: 2 }]) {
    const lifecycle = createLoginLifecycle(createMockLoginService("member"));
    const scope = { guest: false, subjectKey: "A", attempt: 1 };
    const old = lifecycle.getStore(scope);
    old.setEmail("a@example.test"); await old.send(); await old.verify("code", null);
    assert.ok(old.getState().resolution);
    assert.equal(lifecycle.getStore(scope), old);
    const current = lifecycle.getStore(next);
    assert.equal(current.getState().resolution, null);
    assert.equal(current.getState().providerVerified, false);
    assert.equal(current.getState().email, "");
    current.setEmail("b@example.test"); await current.send(); await current.verify("new-code", null);
    assert.equal(current.getState().resolution?.session.status, "member");
    lifecycle.dispose();
  }
});
test("cancel then reentry requires fresh verification", async () => {
  const lifecycle = createLoginLifecycle(createMockLoginService("member"));
  const login = lifecycle.getStore({ guest: true, subjectKey: null, attempt: 1 });
  login.setEmail("a@example.test"); await login.send(); await login.verify("code", null);
  login.cancel();
  const current = lifecycle.getStore({ guest: true, subjectKey: null, attempt: 2 });
  assert.equal(current.getState().resolution, null);
  await current.retrySession(null);
  assert.equal(current.getState().resolution, null);
  current.setEmail("a@example.test"); await current.send(); await current.verify("code", null);
  assert.ok(current.getState().resolution);
  lifecycle.dispose();
});
test("account switch aborts pending verification and ignores its late result", async () => {
  let complete!: (value: { kind: "verified" }) => void;
  let signal!: AbortSignal;
  let resolved = 0;
  const mock = createMockLoginService("member");
  const lifecycle = createLoginLifecycle({ ...mock, verifyCode: async (_email, _code, nextSignal) => { signal = nextSignal; return new Promise(resolve => { complete = resolve; }); }, resolveSession: async (...args) => { resolved++; return mock.resolveSession(...args); } });
  const old = lifecycle.getStore({ guest: false, subjectKey: "A", attempt: 1 });
  old.setEmail("a@example.test"); await old.send();
  const pending = old.verify("code", null);
  const current = lifecycle.getStore({ guest: false, subjectKey: "B", attempt: 1 });
  assert.ok(signal.aborted); complete({ kind: "verified" }); await pending;
  assert.equal(resolved, 0); assert.equal(current.getState().resolution, null);
  lifecycle.dispose();
});


test("first authenticated identity preserves the current signup handoff", async () => {
  const lifecycle = createLoginLifecycle(createMockLoginService("newcomer"));
  const login = lifecycle.getStore({ guest: true, subjectKey: null, attempt: 1 });
  login.setEmail("new@example.test"); await login.send(); await login.verify("code", null);
  const resolution = login.getState().resolution;
  assert.equal(resolution?.session.status, "signup-incomplete");
  const current = lifecycle.getStore({ guest: false, subjectKey: "first-member", attempt: 1 });
  assert.equal(current, login);
  assert.equal(current.getState().resolution, resolution);
  lifecycle.dispose();
});
