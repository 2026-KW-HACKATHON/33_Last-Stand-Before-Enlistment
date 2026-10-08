import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { NavigationProvider, useSession, type SessionState } from "../../lib/navigation";
import { LogoutActionView } from "./LogoutAction";
import { LogoutSessionProvider } from "./provider";
import { SettingsScreen } from "../settings/SettingsScreen";
const noop = () => {};
const member: SessionState = { status: "member", capabilities: { status: "ready", grants: [] } };
test("pending row is disabled and announces current-device processing", () => { const html = renderToStaticMarkup(<LogoutActionView state={{ phase: "pending" }} source="mock" disabled={false} onRun={noop} onCancel={noop}/>); assert.match(html, /disabled=""/); assert.match(html, /aria-busy="true"/); assert.match(html, /현재 기기 로그아웃 중/); assert.match(html, /개발 Mock/); });
test("failure offers Retry/cancel and explicitly preserves logged-in state", () => { const html = renderToStaticMarkup(<LogoutActionView state={{ phase: "error" }} source="provider" disabled={false} onRun={noop} onCancel={noop}/>); assert.match(html, /다시 시도/); assert.match(html, /취소/); assert.match(html, /기존 로그인 상태/); assert.doesNotMatch(html, /개발 Mock/); });
test("L01 keeps existing menu structure and connects logout with a missing-adapter boundary", () => {
  const html = renderToStaticMarkup(<LogoutSessionProvider session={member} subjectKey="member-a"><NavigationProvider currentDestination={{ id: "me" }} onNavigate={noop}><SettingsScreen visible onBack={noop} onReturn={noop} getScroll={() => 0} onScroll={noop}/></NavigationProvider></LogoutSessionProvider>);
  for (const caption of ["계정 관리", "내가 쓴 글", "댓글 남긴 글", "알림 설정", "로그아웃", "회원 탈퇴"]) assert.match(html, new RegExp(caption));
});
for (const status of ["guest", "loading", "error", "signup-incomplete"] as const) test(`L01 ${status} cannot expose logout controls`, () => {
  const html = renderToStaticMarkup(<LogoutSessionProvider session={{ status }} subjectKey="member-a"><NavigationProvider currentDestination={{ id: "me" }} onNavigate={noop}><SettingsScreen visible onBack={noop} onReturn={noop} getScroll={() => 0} onScroll={noop}/></NavigationProvider></LogoutSessionProvider>);
  assert.doesNotMatch(html, /로그아웃/); assert.match(html, /인증 상태|로그인과 가입/);
});
test("SessionProvider retains upstream status before any confirmed result", () => { let seen: SessionState | null = null; function Capture() { seen = useSession().session; return null; } renderToStaticMarkup(<LogoutSessionProvider session={member} subjectKey="member-a"><Capture/></LogoutSessionProvider>); assert.equal(seen, member); });
