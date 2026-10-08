import test from "node:test";
import assert from "node:assert/strict";
import { createInterestRegionStore, type InterestRegionService } from "./model";
import { createMockInterestRegionService, interestRegionFixtures } from "./mock";
import { evaluateGuard, resolveDestination } from "../../lib/navigation";

test("search, immediate add/remove and requery use the same saved Mock list; zero is valid", async () => {
  const service = createMockInterestRegionService("success", 0);
  const store = createInterestRegionStore(service);
  await store.load(); await store.search();
  assert.equal(store.getState().candidates.length, 3);
  await store.toggle(interestRegionFixtures[1]);
  assert.equal(store.getState().saved.length, 2);
  await store.toggle(interestRegionFixtures[0]); await store.toggle(interestRegionFixtures[1]);
  assert.deepEqual(service.getSaved(), []);
  await store.load(); assert.deepEqual(store.getState().saved, []);
});
test("no maximum; region IDs are unique and interests do not change authority or activity region", async () => {
  const service = createMockInterestRegionService("empty", 0);
  const store = createInterestRegionStore(service);
  await store.load();
  for (let i = 0; i < 25; i++) await store.toggle({ reference: `preview-${i}`, label: `Region ${i}` });
  assert.equal(store.getState().saved.length, 25);
  assert.equal(new Set(store.getState().saved.map(region => region.reference)).size, 25);
  const member = { status: "member", capabilities: { status: "ready", grants: [] } } as const;
  assert.equal(evaluateGuard({ id: "interestRegions" }, member).status, "allowed");
  assert.equal(evaluateGuard({ id: "interestRegions" }, { status: "guest" }).status, "login-required");
  assert.equal(evaluateGuard({ id: "interestRegions" }, { status: "signup-incomplete" }).status, "signup-required");
  assert.equal(resolveDestination({ id: "interestRegions" }).status, "unresolved");
});
test("failed save retains saved values, retry succeeds; query and scroll survive returning", async () => {
  const service = createMockInterestRegionService("save-error", 0);
  const store = createInterestRegionStore(service);
  await store.load(); store.setQuery("월계"); store.setScroll(160);
  await store.toggle(interestRegionFixtures[1]);
  assert.equal(store.getState().mutation, "error"); assert.equal(store.getState().saved.length, 1);
  assert.equal(service.getSaved().length, 1);
  await store.retrySave(); assert.equal(store.getState().saved.length, 2);
  store.cancel(); assert.equal(store.getState().query, "월계"); assert.equal(store.getState().scroll, 160);
});
test("pending change disables double mutation; cancellation never persists to the Mock", async () => {
  const service = createMockInterestRegionService("pending", 0);
  const store = createInterestRegionStore(service); await store.load();
  const save = store.toggle(interestRegionFixtures[1]);
  assert.equal(store.getState().mutation, "pending");
  await store.toggle(interestRegionFixtures[2]);
  store.cancel(); await save;
  assert.equal(store.getState().mutation, "idle"); assert.equal(store.getState().saved.length, 1);
  assert.equal(service.getSaved().length, 1);
});
test("late save from cancelled/account-disposed store cannot replace current saved values", async () => {
  let finish: (regions: typeof interestRegionFixtures) => void = () => {};
  const service: InterestRegionService = { source: "mock", load: async () => [interestRegionFixtures[0]], search: async () => [], save: () => new Promise(resolve => { finish = resolve; }) };
  const store = createInterestRegionStore(service); await store.load();
  const pending = store.toggle(interestRegionFixtures[1]); store.cancel(); finish(interestRegionFixtures); await pending;
  assert.equal(store.getState().saved.length, 1);
  const oldSave = store.toggle(interestRegionFixtures[1]); store.dispose(); finish(interestRegionFixtures); await oldSave;
  const other = createInterestRegionStore(createMockInterestRegionService("empty", 0)); await other.load();
  assert.deepEqual(other.getState().saved, []); assert.equal(store.getState().saved.length, 1);
});
test("load and search errors have independent retries, query misses show empty", async () => {
  const load = createInterestRegionStore(createMockInterestRegionService("load-error", 0));
  await load.load(); assert.equal(load.getState().phase, "error"); await load.load(); assert.equal(load.getState().phase, "ready");
  const search = createInterestRegionStore(createMockInterestRegionService("search-error", 0));
  await search.load(); await search.search(); assert.equal(search.getState().searchPhase, "error");
  await search.search(); assert.equal(search.getState().candidates.length, 3);
  search.setQuery("없는 지역"); await search.search(); assert.deepEqual(search.getState().candidates, []);
  assert.equal(search.getState().saved.length, 1);
});
test("stale search response cannot overwrite newer query results", async () => {
  const resolvers: ((regions: typeof interestRegionFixtures) => void)[] = [];
  const service: InterestRegionService = { source: "mock", load: async () => [], save: async regions => [...regions], search: () => new Promise(resolve => resolvers.push(resolve)) };
  const store = createInterestRegionStore(service);
  store.setQuery("월계"); const old = store.search(); store.setQuery("하계"); const next = store.search();
  resolvers[1]([interestRegionFixtures[2]]); await next; resolvers[0]([interestRegionFixtures[0]]); await old;
  assert.deepEqual(store.getState().candidates, [interestRegionFixtures[2]]);
});
test("production without an injected service reports unavailable instead of claiming Mock success", async () => {
  const store = createInterestRegionStore(null); await store.load(); await store.search();
  assert.equal(store.getState().phase, "error"); assert.equal(store.getState().searchPhase, "error");
  await store.toggle(interestRegionFixtures[0]); assert.deepEqual(store.getState().saved, []);
});
test("host StrictMode cleanup/re-attach restarts aborted reads without replaying mutations", async () => {
  const store = createInterestRegionStore(createMockInterestRegionService("success", 10));
  const pending = store.load(); store.dispose(); await pending;
  store.activate(); assert.equal(store.getState().phase, "idle");
  await store.load(); assert.equal(store.getState().phase, "ready");
  const save = store.toggle(interestRegionFixtures[1]); store.dispose(); await save;
  store.activate(); assert.equal(store.getState().mutation, "idle"); assert.equal(store.getState().saved.length, 1);
});
