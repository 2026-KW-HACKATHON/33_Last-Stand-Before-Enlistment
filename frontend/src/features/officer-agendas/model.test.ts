import test from "node:test";
import assert from "node:assert/strict";
import { createMockAdoptionService, mockOfficerAuthority } from "./mock";
import { canChange, canRead, createOfficerStore, ownRelation, publicAdoptions, publicPost, visibleAgendas, type AdoptionService, type AgendaReview } from "./model";

async function ready() {
  const authority = mockOfficerAuthority(); const mock = createMockAdoptionService(() => authority, "normal", 0);
  const store = createOfficerStore(mock.service, () => authority); await store.load();
  return { authority, mock, store };
}
test("all public agendas are reaction-sorted, including outside the responsible region", async () => {
  const { authority, store } = await ready(); const rows = visibleAgendas(store.getState(), authority);
  assert.deepEqual(rows.map(row => row.post.reactionCount), [32, 21]);
  assert.equal(canChange(rows[0], authority), true); assert.equal(canChange(rows[1], authority), false);
  store.filter("all", rows[1].regionId); assert.equal(visibleAgendas(store.getState(), authority).length, 1);
  store.filter("adopted", ""); assert.equal(visibleAgendas(store.getState(), authority).length, 0);
});
test("adopt, duplicate retry and cancel preserve other institution relations and original reactions", async () => {
  const { authority, mock, store } = await ready(); const original = structuredClone(store.getState().items[0]);
  await store.read(original.post.id); await store.change(true); await store.change(true);
  assert.equal(store.getState().detail!.relations.length, 2); assert.ok(ownRelation(store.getState().detail!, authority));
  assert.equal(store.getState().record, true);
  store.filter("adopted", ""); assert.equal(visibleAgendas(store.getState(), authority).length, 1);
  assert.deepEqual(Object.keys(publicAdoptions(store.getState().detail!)[0]).sort(), ["adoptedAtLabel", "institutionName"]);
  assert.equal(publicPost(store.getState().detail!).adoptions.length, 2);
  await store.change(false); assert.equal(visibleAgendas(store.getState(), authority).length, 0);
  assert.deepEqual(store.getState().detail!.relations, original.relations);
  assert.equal(store.getState().detail!.post.reactionCount, original.post.reactionCount);
  assert.equal(mock.canceled().length, 1);
  assert.equal((await mock.service.get(original.post.id, new AbortController().signal))!.relations.length, 1);
});
test("other region permits original opinions/replies, never an adoption or cancellation", async () => {
  const { authority, mock, store } = await ready(); const outside = store.getState().items[1];
  await store.read(outside.post.id); assert.ok(store.getState().detail!.opinions[0].replies.length);
  await store.change(true); assert.equal(store.getState().detail!.relations.length, 1);
  await assert.rejects(mock.service.setAdopted(outside.post.id, true, new AbortController().signal));
  assert.equal(canChange(outside, authority), false);
});
test("expired, absent, unknown, guest and mismatched subject never read or mutate", async () => {
  for (const scenario of ["expired", "none", "unknown"] as const) {
    const authority = mockOfficerAuthority(scenario); assert.equal(canRead(authority), false);
    let calls = 0; const mock = createMockAdoptionService(() => authority, "normal", 0);
    const store = createOfficerStore({ ...mock.service, async list(signal) { calls++; return mock.service.list(signal); } }, () => authority);
    await store.load(); assert.equal(calls, 0);
  }
  const authority = mockOfficerAuthority(); assert.equal(canRead({ ...authority, session: { status: "guest" } }), false);
  assert.equal(canRead({ ...authority, subjectId: "other-account" }), false);
  if (authority.session.status === "member" && authority.session.capabilities.status === "ready") {
    assert.ok(authority.session.capabilities.grants.some(grant => grant.capability.kind === "neighbor-region" && !grant.allowed));
    assert.equal(canRead(authority), true);
  }
});
test("query and save failure keep filters, scroll, original relationships; retry succeeds", async () => {
  const { authority, mock, store } = await ready(); const item = store.getState().items[0];
  store.filter("all", item.regionId); store.saveScroll(227); mock.scenario("error"); await store.load();
  assert.equal(store.getState().phase, "error"); assert.equal(store.getState().regionId, item.regionId); assert.equal(store.getState().scroll, 227);
  mock.scenario("normal"); await store.load(); await store.read(item.post.id);
  mock.scenario("save-error"); await store.change(true); assert.equal(store.getState().pending, false); assert.equal(store.getState().detail!.relations.length, 1);
  mock.scenario("normal"); await store.change(true); assert.ok(ownRelation(store.getState().detail!, authority));
  mock.scenario("save-error"); await store.change(false); assert.ok(ownRelation(store.getState().detail!, authority));
  mock.scenario("normal"); await store.change(false); assert.equal(ownRelation(store.getState().detail!, authority), undefined);
  assert.equal(store.getState().scroll, 227); assert.equal(store.getState().regionId, item.regionId);
});
test("empty, deleted, missing adapter and detail failure are separate states", async () => {
  const { mock, store } = await ready(); mock.scenario("empty"); await store.load(); assert.equal(store.getState().phase, "ready"); assert.equal(store.getState().items.length, 0);
  mock.scenario("deleted"); await store.read("agenda-photo"); assert.equal(store.getState().detailPhase, "unavailable");
  mock.scenario("detail-error"); await store.read("agenda-photo"); assert.equal(store.getState().detailPhase, "error");
  mock.scenario("normal"); await store.read("agenda-photo"); assert.equal(store.getState().detailPhase, "ready");
  const missing = createOfficerStore(null, mockOfficerAuthority); await missing.load(); assert.equal(missing.getState().phase, "unavailable");
});
test("double click sends one mutation and late mutation after disposal cannot publish", async () => {
  const { authority, mock } = await ready(); let resolve!: (value: AgendaReview) => void, calls = 0;
  const service: AdoptionService = { ...mock.service, setAdopted: () => { calls++; return new Promise(done => { resolve = done; }); } };
  const store = createOfficerStore(service, () => authority); await store.load(); await store.read("agenda-photo");
  const mutation = store.change(true); assert.equal(store.getState().pending, true); await store.change(true); assert.equal(calls, 1);
  const next = await mock.service.get("agenda-photo", new AbortController().signal); store.dispose(); resolve(next!); await mutation;
  assert.equal(store.getState().detail, null); assert.equal(store.getState().phase, "idle");
});
test("qualification revoked during pending operation cannot apply an old success", async () => {
  let authority = mockOfficerAuthority(); const mock = createMockAdoptionService(() => authority, "normal", 0);
  let resolve!: (value: AgendaReview) => void;
  const store = createOfficerStore({ ...mock.service, setAdopted: () => new Promise(done => { resolve = done; }) }, () => authority);
  await store.read("agenda-photo"); const previous = store.getState().detail!; const mutation = store.change(true);
  authority = mockOfficerAuthority("expired"); store.clear(); resolve(previous); await mutation;
  assert.equal(store.getState().detail, null); assert.equal(store.getState().pending, false);
});
test("effect replay aborts old queries and permits a fresh request", async () => {
  const { authority, mock } = await ready(); let resolve!: (rows: readonly AgendaReview[]) => void;
  let first = true;
  const store = createOfficerStore({ ...mock.service, list: signal => { if (!first) return mock.service.list(signal); first = false; return new Promise(done => { resolve = done; }); } }, () => authority);
  const old = store.load(); store.dispose(); store.activate(); await store.load(); const fresh = store.getState().items;
  resolve([]); await old; assert.deepEqual(store.getState().items, fresh); assert.equal(fresh.length, 2);
});
