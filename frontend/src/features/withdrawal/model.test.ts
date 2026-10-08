import test from "node:test";
import assert from "node:assert/strict";
import { createWithdrawalStore, createWithdrawalLifecycle, withdrawalSession, withdrawAndComplete, finishWithdrawal, type WithdrawalService } from "./model";
import { createMockWithdrawalService } from "./mock";
import { evaluateGuard, createNavigationStore, type SessionState } from "../../lib/navigation";
const member: SessionState = { status: "member", capabilities: { status: "ready", grants: [] } };
async function final(store: ReturnType<typeof createWithdrawalStore>) { store.open(); store.next(); await store.verify(); }
test("caution and verified final consent required; success keeps L12 until explicit A02", async () => {
  const service = createMockWithdrawalService("normal", 0), store = createWithdrawalStore(service, "a", "member");
  assert.equal(await store.withdraw(), false); store.open(); assert.equal(store.getState().phase, "caution"); assert.equal(await store.withdraw(), false);
  store.next(); await store.verify(); assert.equal(store.getState().phase, "final"); assert.equal(withdrawalSession(member, store.getState()), member);
  const nav = createNavigationStore({ destination: { id: "settings" } }); nav.beginAuthentication({ destination: { id: "me" } }); nav.registerSnapshot({ destination: { id: "bookmarks" }, kind: "list", ref: "member-list" });
  assert.equal(await withdrawAndComplete(store, nav), true); assert.equal(store.getState().phase, "complete"); assert.equal(nav.getState().returnTo, null); assert.deepEqual(nav.getState().snapshots, []);
  const guest = withdrawalSession(member, store.getState()); assert.equal(guest.status, "guest"); assert.equal(evaluateGuard({ id: "settings" }, guest).status, "login-required");
  assert.equal(evaluateGuard({ id: "sharedPost", params: { postId: "p" } }, guest, { sharedAccess: { status: "ready", postId: "p" } }).status, "allowed");
  store.cancel(); store.back(); assert.equal(store.getState().phase, "complete");
  finishWithdrawal(store, nav); assert.equal(nav.getState().current?.destination.id, "login"); assert.equal(withdrawalSession(member, store.getState()).status, "guest");
});
test("Mock preserves public records, anonymous display and aggregates; clears only fixture relationships", async () => {
  const service = createMockWithdrawalService("normal", 0), before = service.getFixture(), other = createMockWithdrawalService(); const store = createWithdrawalStore(service, "a", "member"); await final(store); await store.withdraw();
  const after = service.getFixture(); for (const key of ["publicRecords", "reactions", "evaluations", "votes", "anonymousAuthor"] as const) assert.equal(after[key], before[key]);
  assert.equal(after.author, "회원 탈퇴한 사용자"); assert.equal(after.memberLink, false); assert.equal(after.bookmarks, 0); assert.equal(after.settings, false); assert.equal(after.contactEmail, null); assert.equal(after.active, false); assert.equal(other.getFixture().active, true); assert.equal(service.outstandingConfirmations(), 0);
});
for (const scenario of ["verification-error", "expired", "cancelled", "withdrawal-error"] as const) test(`${scenario} keeps member and fixture, then explicit Retry`, async () => {
  const service = createMockWithdrawalService(scenario, 0), before = service.getFixture(), store = createWithdrawalStore(service, "a", "member"); await final(store);
  if (scenario === "withdrawal-error") { assert.equal(await store.withdraw(), false); assert.equal(store.getState().phase, "final"); }
  else assert.equal(store.getState().phase, "verification");
  assert.equal(withdrawalSession(member, store.getState()), member); assert.deepEqual(service.getFixture(), before);
  if (scenario !== "withdrawal-error") await store.verify(); assert.equal(await store.withdraw(), true);
});
test("pending prevents duplicate verification/withdrawal and cancel aborts without data changes", async () => {
  const service = createMockWithdrawalService("normal", 5), store = createWithdrawalStore(service, "a", "member"); store.open(); store.next(); const verify = store.verify(); await store.verify(); store.cancel(); await verify;
  assert.equal(store.getState().phase, "closed"); assert.equal(service.outstandingConfirmations(), 0); assert.equal(service.getFixture().active, true);
  await final(store); const withdraw = store.withdraw(); assert.equal(await store.withdraw(), false); store.cancel(); assert.equal(await withdraw, false); assert.equal(service.getFixture().active, true);
});
test("back invalidates confirmation; cancel/re-entry needs new verification", async () => {
  const service = createMockWithdrawalService("normal", 0), store = createWithdrawalStore(service, "a", "member"); await final(store); store.back(); assert.equal(store.getState().phase, "verification"); assert.equal(service.outstandingConfirmations(), 0); assert.equal(await store.withdraw(), false);
  store.back(); assert.equal(store.getState().phase, "caution"); store.cancel(); await final(store); assert.equal(await store.withdraw(), true);
});
test("account/status changes reject late success and release late verification handles", async () => {
  let resolve!: (result: { kind: "verified"; confirmation: object }) => void, released = 0;
  const service: WithdrawalService = { source: "provider", verify: () => new Promise(done => { resolve = done; }), async withdraw() { return { kind: "withdrawn", subjectKey: "a" }; }, release() { released++; } };
  const lifecycle = createWithdrawalLifecycle(), old = lifecycle.getStore(service, "a", "member"); old.open(); old.next(); const run = old.verify(); const next = lifecycle.getStore(service, "b", "member"); resolve({ kind: "verified", confirmation: {} }); await run; assert.equal(released, 1); assert.equal(next.getState().phase, "closed"); assert.equal(await old.withdraw(), false);
});
test("late processing completion after cancel cannot mark ended", async () => {
  let resolve!: (result: { kind: "withdrawn"; subjectKey: string }) => void;
  const service: WithdrawalService = { source: "provider", async verify() { return { kind: "verified", confirmation: {} }; }, withdraw: () => new Promise(done => { resolve = done; }), release() {} };
  const store = createWithdrawalStore(service, "a", "member"); await final(store); const run = store.withdraw(); store.cancel(); resolve({ kind: "withdrawn", subjectKey: "a" }); assert.equal(await run, false); assert.equal(withdrawalSession(member, store.getState()), member);
});
test("expired final confirmation requires new verification", async () => {
  const service: WithdrawalService = { source: "provider", async verify() { return { kind: "verified", confirmation: {} }; }, async withdraw() { return { kind: "failure", reason: "expired" }; }, release() {} };
  const store = createWithdrawalStore(service, "a", "member"); await final(store); assert.equal(await store.withdraw(), false); assert.equal(store.getState().phase, "verification"); assert.equal(await store.withdraw(), false);
});
test("missing adapter and wrong-account results never fake success", async () => {
  const missing = createWithdrawalStore(null, "a", "member"); await final(missing); assert.equal(missing.getState().failure, "unavailable");
  const service: WithdrawalService = { source: "provider", async verify() { return { kind: "verified", confirmation: {} }; }, async withdraw() { return { kind: "withdrawn", subjectKey: "b" }; }, release() {} }; const store = createWithdrawalStore(service, "a", "member"); await final(store); assert.equal(await store.withdraw(), false); assert.equal(withdrawalSession(member, store.getState()), member);
});
test("success survives adapter replacement until actual upstream reauthentication", async () => {
  const service = createMockWithdrawalService("normal", 0), lifecycle = createWithdrawalLifecycle(), store = lifecycle.getStore(service, "a", "member"); await final(store); await store.withdraw(); store.finish(); assert.equal(lifecycle.getStore(createMockWithdrawalService(), "a", "member").getState().ended, true);
  lifecycle.getStore(service, "a", "guest"); assert.equal(lifecycle.getStore(service, "a", "member").getState().ended, false);
});
test("failed processing keeps auth returnTo, snapshots and current destination", async () => {
  const service = createMockWithdrawalService("withdrawal-error", 0), store = createWithdrawalStore(service, "a", "member"); await final(store);
  const nav = createNavigationStore({ destination: { id: "settings" } }); nav.registerSnapshot({ destination: { id: "bookmarks" }, kind: "list", ref: "account-list" }); nav.beginAuthentication({ destination: { id: "sharedPost", params: { postId: "p" } } }); const before = nav.getState();
  assert.equal(await withdrawAndComplete(store, nav), false); assert.equal(nav.getState(), before); finishWithdrawal(store, nav); assert.equal(nav.getState(), before);
});
for (const status of ["guest", "signup-incomplete", "loading", "error"] as const) test(`${status} cannot start withdrawal`, async () => { const store = createWithdrawalStore(createMockWithdrawalService(), "a", status); await final(store); assert.equal(store.getState().phase, "closed"); assert.equal(await store.withdraw(), false); });
