import test from "node:test";
import assert from "node:assert/strict";
import { createAccountInfoStore, accountEntry, type EmailChangeRequest } from "../account-info/model";
import { createEmailChangeStore } from "./model";
import { createMockEmailChangeServices, type EmailChangeScenario } from "./mock";
import type { EmailChangeService } from "./contracts";
const subject = "member-a", nextEmail = "new@example.com";
function setup(scenario: EmailChangeScenario = "normal", delay = 0, injected?: EmailChangeService | null) {
  const services = createMockEmailChangeServices(subject, scenario, delay);
  const lifecycle = new AbortController(); let changed = 0, cancelled = 0;
  const request: EmailChangeRequest = { entry: { destination: { id: "emailChange" }, origin: { id: "account" } }, signal: lifecycle.signal, onChanged: () => { changed++; }, onCancel: () => { cancelled++; }, onFailure: () => assert.fail("Recoverable failures stay in the change flow") };
  const store = createEmailChangeStore(services.account, injected === undefined ? services.change : injected, subject, request);
  return { ...services, store, lifecycle, changed: () => changed, cancelled: () => cancelled };
}
const lookup = (service: ReturnType<typeof createMockEmailChangeServices>["account"]) => service.load(new AbortController().signal);
async function ready(h: ReturnType<typeof setup>) { await h.store.load(); h.store.setEmail(nextEmail); }
test("same member change happens only after confirmation and reuses account lookup", async () => {
  const h = setup(); await ready(h); await h.store.confirm(); assert.equal(h.changed(), 0);
  await h.store.begin(); assert.equal(h.store.getState().phase, "confirmation"); assert.equal((await lookup(h.account)).registeredEmail, "neighbor@example.com");
  await h.store.confirm(); assert.equal(h.changed(), 1); assert.equal(h.store.getState().phase, "success"); assert.equal((await lookup(h.account)).registeredEmail, nextEmail);
  await h.store.confirm(); await h.store.begin(); assert.equal(h.changed(), 1);
});
test("empty and invalid input never start verification; whitespace is normalized", async () => {
  const h = setup(); await h.store.load();
  for (const email of ["", "x", "x@", "a @example.com"]) { h.store.setEmail(email); await h.store.begin(); assert.equal(h.store.getState().failure, "invalid-email"); }
  h.store.setEmail("neighbor@example.com"); await h.store.begin(); assert.equal(h.store.getState().failure, "same-email");
  h.store.setEmail("  new@example.com  "); await h.store.begin(); await h.store.confirm(); assert.equal((await lookup(h.account)).registeredEmail, nextEmail);
});
test("unset current email is explicit and can be changed", async () => { const h = setup("empty"); await ready(h); assert.equal(h.store.getState().currentEmail, null); await h.store.begin(); await h.store.confirm(); assert.equal(h.changed(), 1); });
test("lookup failure can retry without inventing an email", async () => { const h = setup("load-error"); await h.store.load(); assert.equal(h.store.getState().phase, "load-error"); assert.equal(h.store.getState().currentEmail, null); await ready(h); assert.equal(h.store.getState().phase, "email"); });
for (const scenario of ["send-failed", "duplicate", "mismatch", "expired", "cancelled", "failed"] as const) test(`${scenario} retains the old email and permits explicit retry`, async () => {
  const h = setup(scenario); await ready(h); await h.store.begin();
  if (!["send-failed", "duplicate"].includes(scenario)) await h.store.confirm();
  assert.equal(h.store.getState().failure, scenario); assert.equal(h.changed(), 0); assert.equal((await lookup(h.account)).registeredEmail, "neighbor@example.com");
  if (["expired", "cancelled"].includes(scenario)) { await h.store.confirm(); assert.equal(h.changed(), 0); }
  if (["send-failed", "duplicate", "expired", "cancelled"].includes(scenario)) await h.store.begin();
  await h.store.confirm(); assert.equal(h.changed(), 1); assert.equal((await lookup(h.account)).registeredEmail, nextEmail);
});
test("another member email remains duplicate, editable, never an account recovery", async () => {
  const h = setup(); await ready(h); h.store.setEmail("other@example.com"); await h.store.begin(); assert.equal(h.store.getState().failure, "duplicate");
  await h.store.begin(); assert.equal(h.store.getState().failure, "duplicate"); assert.equal(h.store.getState().phase, "email");
  h.store.setEmail(nextEmail); await h.store.begin(); await h.store.confirm(); assert.equal(h.changed(), 1);
});
test("missing adapter is explicit and never changes account", async () => { const h = setup("normal", 0, null); await ready(h); await h.store.begin(); assert.equal(h.store.getState().failure, "unavailable"); await h.store.confirm(); assert.equal(h.changed(), 0); });
test("back preserves email, invalidates proof and prevents save until new confirmation", async () => {
  const h = setup(); await ready(h); await h.store.begin(); h.store.edit(); assert.equal(h.store.getState().email, nextEmail); await h.store.confirm(); assert.equal(h.changed(), 0);
  await h.store.begin(); await h.store.confirm(); assert.equal(h.changed(), 1);
});
test("cancel during pending confirmation rejects late updates and keeps old email", async () => {
  const h = setup("normal", 5); await ready(h); await h.store.begin(); const confirm = h.store.confirm(); assert.equal(h.store.getState().phase, "verifying"); h.store.cancel(); await confirm;
  assert.equal(h.cancelled(), 1); assert.equal(h.changed(), 0); assert.equal((await lookup(h.account)).registeredEmail, "neighbor@example.com");
});
test("account/session scope abort during change rejects late response", async () => {
  const h = setup("normal", 5); await ready(h); await h.store.begin();
  const stop = h.store.subscribe(() => { if (h.store.getState().phase === "changing") h.lifecycle.abort(); });
  await h.store.confirm(); stop(); assert.equal(h.changed(), 0); assert.equal((await lookup(h.account)).registeredEmail, "neighbor@example.com");
});
test("duplicate clicks cannot submit twice during pending verification", async () => { const h = setup("normal", 5); await ready(h); const start = h.store.begin(); await h.store.begin(); await start; await Promise.all([h.store.confirm(), h.store.confirm()]); assert.equal(h.changed(), 1); });
test("adapter requires confirmation, account scope and unconsumed proof", async () => {
  const { change, account } = createMockEmailChangeServices(subject, "normal", 0), signal = new AbortController().signal;
  assert.equal((await change.begin("member-b", nextEmail, signal)).kind, "failure");
  const started = await change.begin(subject, nextEmail, signal); assert.equal(started.kind, "pending"); if (started.kind !== "pending") return;
  assert.equal((await change.change(started.confirmation, signal)).kind, "failure");
  await change.confirm(started.confirmation, signal); const result = await change.change(started.confirmation, signal); assert.equal(result.kind, "changed"); if (result.kind === "changed") assert.equal(result.subjectKey, subject);
  assert.equal((await change.change(started.confirmation, signal)).kind, "failure"); assert.equal((await lookup(account)).registeredEmail, nextEmail);
});
test("wrong-account result is never reported as success", async () => {
  const mock = createMockEmailChangeServices(subject, "normal", 0);
  const service: EmailChangeService = { ...mock.change, async change() { return { kind: "changed", subjectKey: "member-b", registeredEmail: nextEmail }; } };
  const h = setup("normal", 0, service); await ready(h); await h.store.begin(); await h.store.confirm(); assert.equal(h.changed(), 0); assert.equal(h.store.getState().failure, "failed");
});
test("late begin from adapter ignoring abort is released after exit", async () => {
  const mock = createMockEmailChangeServices(subject, "normal", 0); let released = 0;
  const service: EmailChangeService = { ...mock.change, async begin() { await new Promise(resolve => setTimeout(resolve, 5)); return { kind: "pending", confirmation: {} }; }, release() { released++; } };
  const h = setup("normal", 0, service); await ready(h); const start = h.store.begin(); h.store.cancel(); await start; assert.equal(released, 1); assert.equal(h.changed(), 0);
});
for (const origin of ["me", "settings"] as const) test(`L02 success requery, cancel and scroll preservation from ${origin}`, async () => {
  const services = createMockEmailChangeServices(subject, "normal", 0); const parent = createAccountInfoStore(services.account);
  const entry = accountEntry({ destination: { id: "account" }, origin: { id: origin } }); assert.equal(entry.origin?.id, origin);
  await parent.load(); parent.setScroll(90); parent.startChange(true); let request = parent.getChangeRequest()!;
  let store = createEmailChangeStore(services.account, services.change, subject, request); await store.load(); store.setEmail(nextEmail); store.cancel(); assert.equal(parent.getState().account?.registeredEmail, "neighbor@example.com");
  parent.startChange(true); request = parent.getChangeRequest()!; store = createEmailChangeStore(services.account, services.change, subject, request);
  await store.load(); store.setEmail(nextEmail); await store.begin(); await store.confirm(); await new Promise(resolve => setTimeout(resolve, 5));
  assert.equal(parent.getState().account?.registeredEmail, nextEmail); assert.equal(parent.getState().changed, true); assert.equal(parent.getState().changePhase, "idle"); assert.equal(parent.getState().scroll, 90);
  store.dispose(); parent.dispose();
});
