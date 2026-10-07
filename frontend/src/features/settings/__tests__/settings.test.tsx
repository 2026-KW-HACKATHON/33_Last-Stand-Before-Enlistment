import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { SettingRow } from "../../../components/ui/SettingRow";
import { NavigationProvider, useNavigation, createNavigationStore, backDestination, snapshotReference, evaluateGuard, resolveDestination, type NavigationEntry } from "../../../lib/navigation";
import { createSettingsMenuStore, settingsEntry, settingsMenuEntry, settingsMenus } from "../model";
import { createMockSettingsHandler } from "../mock";

test("L01 maps eight menus to existing Owner identities; comments use I04", () => {
 assert.equal(settingsMenus.length, 8); assert.equal(settingsMenuEntry("participations")?.destination.id, "participations"); assert.equal(settingsMenuEntry("logout"), null);
 assert.equal(resolveDestination({ id: "settings" }).status, "unresolved");
 for (const item of settingsMenus) { const entry = settingsMenuEntry(item.id); if (entry) assert.equal(entry.origin?.id, "settings"); }
});
test("L01 remembers home/me entry after a list returns; I01 remains separate", () => {
 for (const source of ["home", "me"] as const) { const original = settingsEntry(null, { destination: { id: "settings" }, origin: { id: source } });
  for (const id of ["myPosts", "participations", "bookmarks"] as const) { const list = settingsMenuEntry(id)!; assert.equal(backDestination(list).id, "settings"); assert.equal(settingsEntry(original, { destination: backDestination(list), origin: list.origin }).origin?.id, source); assert.equal(backDestination({ destination: list.destination, origin: { id: "me" } }).id, "me"); }
 }
});
test("Owner list snapshot references are forwarded; filter/tab/scroll values stay Owner-owned", async () => {
 const nav = createNavigationStore({ destination: { id: "me" } });
 nav.registerSnapshot({ destination: { id: "participations" }, kind: "list", ref: "owner:i04" });
 const entry = settingsMenuEntry("participations", snapshotReference(nav.getState(), { id: "participations" }, "list"));
 let received: NavigationEntry | null = null; const store = createSettingsMenuStore((_id, target) => { received = target; }); await store.open("participations", entry, () => {});
 assert.deepEqual(received, { destination: { id: "participations" }, origin: { id: "settings" }, sharedContextRef: "owner:i04" }); store.dispose();
});
test("missing Owner adapter stays on L01 with explicit unavailable state", async () => { const store = createSettingsMenuStore(); await store.open("account", settingsMenuEntry("account"), () => {}); assert.equal(store.getState().phase, "unavailable"); store.dispose(); });
test("failed handoff retries explicitly and invokes return callback without account mutation", async () => { let opened = 0, returned = 0; const handler = createMockSettingsHandler("error-retry", (_id, _entry, back) => { opened++; back(); }); const store = createSettingsMenuStore(handler); await store.open("logout", null, () => returned++); assert.equal(store.getState().phase, "error"); assert.equal(opened, 0); await store.open("logout", null, () => returned++); assert.equal(store.getState().phase, "success"); assert.equal(opened, 1); assert.equal(returned, 1); store.dispose(); });
test("pending disables duplicate handoff; cancellation invalidates stale success/return", async () => { let resolve!: () => void, back!: () => void; let calls = 0, returned = 0; const store = createSettingsMenuStore((_id, _entry, onReturn) => { calls++; back = onReturn; return new Promise<void>(done => { resolve = done; }); }); const pending = store.open("account", settingsMenuEntry("account"), () => returned++); assert.equal(store.getState().phase, "pending"); await store.open("withdrawal", settingsMenuEntry("withdrawal"), () => returned++); assert.equal(calls, 1); store.cancel(); back(); resolve(); await pending; assert.equal(returned, 0); assert.equal(store.getState().phase, "idle"); store.dispose(); });
test("SettingRow pending semantics and L01 permission states", () => { const markup = renderToStaticMarkup(<SettingRow label="로그아웃" pending/>); assert.match(markup, /disabled/); assert.match(markup, /aria-busy="true"/); for (const [status, expected] of [["guest", "login-required"], ["signup-incomplete", "signup-required"], ["loading", "loading"], ["error", "error"]] as const) assert.equal(evaluateGuard({ id: "settings" }, { status }).status, expected); });

test("central logical intent connects home/settings/list back without inventing a URL", () => {
 let api!: ReturnType<typeof useNavigation>; const intents: NavigationEntry[] = []; const paths: string[] = [];
 function Capture() { api = useNavigation(); return null; }
 renderToStaticMarkup(<NavigationProvider currentDestination={{ id: "home" }} onNavigate={href => paths.push(href)} onIntent={entry => intents.push(entry)}><Capture/></NavigationProvider>);
 api.navigate({ destination: { id: "settings" }, origin: { id: "home" } });
 assert.deepEqual(intents[0], { destination: { id: "settings" }, origin: { id: "home" } }); assert.deepEqual(paths, []);
 api.navigate(settingsMenuEntry("bookmarks")!); assert.deepEqual(paths, ["/me/bookmarks"]);
 assert.equal(api.back().status, "unresolved"); assert.deepEqual(intents[1], { destination: { id: "settings" }, origin: { id: "settings" } });
});
