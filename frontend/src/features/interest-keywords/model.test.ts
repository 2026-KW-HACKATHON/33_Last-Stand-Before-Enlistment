import test from "node:test";
import assert from "node:assert/strict";
import { createKeywordStore, interestKeywords, validateKeywords, type InterestKeyword, type InterestKeywordService } from "./model";
import { createMockKeywordService } from "./mock";
test("existing seven topics, maximum four; selected chips can always be removed", async () => {
  assert.deepEqual(interestKeywords, ["교통", "주거", "안전", "복지", "생활정보", "환경", "기타"]);
  const store = createKeywordStore(createMockKeywordService("four", 0)); await store.load();
  store.toggle("환경"); assert.equal(store.getState().draft.length, 4);
  store.toggle("교통"); store.toggle("환경"); assert.deepEqual(store.getState().draft, ["주거", "안전", "복지", "환경"]);
  assert.throws(() => validateKeywords(["교통", "교통"]));
  assert.throws(() => validateKeywords(["unknown" as InterestKeyword]));
  assert.throws(() => validateKeywords(interestKeywords.slice(0, 5)));
});
test("draft changes never affect recommendation's saved source before successful save; empty is valid", async () => {
  const service = createMockKeywordService("success", 0); const store = createKeywordStore(service); await store.load();
  store.toggle("교통"); store.toggle("주거"); assert.deepEqual(service.getSaved(), ["교통", "주거"]);
  assert.equal(await store.save(), true); assert.deepEqual(service.getSaved(), []);
  await store.load(); assert.deepEqual(store.getState().draft, []);
});
test("failure restores saved selection; Retry resends failed selection and only success changes saved values", async () => {
  const service = createMockKeywordService("save-error", 0); const store = createKeywordStore(service); await store.load();
  store.toggle("환경"); assert.equal(await store.save(), false);
  assert.equal(store.getState().phase, "save-error"); assert.deepEqual(store.getState().draft, ["교통", "주거"]);
  assert.deepEqual(service.getSaved(), ["교통", "주거"]);
  assert.equal(await store.retry(), true); assert.deepEqual(service.getSaved(), ["교통", "주거", "환경"]);
});
test("new selection after failure replaces retry intent; cancel restores saved selection and keeps scroll", async () => {
  const service = createMockKeywordService("save-error", 0); const store = createKeywordStore(service); await store.load();
  store.toggle("환경"); await store.save(); store.toggle("안전"); assert.equal(await store.retry(), false);
  await store.save(); assert.deepEqual(service.getSaved(), ["교통", "주거", "안전"]);
  store.setScroll(170); store.toggle("복지"); store.cancel(); assert.deepEqual(store.getState().draft, service.getSaved()); assert.equal(store.getState().scroll, 170);
});
test("Pending blocks edits and double save; cancel aborts persistence", async () => {
  const service = createMockKeywordService("pending", 0); const store = createKeywordStore(service); await store.load(); store.toggle("안전");
  const save = store.save(); store.toggle("환경"); assert.equal(await store.save(), false);
  assert.deepEqual(store.getState().draft, ["교통", "주거", "안전"]);
  store.cancel(); assert.equal(await save, false); assert.deepEqual(service.getSaved(), ["교통", "주거"]);
});
test("cancel and account disposal ignore a service that resolves late", async () => {
  let finish: (value: InterestKeyword[]) => void = () => {};
  const service: InterestKeywordService = { source: "mock", load: async () => ["교통"], save: () => new Promise(resolve => { finish = resolve; }) };
  const store = createKeywordStore(service); await store.load(); store.toggle("안전");
  const save = store.save(); store.cancel(); finish(["교통", "안전"]); assert.equal(await save, false); assert.deepEqual(store.getState().saved, ["교통"]);
  const oldSave = store.save(); store.dispose(); finish(["복지"]); assert.equal(await oldSave, false);
  const other = createKeywordStore(createMockKeywordService("empty", 0)); await other.load(); assert.deepEqual(other.getState().saved, []);
});
test("load error Retry and absent production adapter never fabricate successful storage", async () => {
  const store = createKeywordStore(createMockKeywordService("load-error", 0)); await store.load(); assert.equal(store.getState().phase, "load-error"); await store.load(); assert.equal(store.getState().phase, "ready");
  const unavailable = createKeywordStore(null); await unavailable.load(); assert.equal(unavailable.getState().phase, "load-error"); assert.equal(await unavailable.save(), false);
});
test("StrictMode re-attach restarts an aborted load without replaying a mutation", async () => {
  const store = createKeywordStore(createMockKeywordService("success", 5)); const load = store.load(); store.dispose(); await load; store.activate(); await store.load(); assert.equal(store.getState().phase, "ready");
  store.toggle("안전"); const save = store.save(); store.dispose(); await save; store.activate(); assert.equal(store.getState().phase, "ready"); assert.deepEqual(store.getState().draft, ["교통", "주거"]);
});
