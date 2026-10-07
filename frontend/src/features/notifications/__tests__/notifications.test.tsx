import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { createNavigationStore, evaluateGuard, resolveDestination, NavigationProvider, useNavigation, headerDestinations, bottomNavigationDestinations, type NavigationEntry } from "../../../lib/navigation";
import { createNotificationStore, filteredNotifications, type NotificationService, type NotificationState, type NotificationItemModel } from "../model";
import { createMockNotificationService, notificationFixtures } from "../mock";
import { NotificationsView } from "../NotificationsScreen";
import { NotificationItem } from "../NotificationItem";

const noop = () => {};
const view = (state: NotificationState) => renderToStaticMarkup(<NotificationsView state={state} onTab={noop} onSelect={noop} onRetryLoad={noop} onRetryItem={noop} onBack={noop} onReturn={noop} onNavigate={noop}/>);
test("list filters notification/activity without losing reads or order", async () => {
  const store = createNotificationStore(createMockNotificationService("success", 0)); await store.load();
  assert.equal(store.getState().items.length, notificationFixtures.length);
  store.setTab("activity"); assert.deepEqual(filteredNotifications(store.getState()).map(item => item.id), ["participation"]);
  store.setTab("notification"); assert.ok(filteredNotifications(store.getState()).every(item => item.category === "notification"));
  store.setTab("all"); assert.equal(filteredNotifications(store.getState()).length, notificationFixtures.length);
});
test("mark-read precedes original resolution and detail return preserves list/tab/scroll", async () => {
  const mock = createMockNotificationService("success", 0); const calls: string[] = [];
  const service: NotificationService = { ...mock, async markRead(id, signal) { calls.push("read"); await mock.markRead(id, signal); }, async resolveTarget(id, signal) { calls.push("target"); return mock.resolveTarget(id, signal); } };
  const store = createNotificationStore(service); await store.load(); store.setTab("notification"); store.setScroll(412);
  const target = await store.open("comment"); assert.deepEqual(calls, ["read", "target"]);
  assert.equal(target?.destination.params.postId, "agenda-photo"); store.returnToList();
  assert.equal(store.getState().tab, "notification"); assert.equal(store.getState().scroll, 412); assert.equal(store.getState().items[0].read, true);
  await store.load(); assert.equal(store.getState().items[0].read, true);
});
test("read failure does not alter unread state or navigate; retry succeeds", async () => {
  const store = createNotificationStore(createMockNotificationService("read-error", 0)); await store.load();
  assert.equal(await store.open("comment"), null); assert.equal(store.getState().phase, "read-error"); assert.equal(store.getState().items[0].read, false);
  assert.equal((await store.retry())?.destination.params.postId, "agenda-photo"); assert.equal(store.getState().items[0].read, true);
});
test("target failure preserves completed read and retry does not repeat mark-read", async () => {
  const mock = createMockNotificationService("target-error", 0); let reads = 0;
  const store = createNotificationStore({ ...mock, async markRead(id, signal) { reads++; await mock.markRead(id, signal); } }); await store.load();
  assert.equal(await store.open("comment"), null); assert.equal(store.getState().phase, "target-error"); assert.equal(store.getState().items[0].read, true);
  assert.ok(await store.retry()); assert.equal(reads, 1);
});
for (const status of ["deleted", "inaccessible"] as const) test(`${status} original shows H03, never original body, and returns with state`, async () => {
  const store = createNotificationStore(createMockNotificationService(status, 0)); await store.load(); store.setScroll(300);
  assert.equal(await store.open("comment"), null); assert.equal(store.getState().phase, status);
  const html = view(store.getState()); assert.match(html, /알림으로/); assert.doesNotMatch(html, /보행로 개선|새 댓글|하단 메뉴/);
  store.returnToList(); assert.equal(store.getState().scroll, 300); assert.equal(store.getState().phase, "ready");
});
test("load error/Retry, Empty, Loading and no-adapter failure are explicit", async () => {
  const store = createNotificationStore(createMockNotificationService("load-error", 0)); assert.match(view(store.getState()), /불러오는 중/);
  await store.load(); assert.match(view(store.getState()), /다시 조회/); await store.load(); assert.equal(store.getState().phase, "ready");
  const empty = createNotificationStore(createMockNotificationService("empty", 0)); await empty.load(); assert.match(view(empty.getState()), /아직 알림이 없습니다/);
  const missing = createNotificationStore(null); await missing.load(); assert.equal(missing.getState().phase, "load-error");
});
test("pending duplicate clicks are ignored; cancel aborts read without a late result", async () => {
  const store = createNotificationStore(createMockNotificationService("pending", 0)); await store.load();
  const request = store.open("comment"); assert.equal(store.getState().phase, "pending"); assert.equal(await store.open("reaction"), null);
  assert.match(view(store.getState()), /취소/); store.returnToList(); assert.equal(await request, null);
  assert.equal(store.getState().items[0].read, false); assert.equal(store.getState().phase, "ready");
});
test("account disposal suppresses late load/read results; a new store starts clean", async () => {
  let finish!: (items: NotificationItemModel[]) => void;
  const mock = createMockNotificationService("success", 0);
  const store = createNotificationStore({ ...mock, load: () => new Promise(resolve => { finish = resolve; }) });
  const request = store.load(); store.dispose(); finish([...notificationFixtures]); await request; assert.equal(store.getState().items.length, 0);
  const next = createNotificationStore(mock); assert.equal(next.getState().tab, "all"); assert.equal(next.getState().scroll, 0);
});
test("adoption and cancellation point at existing original agendas, never a fabricated record URL", async () => {
  const store = createNotificationStore(createMockNotificationService("success", 0)); await store.load();
  assert.deepEqual(await store.open("adoption"), { kind: "adoption-record", destination: { id: "post", params: { postId: "agenda-photo" } } });
  assert.deepEqual(await store.open("adoption-cancel"), { kind: "adoption-record", destination: { id: "post", params: { postId: "agenda-anonymous" } } });
});
test("ordinary detail back delivers notifications intent and list snapshot", () => {
  const nav = createNavigationStore({ destination: { id: "home" } });
  nav.registerSnapshot({ destination: { id: "notifications" }, kind: "list", ref: "notifications-list" });
  nav.navigate({ destination: { id: "post", params: { postId: "vote" } }, origin: { id: "notifications" } });
  assert.equal(nav.back().status, "unresolved"); assert.equal(nav.getState().current?.origin?.id, "notifications");
  assert.equal(nav.getState().snapshots[0].ref, "notifications-list"); assert.equal(resolveDestination({ id: "notifications" }).status, "unresolved");
});
test("notifications reject guest/incomplete sessions and do not need neighbor/institution qualification", () => {
  assert.equal(evaluateGuard({ id: "notifications" }, { status: "guest" }).status, "login-required");
  assert.equal(evaluateGuard({ id: "notifications" }, { status: "signup-incomplete" }).status, "signup-required");
  assert.equal(evaluateGuard({ id: "notifications" }, { status: "member", capabilities: { status: "ready", grants: [] } }).status, "allowed");
});
test("NotificationItem exposes read/unread semantics, title, description, time, and disabled pending", () => {
  const unread = renderToStaticMarkup(<NotificationItem item={notificationFixtures[0]} onSelect={noop} disabled pending/>);
  assert.match(unread, /미확인/); assert.match(unread, /5분 전/); assert.match(unread, /aria-busy="true"/); assert.match(unread, /disabled/);
  const read = renderToStaticMarkup(<NotificationItem item={{ ...notificationFixtures[0], read: true }} onSelect={noop}/>);
  assert.doesNotMatch(read, /미확인/); assert.match(read, /읽음/);
});
test("Header and BottomNavigation share notifications intent; detail back invokes the same consumer", () => {
  let navigation!: ReturnType<typeof useNavigation>; const intents: NavigationEntry[] = []; const paths: string[] = [];
  function Capture() { navigation = useNavigation(); return null; }
  renderToStaticMarkup(<NavigationProvider currentDestination={{ id: "home" }} onIntent={entry => intents.push(entry)} onNavigate={href => paths.push(href)}><Capture/></NavigationProvider>);
  navigation.navigate({ destination: headerDestinations.notification, origin: { id: "home" } });
  navigation.navigate({ destination: bottomNavigationDestinations.notification, origin: { id: "home" } });
  assert.deepEqual(intents[0], intents[1]); assert.equal(paths.length, 0);
  navigation.navigate({ destination: { id: "post", params: { postId: "vote" } }, origin: { id: "notifications" } });
  navigation.back(); assert.equal(intents.at(-1)?.destination.id, "notifications"); assert.deepEqual(paths, ["/posts/vote"]);
});
test("authentication preserves notifications returnTo and consumes it after membership without action replay", () => {
  const nav = createNavigationStore({ destination: { id: "home" } }); const intents: NavigationEntry[] = [];
  nav.beginAuthentication({ destination: { id: "notifications" }, origin: { id: "home" } });
  nav.completeAuthentication({ status: "signup-incomplete" }, "available"); assert.equal(nav.getState().returnTo?.target.destination.id, "notifications");
  nav.completeAuthentication({ status: "member", capabilities: { status: "ready", grants: [] } }, "available", entry => intents.push(entry));
  assert.equal(intents[0].destination.id, "notifications"); assert.equal(nav.getState().returnTo, null);
});
test("cancel before first load retains idle; disposed pending read cannot publish a late success", async () => {
  const mock = createMockNotificationService("success", 0); let finish!: () => void;
  const store = createNotificationStore({ ...mock, markRead: () => new Promise(resolve => { finish = resolve; }) });
  store.returnToList(); assert.equal(store.getState().phase, "idle"); await store.load();
  const opening = store.open("comment"); store.dispose(); finish(); assert.equal(await opening, null); assert.equal(store.getState().items[0].read, false);
});
