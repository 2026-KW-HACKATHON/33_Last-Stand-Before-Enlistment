import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { AccountInfoView } from "../AccountInfoScreen";
import { createAccountInfoStore, accountEntry, type AccountInfo } from "../model";
import { createMockAccountInfoService } from "../mock";
import { createNavigationStore, evaluateGuard, resolveDestination, backDestination, NavigationProvider, useNavigation, type NavigationEntry } from "../../../lib/navigation";
import { settingsEntry, settingsMenuEntry } from "../../settings/model";
import { menuEntry } from "../../my-page/model";
const tick = () => new Promise<void>(done => setTimeout(done, 5));
const member = { status: "member", capabilities: { status: "ready", grants: [] } } as const;

test("registered email loads and explicit null is the only empty state", async () => {
  for (const scenario of ["normal", "empty"] as const) { const store = createAccountInfoStore(createMockAccountInfoService(scenario, 0)); const loading = store.load(); assert.equal(store.getState().phase, "loading"); await loading; assert.equal(store.getState().phase, scenario === "empty" ? "empty" : "ready"); assert.equal(store.getState().account?.registeredEmail, scenario === "empty" ? null : "neighbor@example.com"); store.dispose(); }
});
test("load failure and missing adapter never fabricate empty email or withdrawal", async () => {
  const service = createMockAccountInfoService("error", 0); const store = createAccountInfoStore(service); await store.load(); assert.equal(store.getState().phase, "error"); assert.equal(store.getState().account, null); await store.retry(); assert.equal(store.getState().phase, "ready"); const unavailable = createAccountInfoStore(null); await unavailable.load(); assert.equal(unavailable.getState().phase, "error"); unavailable.dispose(); store.dispose();
});
test("invalid adapter display results stay errors", async () => { for (const value of [undefined, { registeredEmail: "" }, { registeredEmail: 7 }]) { const store = createAccountInfoStore({ async load() { return value as AccountInfo; } }); await store.load(); assert.equal(store.getState().phase, "error"); store.dispose(); } });
test("changed callback re-queries same source rather than fabricating an email", async () => {
  const service = createMockAccountInfoService("normal", 0); const store = createAccountInfoStore(service); await store.load(); store.startChange(true); const request = store.getChangeRequest()!; assert.deepEqual(request.entry, { destination: { id: "emailChange" }, origin: { id: "account" } }); service.simulateChangedResult(); request.onChanged(); assert.equal(store.getState().phase, "loading"); assert.equal(store.getState().account, null); await tick(); assert.equal(store.getState().account?.registeredEmail, "new@example.com"); assert.equal(store.getState().changed, true); store.dispose();
});
test("changed callback refresh failure stays Error and explicit Retry obtains the new email", async () => {
  const service = createMockAccountInfoService("refresh-error", 0); const store = createAccountInfoStore(service); await store.load(); store.startChange(true); service.simulateChangedResult(); store.getChangeRequest()!.onChanged(); await tick(); assert.equal(store.getState().phase, "error"); assert.equal(store.getState().account, null); await store.retry(); assert.equal(store.getState().account?.registeredEmail, "new@example.com"); assert.equal(store.getState().changed, true); store.dispose();
});
test("cancel/failure keep stored email, preserve L02 scroll and permit explicit retry", async () => {
  const store = createAccountInfoStore(createMockAccountInfoService("normal", 0)); await store.load(); store.setScroll(99); store.startChange(true); const request = store.getChangeRequest()!; request.onCancel(); assert.equal(request.signal.aborted, true); assert.equal(store.getState().account?.registeredEmail, "neighbor@example.com"); assert.equal(store.getState().scroll, 99); store.startChange(true); store.getChangeRequest()!.onFailure(); assert.equal(store.getState().changePhase, "error"); assert.equal(store.getState().account?.registeredEmail, "neighbor@example.com"); store.startChange(true); assert.equal(store.getState().changePhase, "active"); store.dispose();
});
test("unavailable #93 port never opens a fake email change route", async () => { const store = createAccountInfoStore(createMockAccountInfoService("normal", 0)); await store.load(); store.startChange(false); assert.equal(store.getState().changePhase, "unavailable"); assert.equal(store.getChangeRequest(), null); assert.equal(store.getState().account?.registeredEmail, "neighbor@example.com"); store.dispose(); });
test("pending query and active change ignore duplicate change requests", async () => { const store = createAccountInfoStore(createMockAccountInfoService("normal", 0)); const pending = store.load(); store.startChange(true); assert.equal(store.getChangeRequest(), null); await pending; store.startChange(true); const first = store.getChangeRequest(); store.startChange(true); assert.equal(store.getChangeRequest(), first); store.dispose(); });
test("back cancellation ignores late changed callbacks and async results", async () => {
  let resolve!: (value: AccountInfo) => void; let signal!: AbortSignal; const store = createAccountInfoStore({ load(requestSignal) { signal = requestSignal; return new Promise(done => { resolve = done; }); } }); const pending = store.load(); store.cancel(); assert.equal(signal.aborted, true); resolve({ registeredEmail: "stale@example.com" }); await pending; assert.equal(store.getState().phase, "idle"); assert.equal(store.getState().account, null); store.dispose();
  const ready = createAccountInfoStore(createMockAccountInfoService("normal", 0)); await ready.load(); ready.startChange(true); const request = ready.getChangeRequest()!; ready.cancel(); request.onChanged(); assert.equal(ready.getState().phase, "ready"); assert.equal(ready.getState().account?.registeredEmail, "neighbor@example.com"); ready.dispose();
});
test("account switch disposal aborts old load/change and blocks stale callbacks", async () => {
  let resolve!: (value: AccountInfo) => void; const old = createAccountInfoStore({ load() { return new Promise(done => { resolve = done; }); } }); const pending = old.load(); old.dispose(); resolve({ registeredEmail: "old@example.com" }); await pending; assert.equal(old.getState().account, null);
  const store = createAccountInfoStore(createMockAccountInfoService("normal", 0)); await store.load(); store.startChange(true); const request = store.getChangeRequest()!; store.dispose(); request.onChanged(); assert.equal(request.signal.aborted, true); assert.equal(store.getState().account?.registeredEmail, "neighbor@example.com"); const next = createAccountInfoStore({ async load() { return { registeredEmail: "other@example.com" }; } }); await next.load(); assert.equal(next.getState().account?.registeredEmail, "other@example.com"); next.dispose();
});
test("L01 and I01 entries retain original return destinations; L01 retains its home origin", () => {
  for (const entry of [settingsMenuEntry("account")!, menuEntry("account")!]) { const target = accountEntry(entry); assert.equal(backDestination(target).id, entry.origin?.id); }
  const parent = settingsEntry(null, { destination: { id: "settings" }, origin: { id: "home" } }); assert.equal(settingsEntry(parent, { destination: { id: "settings" }, origin: { id: "settings" } }).origin?.id, "home");
  assert.equal(resolveDestination({ id: "account" }).status, "unresolved"); assert.equal(resolveDestination({ id: "emailChange" }).status, "unresolved");
});
test("logical account intent and auth return preserve origin without inventing URLs", () => {
  for (const source of ["me", "settings"] as const) { const entry = { destination: { id: "account" as const }, origin: { id: source } }; const nav = createNavigationStore({ destination: { id: "me" } }); nav.beginAuthentication(entry); const intents: NavigationEntry[] = []; assert.equal(nav.completeAuthentication(member, "available", e => intents.push(e)).status, "unresolved"); assert.deepEqual(intents[0], entry); assert.equal(nav.getState().returnTo, null); }
  let api!: ReturnType<typeof useNavigation>; const entries: NavigationEntry[] = []; const paths: string[] = []; function Capture() { api = useNavigation(); return null; } renderToStaticMarkup(<NavigationProvider currentDestination={{ id: "me" }} onNavigate={href => paths.push(href)} onIntent={entry => entries.push(entry)}><Capture/></NavigationProvider>); api.navigate(menuEntry("account")!); assert.equal(entries[0].destination.id, "account"); assert.equal(entries[0].origin?.id, "me"); assert.deepEqual(paths, []);
});
test("account permission requires membership, not neighbor/institution certification", () => { for (const [status, expected] of [["guest", "login-required"], ["signup-incomplete", "signup-required"], ["loading", "loading"], ["error", "error"]] as const) assert.equal(evaluateGuard({ id: "account" }, { status }).status, expected); assert.equal(evaluateGuard({ id: "account" }, member).status, "allowed"); });
test("SSR L02 reuses Notice/Menu and omits removed password/withdrawal controls", async () => {
  const store = createAccountInfoStore(createMockAccountInfoService("normal", 0)); const render = () => renderToStaticMarkup(<AccountInfoView state={store.getState()} onBack={() => {}} onRetry={() => {}} onChange={() => {}}/>); assert.match(render(), /disabled/); await store.load(); const html = render(); assert.match(html, /등록 이메일 · neighbor@example.com/); assert.match(html, /이메일 변경/); assert.match(html, /rounded-notice/); assert.match(html, /rounded-card/); assert.doesNotMatch(html, /비밀번호|탈퇴|<input/); store.dispose();
});
