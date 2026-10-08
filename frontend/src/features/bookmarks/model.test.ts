import test from "node:test";
import assert from "node:assert/strict";
import { createBookmarksStore, selectBookmarks, bookmarkTopics, bookmarkTypes, type BookmarkService } from "./model";
import { bookmarkMockSubject as subject, createBookmarksMock } from "./mock";
import { postFixtures } from "../post/mock";
import { createNavigationStore } from "../../lib/navigation/state";
import { settingsEntry } from "../settings/model";

test("type and topic conditions intersect and include all confirmed filter choices", async () => {
  const mock = createBookmarksMock(); const store = createBookmarksStore(mock.service, subject);
  await store.load(); assert.equal(selectBookmarks(store.getState()).length, 4);
  store.filter("LOCAL_AGENDA", "주거"); assert.equal(selectBookmarks(store.getState()).length, 0);
  store.filter("LOCAL_ACTIVITY", "환경"); assert.deepEqual(selectBookmarks(store.getState()).map(p => p.id), ["activity"]);
  store.filter("VOTE", "생활정보"); assert.deepEqual(selectBookmarks(store.getState()).map(p => p.id), ["vote"]);
  assert.equal(Object.keys(bookmarkTypes).length, 4); assert.equal(bookmarkTopics.length, 8);
});
test("list and detail share a single original/relationship; save/remove/re-save is idempotent", async () => {
  const mock = createBookmarksMock("empty"); const store = createBookmarksStore(mock.service, subject);
  await store.load(); assert.equal(store.getState().records.length, 0);
  await store.read("activity"); await store.toggle("activity");
  assert.equal(store.getState().details.activity.value?.isBookmarked, true);
  assert.equal(store.getState().details.activity.saved, true);
  assert.deepEqual(store.getState().records.map(p => p.id), ["activity"]);
  assert.equal(store.getState().records[0], mock.post("activity"));
  const signal = new AbortController().signal;
  await mock.service.set(subject, "activity", true, signal); await store.load(); assert.equal(store.getState().records.length, 1);
  store.dismissSaved("activity"); assert.equal(store.getState().details.activity.saved, false);
  await store.toggle("activity"); assert.equal(store.getState().records.length, 0); assert.equal(store.getState().details.activity.saved, false);
  await store.toggle("activity"); assert.equal(store.getState().records.length, 1);
});
test("a deleted original removes relations in every account, never retaining a deleted card", async () => {
  const mock = createBookmarksMock(); const store = createBookmarksStore(mock.service, subject);
  const signal = new AbortController().signal;
  await mock.service.set("other", "vote", true, signal); await store.load(); await store.read("vote");
  mock.removeOriginal("vote"); await store.toggle("vote");
  assert.equal(store.getState().details.vote.phase, "unavailable");
  assert.ok(!store.getState().records.some(post => post.id === "vote"));
  assert.equal((await mock.service.list("other", signal)).length, 0);
  await store.read("missing"); await store.toggle("missing"); assert.equal(store.getState().details.missing.value?.available, false);
});
test("failed writes preserve saved state and are retryable without a false success popup", async () => {
  const mock = createBookmarksMock(); const store = createBookmarksStore(mock.service, subject);
  await store.load(); await store.read("activity"); mock.scenario("mutation-error"); await store.toggle("activity");
  assert.equal(store.getState().details.activity.value?.isBookmarked, true);
  assert.ok(store.getState().records.some(post => post.id === "activity"));
  assert.equal(store.getState().details.activity.saved, false);
  assert.match(store.getState().details.activity.feedback, /실패/);
  mock.scenario("normal"); await store.toggle("activity"); assert.ok(!store.getState().records.some(post => post.id === "activity"));
});
test("read failure/retry and detail return preserve both filters, scroll and source origin", async () => {
  const mock = createBookmarksMock("error"); const store = createBookmarksStore(mock.service, subject);
  store.open("settings"); store.filter("LOCAL_AGENDA", "환경"); store.scroll(205);
  await store.load(); assert.equal(store.getState().phase, "error"); assert.equal(store.getState().records.length, 0);
  await store.read("agenda-photo"); assert.equal(store.getState().details["agenda-photo"].phase, "error");
  mock.scenario("normal"); await store.load(); await store.read("agenda-photo");
  store.close(); store.open(); await store.load();
  assert.equal(store.getState().origin, "settings"); assert.equal(store.getState().scroll, 205);
  assert.equal(store.getState().type, "LOCAL_AGENDA"); assert.equal(store.getState().topic, "환경");
  assert.equal(store.getState().details["agenda-photo"].phase, "ready");
});
test("origin and existing original-detail navigation preserve I01/L01, not an invented settings URL", () => {
  for (const origin of ["me", "settings"] as const) {
    const nav = createNavigationStore({ destination: { id: "bookmarks" }, origin: { id: origin } });
    nav.navigate({ destination: { id: "post", params: { postId: "vote" } }, origin: { id: "bookmarks" } });
    nav.back(); assert.equal(nav.getState().current?.destination.id, "bookmarks");
    const store = createBookmarksStore(null, subject); store.open(origin); store.close(); store.open();
    assert.equal(store.getState().origin, origin);
    const result = nav.navigate({ destination: { id: store.getState().origin }, origin: { id: origin } }, true);
    assert.equal(result.status, origin === "settings" ? "unresolved" : "ready");
    if (origin === "settings") assert.equal(settingsEntry({ destination: { id: "settings" }, origin: { id: "me" } }, { destination: { id: "settings" }, origin: { id: "settings" } }).origin?.id, "me");
  }
});
test("pending guards prevent duplicate writes and disposal suppresses late account results", async () => {
  let resolve!: (value: Awaited<ReturnType<BookmarkService["set"]>>) => void;
  let calls = 0;
  const service: BookmarkService = { source: "mock", list: async () => [postFixtures.activity], get: async () => ({ postId: "activity", available: true, isBookmarked: true }), set: async () => { calls++; return new Promise(done => { resolve = done; }); } };
  const store = createBookmarksStore(service, subject); await store.read("activity");
  const pending = store.toggle("activity"); await store.toggle("activity"); assert.equal(calls, 1);
  assert.equal(store.getState().details.activity.pending, true);
  store.dispose(); const disposedState = store.getState();
  resolve({ postId: "activity", available: true, isBookmarked: false }); await pending;
  assert.equal(store.getState(), disposedState); await store.toggle("activity"); assert.equal(calls, 1);
  const newAccount = createBookmarksStore(createBookmarksMock().service, "other"); await newAccount.load(); assert.equal(newAccount.getState().records.length, 0);
});
test("out-of-order list requests cannot restore stale data after newer deletion results", async () => {
  let first!: (value: readonly typeof postFixtures.activity[]) => void; let calls = 0;
  const mock = createBookmarksMock(); const service = { ...mock.service, list: async () => ++calls === 1 ? new Promise<readonly typeof postFixtures.activity[]>(done => { first = done; }) : [] };
  const store = createBookmarksStore(service, subject); const old = store.load(); await store.load();
  first([postFixtures.activity]); await old; assert.equal(store.getState().phase, "ready"); assert.equal(store.getState().records.length, 0);
});
test("development effect cleanup/reattach permits fresh loads but rejects the old aborted response", async () => {
  let oldResolve!: (value: readonly typeof postFixtures.activity[]) => void; let calls = 0;
  const mock = createBookmarksMock(); const service = { ...mock.service, list: async () => ++calls === 1 ? new Promise<readonly typeof postFixtures.activity[]>(done => { oldResolve = done; }) : [postFixtures.vote] };
  const store = createBookmarksStore(service, subject); const old = store.load();
  store.dispose(); store.activate(); await store.load(); oldResolve([postFixtures.activity]); await old;
  assert.deepEqual(store.getState().records.map(post => post.id), ["vote"]);
});
test("guest/missing adapter performs no private reads or writes; accounts do not share bookmarks", async () => {
  let calls = 0; const mock = createBookmarksMock();
  const service: BookmarkService = { ...mock.service, list: async () => { calls++; return []; }, get: async () => { calls++; throw new Error(); }, set: async () => { calls++; throw new Error(); } };
  const guest = createBookmarksStore(service, null); await guest.load(); await guest.read("activity"); await guest.toggle("activity"); assert.equal(calls, 0);
  const unavailable = createBookmarksStore(null, subject); await unavailable.load(); assert.equal(unavailable.getState().phase, "unavailable");
  const signal = new AbortController().signal; assert.equal((await mock.service.get("other", "activity", signal)).isBookmarked, false);
  await mock.service.set("other", "vote", true, signal); assert.deepEqual((await mock.service.list("other", signal)).map(post => post.id), ["vote"]);
});
