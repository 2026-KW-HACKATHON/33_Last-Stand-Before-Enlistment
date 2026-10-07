import assert from "node:assert/strict";
import { test } from "node:test";
import { createNavigationStore } from "../../../lib/navigation/state";
import type { SignupDraft, SignupService } from "../contracts";
import { createMockSignupService, signupRegionFixtures } from "../mock";
import { createSignupStore, emptySignupDraft, requiredAgreements, validProfile } from "../state";

const draft = (): SignupDraft => ({ agreements: { terms: true, privacy: true, marketing: false }, profile: { nickname: "동네이웃", bio: "", attributes: ["학생"] }, activityRegion: signupRegionFixtures[0] });
function setup(service: SignupService | null = createMockSignupService()) {
  const store = createSignupStore(service); store.setAccess("verified-incomplete"); store.setDraft(draft()); return store;
}
test("required consents are independent and marketing stays optional", async () => {
  const service = createMockSignupService(); const store = setup(service);
  for (const missing of ["terms", "privacy"] as const) {
    store.setDraft({ ...draft(), agreements: { ...draft().agreements, [missing]: false } });
    store.next(); await store.submit(null); assert.equal(store.getState().step, "agreements");
  }
  assert.equal(service.getSavedDrafts().length, 0);
  store.setDraft(draft()); store.next(); assert.equal(store.getState().step, "profile");
  await store.submit(null); assert.equal(store.getState().phase, "success");
  assert.equal(service.getSavedDrafts()[0].agreements.marketing, false);
});
test("Provider success/incomplete access alone does not complete local signup or grant qualifications", () => {
  const store = createSignupStore(createMockSignupService()); store.setAccess("verified-incomplete");
  assert.equal(store.getState().member, null); assert.equal(store.getState().availability, null); assert.equal(requiredAgreements(store.getState().draft), false);
});
test("all unverified/loading/error/member access blocks submission and editing", async () => {
  for (const access of ["unverified", "loading", "error", "member"] as const) {
    const service = createMockSignupService(); const store = setup(service); store.setAccess(access);
    const before = store.getState().draft; store.setDraft(emptySignupDraft()); store.next(); await store.submit(null);
    assert.deepEqual(store.getState().draft, before); assert.equal(service.getSavedDrafts().length, 0);
  }
});
test("invalid nickname/bio and missing region cannot complete", async () => {
  const service = createMockSignupService(); const store = setup(service);
  for (const profile of [{ ...draft().profile, nickname: " " }, { ...draft().profile, nickname: "가".repeat(11) }, { ...draft().profile, bio: "가".repeat(51) }]) {
    store.setDraft({ ...draft(), profile }); assert.equal(validProfile(store.getState().draft), false); await store.submit(null); assert.equal(store.getState().step, "profile");
  }
  store.setDraft({ ...draft(), activityRegion: null }); await store.submit(null); assert.equal(store.getState().failure?.reason, "region");
  assert.equal(service.getSavedDrafts().length, 0);
});
test("stage back, cancellation, and reentry preserve all form fields", () => {
  const store = setup(); store.next(); store.next(); assert.equal(store.getState().step, "region");
  store.back(); assert.equal(store.getState().step, "profile"); store.back(); assert.equal(store.getState().step, "agreements"); store.cancel();
  assert.deepEqual(store.getState().draft, draft()); store.next(); store.next(); assert.equal(store.getState().step, "region");
});
test("known save failure allows manual retry and only saves once", async () => {
  const service = createMockSignupService("failure"); const store = setup(service);
  await store.submit(null); assert.equal(store.getState().failure?.reason, "failed"); assert.deepEqual(store.getState().draft, draft());
  await store.submit(null); assert.equal(service.getSavedDrafts().length, 1); assert.equal(store.getState().phase, "success");
  assert.deepEqual(store.getState().member?.capabilities, { status: "ready", grants: [] });
});
test("duplicate nickname can be corrected without losing consent/region", async () => {
  const service = createMockSignupService("nickname"); const store = setup(service);
  store.setDraft({ ...draft(), profile: { ...draft().profile, nickname: "중복" } }); await store.submit(null); assert.equal(store.getState().failure?.reason, "nickname");
  store.reviewProfile(); store.setDraft(draft()); await store.submit(null); assert.equal(store.getState().phase, "success");
});
test("unknown save result never resubmits, even after edits and stage round trip", async () => {
  let calls = 0; const mock = createMockSignupService();
  const store = setup({ ...mock, async complete() { calls++; throw new Error("private provider details"); } });
  await store.submit(null); assert.equal(store.getState().failure?.reason, "unknown");
  store.setDraft(draft()); store.next(); store.next(); store.back(); store.next(); await store.submit(null);
  assert.equal(calls, 1); assert.equal(JSON.stringify(store.getState()).includes("private provider"), false);
});
test("retrying return target does not create another local member", async () => {
  const service = createMockSignupService("target-error"); const store = setup(service);
  await store.submit(null); assert.equal(store.getState().failure?.reason, "failed"); assert.ok(store.getState().member);
  store.setDraft(emptySignupDraft()); await store.submit(null); assert.equal(store.getState().phase, "success"); assert.equal(service.getSavedDrafts().length, 1);
});
test("shared returnTo survives failure and steps, completion only navigates", async () => {
  const navigation = createNavigationStore({ destination: { id: "sharedPost", params: { postId: "original" } }, sharedContextRef: "context" });
  navigation.beginAuthentication(); navigation.completeAuthentication({ status: "signup-incomplete" }, "available");
  const target = navigation.getState().returnTo?.target ?? null; const store = setup(createMockSignupService("failure"));
  store.next(); store.next(); store.back(); await store.submit(target); assert.deepEqual(navigation.getState().returnTo?.target, target);
  await store.submit(target); const state = store.getState(); assert.ok(state.member); assert.ok(state.availability);
  const result = navigation.completeAuthentication(state.member, state.availability);
  assert.equal(result.status, "ready"); if (result.status === "ready") assert.equal(result.href, "/posts/original");
  assert.equal(navigation.getState().returnTo, null); assert.equal("action" in result, false);
});
test("normal/deleted target uses existing Navigation home fallback", async () => {
  for (const scenario of ["success", "deleted"] as const) {
    const navigation = createNavigationStore(); const store = setup(createMockSignupService(scenario)); await store.submit(null);
    const state = store.getState(); assert.ok(state.member); assert.ok(state.availability);
    const result = navigation.completeAuthentication(state.member, state.availability);
    assert.equal(result.status, "ready"); if (result.status === "ready") { assert.equal(result.href, "/home"); if (scenario === "deleted") assert.equal(result.notice, "unavailable"); }
  }
});
test("cancel returns shared origin and preserves draft, explicit clear removes account data", () => {
  const navigation = createNavigationStore({ destination: { id: "sharedPost", params: { postId: "original" } } }); navigation.beginAuthentication(undefined, "signup");
  const store = setup(); store.next(); store.cancel(); const result = navigation.cancelAuthentication();
  assert.equal(result.status, "ready"); if (result.status === "ready") assert.equal(result.href, "/shared/posts/original");
  assert.deepEqual(store.getState().draft, draft()); store.clear(); assert.deepEqual(store.getState().draft, emptySignupDraft()); assert.equal(store.getState().access, "loading");
});
test("double submission and cancellation ignore late results", async () => {
  const service = createMockSignupService(); let calls = 0; let finish: (() => void) | undefined;
  const store = setup({ ...service, async complete() { calls++; await new Promise<void>(resolve => { finish = resolve; }); return { kind: "completed", session: { status: "member", capabilities: { status: "ready", grants: [] } } }; } });
  const first = store.submit(null); await store.submit(null); assert.equal(calls, 1);
  store.cancel(); finish?.(); await first; assert.equal(store.getState().member, null); assert.equal(store.getState().failure?.reason, "unknown");
});
test("access loss aborts pending completion and cannot create a successful FE state", async () => {
  const store = setup(createMockSignupService("success", 5)); const pending = store.submit(null); store.setAccess("unverified"); await pending;
  assert.equal(store.getState().member, null); assert.equal(store.getState().access, "unverified");
});
test("missing adapter cannot report success; already linked mock does not create a member", async () => {
  const store = setup(null); await store.submit(null); assert.equal(store.getState().failure?.reason, "unavailable"); assert.equal(store.getState().member, null);
  const service = createMockSignupService("already-linked"); const linked = setup(service); await linked.submit(null);
  assert.equal(linked.getState().phase, "success"); assert.equal(service.getSavedDrafts().length, 0);
});
