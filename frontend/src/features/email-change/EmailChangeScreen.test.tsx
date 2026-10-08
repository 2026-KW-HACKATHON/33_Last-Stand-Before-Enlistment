import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { EmailChangeView } from "./EmailChangeScreen";
import type { EmailChangeState } from "./model";
const noop = () => {};
const base: EmailChangeState = { phase: "email", currentEmail: "neighbor@example.com", email: "", failure: null, renewed: false, confirmationReady: false };
const render = (patch: Partial<EmailChangeState> = {}, available = true) => renderToStaticMarkup(<EmailChangeView state={{ ...base, ...patch }} source="mock" available={available} onBack={noop} onCancel={noop} onRetry={noop} onEmail={noop} onBegin={noop} onConfirm={noop} onEdit={noop}/>);
test("input screen uses common UI and contains no password, OTP length or timer", () => { const html = render(); assert.match(html, /이메일 변경/); assert.match(html, /현재 이메일/); assert.match(html, /type="email"/); assert.match(html, /Privy/); assert.match(html, /개발 Mock/); assert.doesNotMatch(html, /type="password"|6자리|04:59|60초/); assert.match(html, /disabled=""/); });
test("invalid email is visible and cannot start verification", () => { const html = render({ email: "invalid" }); assert.match(html, /올바른 이메일/); assert.match(html, /disabled=""/); });
test("confirmation preserves email read only and expired state requires restart", () => { const html = render({ phase: "confirmation", email: "new@example.com", failure: "expired" }); assert.match(html, /readOnly=""/); assert.match(html, /만료/); assert.match(html, /다시 시작/); assert.match(html, /disabled=""/); });
test("Loading, empty, Error/Retry and adapter unavailable are explicit", () => { assert.match(render({ phase: "loading" }), /불러오는 중/); assert.match(render({ currentEmail: null }), /설정되지 않았/); assert.match(render({ phase: "load-error" }), /다시 조회/); assert.match(render({}, false), /연결 대기/); });
test("duplicate is an edit path without account recovery", () => { const html = render({ email: "other@example.com", failure: "duplicate" }); assert.match(html, /다른 이메일/); assert.doesNotMatch(html, /계정 복구/); });

import { EmailChangeScreen } from "./EmailChangeScreen";
import { SessionProvider } from "../../lib/navigation";
for (const status of ["guest", "signup-incomplete", "loading", "error"] as const) test(`${status} cannot render the email input or call the change adapter`, () => {
  const request = { entry: { destination: { id: "emailChange" as const } }, signal: new AbortController().signal, onChanged: noop, onCancel: noop, onFailure: noop };
  const html = renderToStaticMarkup(<SessionProvider session={{ status }}><EmailChangeScreen request={request} account={null} service={null} subjectKey="member-a"/></SessionProvider>);
  assert.doesNotMatch(html, /type="email"/); assert.match(html, /계정 관리로 돌아가기/);
});
test("provider confirmation and save pending states disable duplicate submit", () => {
  for (const phase of ["verifying", "changing"] as const) { const html = render({ phase, email: "new@example.com", confirmationReady: true }); assert.match(html, /disabled=""/); assert.match(html, /확인 중|처리 중/); }
});
