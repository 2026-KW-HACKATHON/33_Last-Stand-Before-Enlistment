import assert from "node:assert/strict";
import { test } from "node:test";
import { renderToStaticMarkup } from "react-dom/server";
import { AccessGuard, SessionProvider, useSession } from "../session";
import { NavigationProvider, useNavigation } from "../context";
import { evaluateGuard, type Capability, type SessionState } from "../guard";
import { accountReturnDestinations, bottomNavigationDestinations, destinationFromPathname, headerDestinations, parseDestination, resolveDestination, routes, type Destination } from "../routes";
import { backDestination, createNavigationStore, snapshotReference, type SnapshotKind } from "../state";

const post: Destination = { id: "post", params: { postId: "post-가" } };
const shared: Destination = { id: "sharedPost", params: { postId: "post-가" } };
const member = (grants: { capability: Capability; allowed: boolean }[] = []): SessionState => ({ status: "member", capabilities: { status: "ready", grants } });

test("all twenty confirmed destinations generate and round-trip their documented path", () => {
  let confirmed = 0;
  for (const [id, route] of Object.entries(routes)) {
    if (route.path === null) continue;
    const destination = parseDestination({ id, params: { postId: "post-가" } });
    assert.ok(destination);
    const result = resolveDestination(destination);
    assert.equal(result.status, "ready");
    if (result.status !== "ready") throw new Error("expected ready");
    assert.equal(result.href, route.path.replace("[postId]", "post-%EA%B0%80"));
    assert.deepEqual(destinationFromPathname(result.href), destination);
    confirmed++;
  }
  assert.equal(confirmed, 20);
  assert.deepEqual(destinationFromPathname("/posts/new"), { id: "newPost" });
  assert.equal(parseDestination({ id: "post", params: { postId: "new" } }), null);
});
test("restored logical destinations remain unresolved; navigation does not invent URLs", () => {
  const store = createNavigationStore({ destination: { id: "me" } });
  for (const [id, route] of Object.entries(routes)) {
    if (route.path !== null) continue;
    const destination = parseDestination({ id });
    assert.ok(destination);
    assert.equal(resolveDestination(destination).status, "unresolved");
    assert.equal(store.navigate({ destination }).status, "unresolved");
    assert.deepEqual(store.getState().current?.destination, { id: "me" });
  }
});
test("five navigation items, Header actions and logout/withdrawal returns use identities", () => {
  assert.deepEqual(Object.keys(bottomNavigationDestinations), ["main", "map", "write", "notification", "my"]);
  assert.deepEqual(headerDestinations.settings, { id: "settings" });
  assert.deepEqual(headerDestinations.notification, bottomNavigationDestinations.notification);
  assert.deepEqual(accountReturnDestinations, { logout: { id: "start" }, withdrawal: { id: "login" } });
});
test("external, unknown, prototype and discarded verification targets are rejected", () => {
  for (const value of ["https://evil.test", "//evil.test", { id: "https://evil.test" }, { id: "__proto__" }, { id: "constructor" }, { id: "verification" }]) {
    assert.equal(parseDestination(value), null);
    assert.equal(resolveDestination(value).status, "invalid");
  }
  for (const path of ["https://evil.test/home", "//evil.test", "/home?returnTo=evil", "/home#x", "/home\\x", "/verifications/neighbor", "/verifications/institution", "/settings", "/search", "/notifications"]) assert.equal(destinationFromPathname(path), null);
});
test("dynamic identifiers cannot inject traversal, extra segments, query or malformed encoding", () => {
  for (const postId of ["", ".", "..", "a/b", "a\\b", "a?b", "a#b", "%2f", "\u0000", "\ud800"]) assert.equal(parseDestination({ id: "post", params: { postId } }), null);
  for (const pathname of ["/posts/%2F", "/posts/%2e%2e", "/posts/%", "/posts/a/b", "/posts/%252f", "/posts/%5c"]) assert.equal(destinationFromPathname(pathname), null);
});
test("public access, session loading/error, guest, incomplete signup and member are distinct", () => {
  for (const status of ["loading", "guest", "signup-incomplete", "error"] as const) assert.equal(evaluateGuard({ id: "login" }, { status }).status, "allowed");
  assert.equal(evaluateGuard(post, { status: "loading" }).status, "loading");
  assert.equal(evaluateGuard(post, { status: "error" }).status, "error");
  assert.equal(evaluateGuard(post, { status: "guest" }).status, "login-required");
  assert.equal(evaluateGuard(post, { status: "signup-incomplete" }).status, "signup-required");
  assert.equal(evaluateGuard(post, member()).status, "allowed");
});
test("neighbor-region grant is scoped; missing, loading and failed assertions are not forbidden", () => {
  const destination: Destination = { id: "newPost" };
  const capability: Capability = { kind: "neighbor-region", regionId: "r1" };
  assert.equal(evaluateGuard(destination, member([{ capability, allowed: true }]), { regionId: "r1" }).status, "allowed");
  assert.equal(evaluateGuard(destination, member([{ capability, allowed: false }]), { regionId: "r1" }).status, "forbidden");
  assert.equal(evaluateGuard(destination, member([{ capability, allowed: true }]), { regionId: "r2" }).status, "error");
  assert.equal(evaluateGuard(destination, member()).status, "error");
  for (const status of ["loading", "error"] as const) assert.equal(evaluateGuard(destination, { status: "member", capabilities: { status } }, { regionId: "r1" }).status, status);
});
test("institution access and responsible-region action are separate assertions", () => {
  const institution: Capability = { kind: "institution" };
  const responsible: Capability = { kind: "responsible-region", regionId: "r1" };
  const session = member([{ capability: institution, allowed: true }, { capability: responsible, allowed: false }]);
  const destination: Destination = { id: "officerAgenda", params: { postId: "p1" } };
  assert.equal(evaluateGuard(destination, session).status, "allowed");
  assert.equal(evaluateGuard(destination, session, { requirements: [responsible] }).status, "forbidden");
  assert.equal(evaluateGuard(destination, member([{ capability: responsible, allowed: true }])).status, "error");
  assert.equal(evaluateGuard(destination, member([{ capability: institution, allowed: false }])).status, "forbidden");
});
test("edit permission cannot leak across posts or be inferred from another capability", () => {
  const session = member([{ capability: { kind: "edit-post", postId: "a" }, allowed: true }]);
  assert.equal(evaluateGuard({ id: "editPost", params: { postId: "a" } }, session).status, "allowed");
  assert.equal(evaluateGuard({ id: "editPost", params: { postId: "b" } }, session).status, "error");
  const duplicate = { capability: { kind: "institution" } as Capability, allowed: true };
  assert.equal(evaluateGuard({ id: "officerAgendas" }, member([duplicate, duplicate])).status, "error");
});
test("shared guest access needs a verified assertion for this post; session errors stay errors", () => {
  assert.equal(evaluateGuard(shared, { status: "guest" }).status, "loading");
  assert.equal(evaluateGuard(shared, { status: "guest" }, { sharedAccess: { status: "ready", postId: "post-가" } }).status, "allowed");
  assert.equal(evaluateGuard(shared, { status: "guest" }, { sharedAccess: { status: "ready", postId: "other" } }).status, "forbidden");
  for (const status of ["loading", "error", "unavailable"] as const) assert.equal(evaluateGuard(shared, { status: "guest" }, { sharedAccess: { status } }).status, status);
  assert.equal(evaluateGuard(shared, { status: "error" }, { sharedAccess: { status: "ready", postId: "post-가" } }).status, "error");
});
test("AccessGuard never renders protected content during loading, failure or denied access", () => {
  for (const session of [{ status: "loading" }, { status: "guest" }, { status: "signup-incomplete" }, { status: "error" }] as const) {
    const html = renderToStaticMarkup(<SessionProvider session={session}><AccessGuard destination={post} fallback={result => <span>{result.status}</span>}><b>protected-content</b></AccessGuard></SessionProvider>);
    assert.ok(!html.includes("protected-content"));
    assert.ok(html.includes(evaluateGuard(post, session).status));
  }
  const html = renderToStaticMarkup(<SessionProvider session={member()}><AccessGuard destination={post} fallback={() => "blocked"}>protected-content</AccessGuard></SessionProvider>);
  assert.equal(html, "protected-content");
});
test("production provider default stays loading; Retry callback is passed through", () => {
  function Consumer() { return <span>{useSession().session.status}</span>; }
  assert.equal(renderToStaticMarkup(<SessionProvider><Consumer /></SessionProvider>), "<span>loading</span>");
  const retry = () => {};
  let received: unknown;
  renderToStaticMarkup(<SessionProvider session={{ status: "error" }} retry={retry}><AccessGuard destination={post} fallback={(result, callback) => { received = callback; assert.equal(result.status, "error"); return null; }}>hidden</AccessGuard></SessionProvider>);
  assert.equal(received, retry);
});
test("NavigationProvider exposes isolated current destination without navigating on render", () => {
  function Consumer() { return <span>{useNavigation().state.current?.destination.id}</span>; }
  const html = renderToStaticMarkup(<NavigationProvider currentDestination={post} onNavigate={() => assert.fail("render must not navigate")}><Consumer /></NavigationProvider>);
  assert.equal(html, "<span>post</span>");
  assert.throws(() => renderToStaticMarkup(<Consumer />), /NavigationProvider/);
});
test("ordinary login preserves target through failures and consumes it only on confirmed completion", () => {
  const store = createNavigationStore({ destination: post, origin: { id: "board" } });
  store.beginAuthentication();
  const saved = store.getState().returnTo;
  assert.equal(store.completeAuthentication({ status: "error" }, "available").status, "error");
  assert.equal(store.completeAuthentication({ status: "loading" }, "available").status, "loading");
  assert.equal(store.completeAuthentication(member(), "error").status, "error");
  assert.equal(store.getState().returnTo, saved);
  const result = store.completeAuthentication(member(), "available");
  assert.equal(result.status, "ready");
  assert.deepEqual(store.getState().current, { destination: post, origin: { id: "board" } });
  assert.equal(store.getState().returnTo, null);
});
test("shared login/signup round trips return to the same member post without action replay", () => {
  const store = createNavigationStore({ destination: shared, sharedContextRef: "share-context-ref" });
  store.beginAuthentication();
  const saved = store.getState().returnTo;
  store.completeAuthentication({ status: "signup-incomplete" }, "available");
  assert.equal(store.getState().current?.destination.id, "signup");
  store.navigate({ destination: { id: "login" } });
  store.beginAuthentication(undefined, "signup");
  assert.equal(store.getState().returnTo, saved);
  store.completeAuthentication(member(), "available");
  assert.deepEqual(store.getState().current?.destination, post);
  assert.equal(store.getState().returnTo, null);
  assert.ok(!JSON.stringify(store.getState()).includes("pendingAction"));
});
test("authentication cancellation restores original shared context", () => {
  const entry = { destination: shared, sharedContextRef: "shared-ref" };
  const store = createNavigationStore(entry);
  store.beginAuthentication();
  store.completeAuthentication({ status: "signup-incomplete" }, "available");
  store.cancelAuthentication();
  assert.deepEqual(store.getState().current, entry);
  assert.equal(store.getState().returnTo, null);
  assert.deepEqual(store.getState().history, []);
});
test("guard-triggered login preserves requested destination and cancels to the original Page", () => {
  const store = createNavigationStore({ destination: { id: "home" } });
  store.beginAuthentication({ destination: { id: "newPost" }, origin: { id: "home" } });
  assert.deepEqual(store.getState().returnTo?.target.destination, { id: "newPost" });
  store.cancelAuthentication();
  assert.deepEqual(store.getState().current?.destination, { id: "home" });
});
test("unavailable authenticated target emits notice and home fallback without deleted redirect loop", () => {
  const store = createNavigationStore({ destination: shared });
  store.beginAuthentication();
  assert.equal(store.completeAuthentication(member(), "loading").status, "loading");
  const result = store.completeAuthentication(member(), "unavailable");
  assert.deepEqual(result, { status: "ready", destination: { id: "home" }, href: "/home", notice: "unavailable" });
  assert.equal(store.getState().returnTo, null);
  assert.deepEqual(store.getState().history, []);
});
test("runtime returnTo sanitization rejects external identity and discards executable action fields", () => {
  const store = createNavigationStore({ destination: post });
  assert.equal(store.beginAuthentication({ destination: { id: "https://evil.test" } as unknown as Destination }).status, "invalid");
  assert.equal(store.getState().returnTo, null);
  const untrusted = { destination: { ...post, pendingAction: "vote" }, pendingAction: "bookmark" };
  store.beginAuthentication(untrusted);
  assert.ok(!JSON.stringify(store.getState()).includes("pendingAction"));
  assert.deepEqual(store.getState().returnTo?.target.destination, post);
});
for (const id of ["bookmarks", "myPosts", "participations"] as const) {
  test(`${id}: I01 origin returns to I01; L01 origin stays logical until URL is confirmed`, () => {
    const store = createNavigationStore({ destination: { id: "me" } });
    store.navigate({ destination: { id }, origin: { id: "me" } });
    assert.equal(store.back().status, "ready");
    assert.deepEqual(store.getState().current?.destination, { id: "me" });
    store.navigate({ destination: { id }, origin: { id: "settings" } });
    assert.deepEqual(backDestination(store.getState().current!), { id: "settings" });
    assert.deepEqual(store.back(), { status: "unresolved", destination: { id: "settings" } });
  });
}
test("native pathname back restores entry origin and no state is shared across stores", () => {
  const store = createNavigationStore({ destination: { id: "bookmarks" }, origin: { id: "settings" } });
  store.navigate({ destination: post });
  store.syncDestination({ id: "bookmarks" });
  assert.deepEqual(store.getState().current?.origin, { id: "settings" });
  assert.equal(createNavigationStore().getState().current, null);
  store.syncDestination(null);
  assert.equal(store.getState().current, null);
});
for (const kind of ["list", "search", "map", "form"] as const satisfies readonly SnapshotKind[]) {
  test(`${kind} snapshot retains only owner reference, is scoped, and handles missing reference`, () => {
    const store = createNavigationStore({ destination: { id: "board" } });
    assert.equal(snapshotReference(store.getState(), { id: "board" }, kind), undefined);
    store.registerSnapshot({ destination: { id: "board" }, kind, ref: `${kind}:owner-reference` });
    store.registerSnapshot({ destination: post, kind, ref: "different-reference" });
    store.navigate({ destination: post, origin: { id: "board" } });
    store.back();
    assert.equal(snapshotReference(store.getState(), { id: "board" }, kind), `${kind}:owner-reference`);
    assert.equal(snapshotReference(store.getState(), post, kind), "different-reference");
    store.registerSnapshot({ destination: { id: "board" }, kind, ref: "updated-reference" });
    assert.equal(store.getState().snapshots.length, 2);
    store.removeSnapshot({ id: "board" }, kind);
    assert.equal(snapshotReference(store.getState(), { id: "board" }, kind), undefined);
    store.clear();
    assert.deepEqual(store.getState(), { current: null, history: [], returnTo: null, snapshots: [] });
  });
}
