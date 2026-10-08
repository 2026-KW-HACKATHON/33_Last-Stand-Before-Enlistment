import test from "node:test";
import assert from "node:assert/strict";
import { createMyVotesStore, selectVotes, voteSummary, type MyVoteRecord, type MyVotesService } from "./model";
import { createMyVotesMock, mockVoteSubject } from "./mock";
import { createNavigationStore } from "../../lib/navigation/state";

test("confirmed personal vote route connects the same post and returns to its list, then me", () => {
  const navigation = createNavigationStore({ destination: { id: "me" } });
  assert.deepEqual(navigation.navigate({ destination: { id: "myVotes" }, origin: { id: "me" } }), { status: "ready", destination: { id: "myVotes" }, href: "/me/votes" });
  const detail = navigation.navigate({ destination: { id: "post", params: { postId: "vote" } }, origin: { id: "myVotes" } });
  assert.equal(detail.status, "ready"); if (detail.status === "ready") assert.equal(detail.href, "/posts/vote");
  const list = navigation.back(); assert.equal(list.status, "ready"); if (list.status === "ready") assert.equal(list.href, "/me/votes");
  const me = navigation.back(); assert.equal(me.status, "ready"); if (me.status === "ready") assert.equal(me.href, "/me");
});

test("ALL/OPEN/CLOSED use original status and retain unavailable history without inventing status", async () => {
  const rows = await createMyVotesMock().records(mockVoteSubject);
  assert.equal(selectVotes(rows, mockVoteSubject, "ALL").length, 3);
  assert.equal(selectVotes(rows, mockVoteSubject, "OPEN").length, 1);
  assert.equal(selectVotes(rows, mockVoteSubject, "CLOSED").length, 1);
  assert.deepEqual(selectVotes(rows, "other", "ALL"), []);
});
test("actual minority choice is distinct from leader, absent/unknown choice is never inferred", () => {
  const options = [{ id: "a", label: "최다 항목", count: 65 }, { id: "b", label: "선택 항목", count: 35 }];
  assert.equal(voteSummary({ options, submittedOptionId: "b" }, false), "내 선택: 선택 항목 35% | 최다: 최다 항목 65%");
  assert.ok(voteSummary({ options }, true).startsWith("본인 선택 조회 대기 | 최종 결과:"));
  assert.ok(voteSummary({ options, submittedOptionId: "removed" }, false).startsWith("본인 선택 조회 대기"));
  assert.equal(voteSummary(null, false), "본인 선택과 결과 조회 대기");
  assert.ok(voteSummary({ options: [] }, true).includes("집계 없음"));
});
test("a tie keeps all leaders and still uses submitted option", () => {
  const text = voteSummary({ options: [{ id: "a", label: "A", count: 1 }, { id: "b", label: "B", count: 1 }], submittedOptionId: "b" }, true);
  assert.equal(text, "내 선택: B 50% | 최종 결과: A 50% / B 50%");
});
test("latest unavailable row wins deduplication and contains no deleted choice/result/body", async () => {
  const mock = createMyVotesMock();
  const before = await mock.records(mockVoteSubject);
  mock.hide("vote");
  const after = await mock.records(mockVoteSubject);
  const rows = selectVotes([...before, ...after], mockVoteSubject, "ALL");
  assert.equal(rows.length, 3);
  const hidden = rows.find(row => row.postId === "vote")!;
  assert.deepEqual(Object.keys(hidden).sort(), ["availability", "participatedAt", "postId", "status", "subjectKey"]);
  assert.equal(selectVotes(after, mockVoteSubject, "OPEN")[0].availability, "unavailable");
});
test("existing FE2 vote service is the shared source: change in detail is latest choice/count on list return", async () => {
  const mock = createMyVotesMock();
  const store = createMyVotesStore(mock.service, mockVoteSubject);
  store.open(); store.filter("OPEN"); store.scroll(145); await store.load();
  const vote = mock.voteService("vote")!;
  const old = await vote.get("vote");
  const total = old.options.reduce((n, option) => n + option.count, 0);
  await vote.submit("vote", "sunday-afternoon");
  await store.load();
  const row = selectVotes(store.getState().records, mockVoteSubject, "OPEN")[0];
  assert.equal(row.availability, "available");
  if (row.availability === "available") {
    assert.equal(row.snapshot?.submittedOptionId, "sunday-afternoon");
    assert.equal(row.snapshot?.options.reduce((n, option) => n + option.count, 0), total);
  }
  assert.equal(store.getState().filter, "OPEN"); assert.equal(store.getState().scroll, 145);
  store.close(); store.open(); assert.equal(store.getState().scroll, 145);
  store.filter("CLOSED"); assert.equal(store.getState().scroll, 0);
});
test("retry keeps tab/scroll and empty/unknown/other account are distinct", async () => {
  const store = createMyVotesStore(createMyVotesMock("error").service, mockVoteSubject);
  store.open(); store.filter("CLOSED"); store.scroll(90); await store.load();
  assert.equal(store.getState().phase, "error"); await store.load();
  assert.equal(store.getState().phase, "ready"); assert.equal(store.getState().filter, "CLOSED"); assert.equal(store.getState().scroll, 90);
  assert.deepEqual(await createMyVotesMock("empty").records(mockVoteSubject), []);
  assert.deepEqual(await createMyVotesMock().records("other-member"), []);
  assert.ok((await createMyVotesMock("unknown").records(mockVoteSubject)).some(row => row.availability === "available" && row.snapshot === null));
});
test("missing provider never registers production mock or loads another subject", async () => {
  const store = createMyVotesStore(null, mockVoteSubject); await store.load();
  assert.equal(store.getState().phase, "unavailable"); assert.deepEqual(store.getState().records, []);
  let calls = 0;
  const withoutSubject = createMyVotesStore({ source: "api", async list() { calls++; return []; } }, null);
  await withoutSubject.load(); assert.equal(calls, 0);
});
test("close, disposal and newer load discard late responses", async () => {
  const resolves: ((rows: readonly MyVoteRecord[]) => void)[] = [];
  const service: MyVotesService = { source: "api", list: () => new Promise(resolve => resolves.push(resolve)) };
  const store = createMyVotesStore(service, mockVoteSubject);
  const rows = await createMyVotesMock().records(mockVoteSubject);
  const first = store.load(); store.close(); resolves[0](rows); await first;
  assert.equal(store.getState().phase, "idle");
  const older = store.load(); const latest = store.load(); resolves[2]([]); await latest; resolves[1](rows); await older;
  assert.deepEqual(store.getState().records, []);
  const final = store.load(); store.dispose(); resolves[3](rows); await final;
  assert.deepEqual(store.getState().records, []);
});
