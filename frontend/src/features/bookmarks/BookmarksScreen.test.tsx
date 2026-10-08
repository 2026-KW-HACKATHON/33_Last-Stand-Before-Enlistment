import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { NavigationProvider, SessionProvider, useNavigation, type NavigationEntry, type SessionState } from "../../lib/navigation";
import { PostDetail, type BookmarkBinding } from "../post/PostDetail";
import { postFixtures } from "../post/mock";
import { BookmarksScreen } from "./BookmarksScreen";
import { createBookmarksStore, type BookmarksStore } from "./model";
import { createBookmarksMock, bookmarkMockSubject as subject, type BookmarkScenario } from "./mock";
import { BookmarksProvider, useBookmarkBinding } from "./provider";

const member: SessionState = { status: "member", capabilities: { status: "ready", grants: [] } };
function render(store: BookmarksStore, session: SessionState = member) {
  return renderToStaticMarkup(<SessionProvider session={session}><NavigationProvider currentDestination={{ id: "bookmarks" }} onNavigate={() => {}}><BookmarksScreen store={store} subjectKey={subject} onBack={() => {}}/></NavigationProvider></SessionProvider>);
}
async function ready(scenario: BookmarkScenario = "normal") { const mock = createBookmarksMock(scenario); const store = createBookmarksStore(mock.service, subject); store.open(); await store.load(); return { mock, store }; }
test("I02 renders four types and eight topics in separate 3-column grids, with no card bookmark toggles", async () => {
  const { store } = await ready(); const html = render(store);
  assert.equal((html.match(/aria-pressed=/g) ?? []).length, 12);
  assert.equal((html.match(/grid grid-cols-3 gap-internal/g) ?? []).length, 2);
  for (const label of ["지역 안건", "지역 활동 정보", "투표", "생활정보", "교통", "주거", "안전", "복지", "환경", "기타"]) assert.ok(html.includes(label), label);
  assert.ok(html.includes(postFixtures.activity.title)); assert.ok(!html.includes("북마크 저장")); assert.ok(!html.includes("북마크 해제"));
});
test("each type filter and intersecting topic renders only matching originals", async () => {
  const { store } = await ready();
  store.filter("VOTE", "생활정보"); const vote = render(store);
  assert.ok(vote.includes(postFixtures.vote.title)); assert.ok(!vote.includes(postFixtures["vote-ended"].title)); assert.ok(!vote.includes(postFixtures.activity.title));
  store.filter("LOCAL_ACTIVITY", "환경"); const activity = render(store);
  assert.ok(activity.includes(postFixtures.activity.title)); assert.ok(!activity.includes(postFixtures.vote.title));
  store.filter("LOCAL_AGENDA", "전체"); assert.ok(render(store).includes(postFixtures["agenda-photo"].title));
});
test("loading/empty/error/retry/missing adapter remain distinct and failed loads do not leak old cards", async () => {
  const { store, mock } = await ready();
  const loading = store.load(); assert.ok(render(store).includes("북마크 불러오는 중")); assert.ok(!render(store).includes(postFixtures.activity.title)); await loading;
  mock.scenario("error"); await store.load(); const error = render(store); assert.ok(error.includes("다시 시도")); assert.ok(!error.includes("해당 조건의 북마크가 없습니다")); assert.ok(!error.includes(postFixtures.vote.title));
  mock.scenario("normal"); await store.load(); store.filter("VOTE", "주거"); assert.ok(render(store).includes("해당 조건의 북마크가 없습니다"));
  const missing = createBookmarksStore(null, subject); await missing.load(); assert.ok(render(missing).includes("실 API 연동 대기"));
  assert.ok(render((await ready("empty")).store).includes("해당 조건의 북마크가 없습니다"));
});
test("guest/incomplete/loading session cannot see bookmark originals; member needs no region/institution grant", async () => {
  const { store } = await ready();
  for (const session of [{ status: "guest" }, { status: "signup-incomplete" }, { status: "loading" }] as SessionState[]) {
    const html = render(store, session); assert.ok(!html.includes(postFixtures.activity.title)); assert.ok(!html.includes("images.unsplash.com"));
    if (session.status !== "loading") assert.ok(html.includes("로그인 / 가입"));
  }
  assert.ok(render(store, member).includes(postFixtures.activity.title));
});
test("deleted originals are removed, without a retained deleted-card or content stub", async () => {
  const { store, mock } = await ready(); mock.removeOriginal("agenda-photo"); await store.load();
  const html = render(store); assert.ok(!html.includes(postFixtures["agenda-photo"].title)); assert.ok(!html.includes("접근 불가 안내"));
});
test("existing FE2 detail presents only one bookmark button, with pending/failure feedback and no duplicate control", async () => {
  const { store, mock } = await ready(); await store.read("activity"); mock.scenario("mutation-error"); await store.toggle("activity");
  const row = store.getState().details.activity;
  const html = renderToStaticMarkup(<PostDetail post={postFixtures.activity} bookmark={{ isBookmarked: row.value!.isBookmarked, feedback: row.feedback, onToggle: () => {} }}/>);
  assert.equal((html.match(/북마크 해제/g) ?? []).length, 1); assert.ok(html.includes("실패")); assert.ok(!html.includes("저장되었습니다"));
  const pending = renderToStaticMarkup(<PostDetail post={postFixtures.activity} bookmark={{ isBookmarked: true, pending: true, onToggle: () => {} }}/>);
  assert.match(pending, /disabled=""[^>]*>북마크 해제/);
});
test("guest single-button binding preserves ordinary/shared returnTo and never reads or automatically saves", () => {
  for (const id of ["post", "sharedPost"] as const) {
    const mock = createBookmarksMock(); let calls = 0;
    const service = { ...mock.service, get: async () => { calls++; return { postId: "vote", available: true, isBookmarked: false }; }, set: async () => { calls++; return { postId: "vote", available: true, isBookmarked: true }; } };
    const entry: NavigationEntry = { destination: { id, params: { postId: "vote" } }, origin: { id: id === "sharedPost" ? "start" : "home" }, ...(id === "sharedPost" ? { sharedContextRef: "opaque-preview-context" } : {}) };
    let binding: BookmarkBinding | undefined; let nav!: ReturnType<typeof useNavigation>; const delivered: string[] = [];
    function Probe() { binding = useBookmarkBinding("vote", entry); nav = useNavigation(); return null; }
    renderToStaticMarkup(<SessionProvider session={{ status: "guest" }}><BookmarksProvider subjectKey={subject} service={service}><NavigationProvider currentDestination={entry.destination} onNavigate={href => { delivered.push(href); }}><Probe/></NavigationProvider></BookmarksProvider></SessionProvider>);
    assert.equal(binding?.isBookmarked, false); binding?.onToggle?.(); assert.ok(delivered.at(-1)?.includes("login"));
    // Existing navigation deliberately converts shared guest detail to the same member post.
    nav.completeAuthentication(member, "available"); assert.equal(delivered.at(-1), "/posts/vote"); assert.equal(calls, 0);
  }
});
