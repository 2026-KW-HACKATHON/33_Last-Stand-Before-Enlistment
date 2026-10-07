import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { renderToStaticMarkup } from "react-dom/server";
import { evaluateGuard, resolveDestination, createNavigationStore } from "../../../lib/navigation";
import { createPushPreferenceStore, type PushPreferenceService, type PushPreferenceState } from "../model";
import { createMockPushPreferenceService } from "../mock";
import { PushPreferenceView } from "../PushPreferenceScreen";
import { PushPreferenceToggle } from "../PushPreferenceToggle";
import { createMockNotificationService } from "../../notifications/mock";
import { createNotificationStore } from "../../notifications/model";
const noop = () => {};
const view = (state: PushPreferenceState) => renderToStaticMarkup(<PushPreferenceView state={state} onChange={noop} onRetryLoad={noop} onRetrySave={noop} onBack={noop}/>);
for (const enabled of [true, false]) test(`load ${enabled}, change, persist and requery`, async () => {
  const service = createMockPushPreferenceService(enabled ? "on" : "off", 0); const store = createPushPreferenceStore(service);
  await store.load(); assert.equal(store.getState().saved, enabled); assert.equal(store.getState().draft, enabled);
  assert.equal(await store.save(!enabled), true); assert.equal(store.getState().phase, "success"); assert.equal(store.getState().saved, !enabled);
  store.cancel(); await store.load(); assert.equal(store.getState().saved, !enabled);
});
test("unknown preference stays unset, with explicit ON/OFF choice instead of guessed default", async () => {
  const store = createPushPreferenceStore(createMockPushPreferenceService("unset", 0)); await store.load();
  assert.equal(store.getState().saved, null); assert.equal(store.getState().phase, "unset"); const html = view(store.getState());
  assert.match(html, /ON으로 설정/); assert.match(html, /OFF로 설정/); assert.doesNotMatch(html, /role="switch"/);
  await store.save(false); assert.equal(store.getState().saved, false);
});
test("save failure restores saved value, explicit Retry preserves failed false value", async () => {
  const store = createPushPreferenceStore(createMockPushPreferenceService("save-error", 0)); await store.load();
  assert.equal(await store.save(false), false); assert.equal(store.getState().saved, true); assert.equal(store.getState().draft, true);
  assert.match(view(store.getState()), /기존 저장값으로 복구/); assert.match(view(store.getState()), /다시 시도/);
  assert.equal(await store.retry(), true); assert.equal(store.getState().saved, false);
});
test("failure from unset returns null rather than inventing OFF", async () => {
  const mock = createMockPushPreferenceService("unset", 0); const store = createPushPreferenceStore({ ...mock, async save() { throw new Error("failure"); } });
  await store.load(); await store.save(true); assert.equal(store.getState().draft, null); store.cancel(); assert.equal(store.getState().phase, "unset");
});
test("Loading, load error, Retry and absent adapter never imply saved OFF", async () => {
  const store = createPushPreferenceStore(createMockPushPreferenceService("load-error", 0)); assert.match(view(store.getState()), /불러오는 중/);
  await store.load(); assert.equal(store.getState().phase, "load-error"); assert.match(view(store.getState()), /다시 조회/);
  await store.load(); assert.equal(store.getState().saved, true);
  const missing = createPushPreferenceStore(null); await missing.load(); assert.equal(missing.getState().phase, "load-error"); assert.equal(missing.getState().saved, null);
});
test("pending disables switch/duplicates; cancel aborts and restores saved value", async () => {
  const store = createPushPreferenceStore(createMockPushPreferenceService("pending", 0)); await store.load();
  const pending = store.save(false); assert.equal(store.getState().phase, "pending"); assert.equal(store.getState().draft, false);
  const html = view(store.getState()); assert.match(html, /disabled/); assert.match(html, /저장 중/); assert.match(html, /취소/);
  assert.equal(await store.save(true), false); store.cancel(); assert.equal(await pending, false); assert.equal(store.getState().saved, true); assert.equal(store.getState().draft, true);
});
test("late result after cancel does not overwrite state or reenter Success", async () => {
  const mock = createMockPushPreferenceService("on", 0); let finish!: (value: boolean) => void;
  const store = createPushPreferenceStore({ ...mock, save: () => new Promise(resolve => { finish = resolve; }) }); await store.load();
  const pending = store.save(false); store.cancel(); finish(false); await pending;
  assert.equal(store.getState().saved, true); assert.equal(store.getState().phase, "ready");
});
test("account disposal suppresses late load and save results", async () => {
  const mock = createMockPushPreferenceService("on", 0); let load!: (value: boolean) => void;
  const store = createPushPreferenceStore({ ...mock, load: () => new Promise(resolve => { load = resolve; }) });
  const loading = store.load(); store.dispose(); load(true); await loading; assert.equal(store.getState().saved, null);
  let finish!: (value: boolean) => void;
  const oldAccount = createPushPreferenceStore({ ...mock, save: () => new Promise(resolve => { finish = resolve; }) }); await oldAccount.load();
  const saving = oldAccount.save(false); oldAccount.dispose(); finish(false); await saving; assert.equal(oldAccount.getState().saved, true);
  const newAccount = createPushPreferenceStore(createMockPushPreferenceService("off", 0)); await newAccount.load(); assert.equal(newAccount.getState().saved, false);
});
test("retry after cancel is a no-op; scroll and saved preference survive reentry", async () => {
  const store = createPushPreferenceStore(createMockPushPreferenceService("save-error", 0)); await store.load(); store.setScroll(130);
  await store.save(false); store.cancel(); assert.equal(await store.retry(), false); assert.equal(store.getState().scroll, 130); assert.equal(store.getState().saved, true);
});
test("OFF does not clear app notification/activity items or existing reads", async () => {
  const notifications = createNotificationStore(createMockNotificationService("success", 0)); await notifications.load(); await notifications.open("comment"); notifications.setTab("activity"); notifications.setScroll(42);
  const before = notifications.getState(); const preference = createPushPreferenceStore(createMockPushPreferenceService("on", 0)); await preference.load(); await preference.save(false);
  assert.deepEqual(notifications.getState(), before); assert.equal(preference.getState().saved, false);
});
test("invalid adapter values become explicit errors rather than defaults", async () => {
  const mock = createMockPushPreferenceService("on", 0);
  const invalidLoad = createPushPreferenceStore({ ...mock, load: async () => "on" } as unknown as PushPreferenceService); await invalidLoad.load(); assert.equal(invalidLoad.getState().phase, "load-error");
  const invalidSave = createPushPreferenceStore({ ...mock, save: async () => null } as unknown as PushPreferenceService); await invalidSave.load(); await invalidSave.save(false); assert.equal(invalidSave.getState().phase, "save-error"); assert.equal(invalidSave.getState().draft, true);
});
test("member-only logical L08 destination never creates URL or privileges", () => {
  assert.equal(resolveDestination({ id: "notificationSettings" }).status, "unresolved");
  assert.equal(evaluateGuard({ id: "notificationSettings" }, { status: "guest" }).status, "login-required");
  assert.equal(evaluateGuard({ id: "notificationSettings" }, { status: "signup-incomplete" }).status, "signup-required");
  const nav = createNavigationStore(); nav.beginAuthentication({ destination: { id: "notificationSettings" }, origin: { id: "settings" } });
  const intents: unknown[] = []; nav.completeAuthentication({ status: "member", capabilities: { status: "ready", grants: [] } }, "available", entry => intents.push(entry)); assert.equal(intents.length, 1); assert.equal(nav.getState().returnTo, null);
});
test("L08 switch and explanatory text match Figma, with no extra master switch", () => {
  const state: PushPreferenceState = { saved: true, draft: true, scroll: 0, phase: "ready" }; const html = view(state);
  assert.equal((html.match(/role="switch"/g) ?? []).length, 1); assert.match(html, /OFF여도 서비스 내 알림과 활동 기록은 유지됩니다/); assert.match(html, /자동으로 다시 보내지 않습니다/);
  assert.doesNotMatch(html, /다크 모드|유형별|마스터/);
  const asset = readFileSync("public/icons/push-toggle-on.svg", "utf8"); assert.match(asset, /width="44"/); assert.match(asset, /height="26"/);
  assert.match(renderToStaticMarkup(<PushPreferenceToggle enabled={false} disabled={false} pending={false} onChange={noop}/>), /aria-checked="false"/);
});
