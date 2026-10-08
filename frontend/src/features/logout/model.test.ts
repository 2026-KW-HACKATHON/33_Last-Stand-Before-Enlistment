import test from "node:test";
import assert from "node:assert/strict";
import { createNavigationStore, evaluateGuard, type SessionState } from "../../lib/navigation";
import { createLogoutStore, createLogoutLifecycle, logoutSession, logoutAndReturn, type CurrentDeviceLogoutService } from "./model";
import { createMockLogoutService } from "./mock";
const member: SessionState = { status: "member", capabilities: { status: "ready", grants: [{ capability: { kind: "neighbor-region", regionId: "demo-region" }, allowed: true }, { capability: { kind: "institution" }, allowed: true }] } };
function navigation() { const nav = createNavigationStore({ destination: { id: "me" } }); nav.registerSnapshot({ destination: { id: "bookmarks" }, kind: "list", ref: "account-a:list" }); nav.beginAuthentication({ destination: { id: "post", params: { postId: "42" } } }); return nav; }
test("confirmed current-device logout clears auth returnTo/history/snapshots and goes to A01", async () => {
  const service = createMockLogoutService("normal", 0), otherDevice = createMockLogoutService("normal", 0);
  const store = createLogoutStore(service, "member-a", "member"), nav = navigation();
  assert.equal(await logoutAndReturn(store, nav), true); assert.equal(store.getState().phase, "success"); assert.equal(service.isCurrentDeviceActive(), false); assert.equal(otherDevice.isCurrentDeviceActive(), true);
  const state = nav.getState(); assert.equal(state.current?.destination.id, "start"); assert.deepEqual(state.history, []); assert.equal(state.returnTo, null); assert.deepEqual(state.snapshots, []);
  const guest = logoutSession(member, store.getState()); assert.equal(guest.status, "guest");
  for (const id of ["home", "settings", "me", "account", "newPost"] as const) assert.equal(evaluateGuard({ id }, guest).status, "login-required");
  assert.equal(evaluateGuard({ id: "sharedPost", params: { postId: "42" } }, guest, { sharedAccess: { status: "ready", postId: "42" } }).status, "allowed");
  store.cancel(); assert.equal(logoutSession(member, store.getState()).status, "guest"); assert.equal(await logoutAndReturn(store, nav), false);
});
test("failure preserves exact logged-in identity/Capabilities and navigation; explicit retry succeeds", async () => {
  const service = createMockLogoutService("error-retry", 0), store = createLogoutStore(service, "member-a", "member"), nav = navigation(), before = nav.getState();
  assert.equal(await logoutAndReturn(store, nav), false); assert.equal(store.getState().phase, "error"); assert.equal(service.isCurrentDeviceActive(), true); assert.equal(logoutSession(member, store.getState()), member); assert.equal(nav.getState(), before);
  assert.equal(await logoutAndReturn(store, nav), true); assert.equal(nav.getState().current?.destination.id, "start");
});
test("pending prevents duplicate requests and does not drop access before success", async () => {
  let calls = 0, finish!: (result: { kind: "logged-out"; subjectKey: string }) => void;
  const service: CurrentDeviceLogoutService = { source: "provider", endCurrentDevice() { calls++; return new Promise(resolve => { finish = resolve; }); } };
  const store = createLogoutStore(service, "member-a", "member"), first = store.run(); assert.equal(store.getState().phase, "pending"); assert.equal(logoutSession(member, store.getState()), member);
  assert.equal(await store.run(), false); assert.equal(calls, 1); finish({ kind: "logged-out", subjectKey: "member-a" }); assert.equal(await first, true);
});
test("cancel/back aborts pending Mock and retains login/returnTo; a new click can retry", async () => {
  const service = createMockLogoutService("normal", 5), store = createLogoutStore(service, "member-a", "member"), nav = navigation(), before = nav.getState();
  const first = logoutAndReturn(store, nav); store.cancel(); assert.equal(await first, false); assert.equal(store.getState().phase, "idle"); assert.equal(service.isCurrentDeviceActive(), true); assert.equal(logoutSession(member, store.getState()), member); assert.equal(nav.getState(), before);
  assert.equal(await logoutAndReturn(store, nav), true);
});
test("ignored-abort late success after cancel cannot update Session or route", async () => {
  let resolve!: (value: { kind: "logged-out"; subjectKey: string }) => void;
  const service: CurrentDeviceLogoutService = { source: "provider", endCurrentDevice() { return new Promise(done => { resolve = done; }); } };
  const store = createLogoutStore(service, "member-a", "member"), nav = navigation(), before = nav.getState(); const first = logoutAndReturn(store, nav); store.cancel(); resolve({ kind: "logged-out", subjectKey: "member-a" });
  assert.equal(await first, false); assert.equal(logoutSession(member, store.getState()), member); assert.equal(nav.getState(), before);
});
test("account switch disposes old request, rejecting late same-account success", async () => {
  let resolve!: (value: { kind: "logged-out"; subjectKey: string }) => void;
  const service: CurrentDeviceLogoutService = { source: "provider", endCurrentDevice() { return new Promise(done => { resolve = done; }); } };
  const lifecycle = createLogoutLifecycle(), old = lifecycle.getStore(service, "member-a", "member"), nav = navigation(), before = nav.getState(); const pending = logoutAndReturn(old, nav);
  const next = lifecycle.getStore(service, "member-b", "member"); resolve({ kind: "logged-out", subjectKey: "member-a" }); assert.equal(await pending, false); assert.equal(next.getState().phase, "idle"); assert.equal(nav.getState(), before);
});
test("ended scope remains guest after adapter replacement; upstream re-login creates fresh scope", async () => {
  const lifecycle = createLogoutLifecycle(), service = createMockLogoutService("normal", 0); const store = lifecycle.getStore(service, "member-a", "member"); await store.run();
  const replacement = lifecycle.getStore(createMockLogoutService(), "member-a", "member"); assert.equal(logoutSession(member, replacement.getState()).status, "guest");
  lifecycle.getStore(service, "member-a", "guest"); const relogin = lifecycle.getStore(service, "member-a", "member"); assert.equal(logoutSession(member, relogin.getState()), member);
});
test("service rejection and wrong identity never end the client session or navigate", async () => {
  for (const service of [{ source: "provider", async endCurrentDevice() { throw new Error("Network failure"); } }, { source: "provider", async endCurrentDevice() { return { kind: "logged-out", subjectKey: "member-b" }; } }] satisfies CurrentDeviceLogoutService[]) {
    const store = createLogoutStore(service, "member-a", "member"), nav = navigation(), before = nav.getState(); assert.equal(await logoutAndReturn(store, nav), false); assert.equal(store.getState().phase, "error"); assert.equal(logoutSession(member, store.getState()), member); assert.equal(nav.getState(), before);
  }
});
test("missing adapter is unavailable, never fake success", async () => { const store = createLogoutStore(null, "member-a", "member"); assert.equal(await store.run(), false); assert.equal(store.getState().phase, "unavailable"); assert.equal(logoutSession(member, store.getState()), member); });
for (const status of ["guest", "loading", "error", "signup-incomplete"] as const) test(`${status} cannot call the logout adapter`, async () => {
  let calls = 0; const service: CurrentDeviceLogoutService = { source: "mock", async endCurrentDevice(subjectKey) { calls++; return { kind: "logged-out", subjectKey }; } };
  const store = createLogoutStore(service, "member-a", status); assert.equal(await store.run(), false); assert.equal(calls, 0);
});
test("missing current member identity never calls the adapter", async () => { const store = createLogoutStore(createMockLogoutService(), null, "member"); assert.equal(await store.run(), false); });
