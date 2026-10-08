import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { WithdrawalView } from "./WithdrawalScreen";
import { WithdrawalSessionProvider, useWithdrawal } from "./provider";
import { SettingsNavigation } from "../settings/SettingsNavigation";
import { SessionProvider, useNavigation, type SessionState } from "../../lib/navigation";
import type { WithdrawalState } from "./model";
const noop = () => {};
function view(phase: WithdrawalState["phase"], failure: WithdrawalState["failure"] = null) { return renderToStaticMarkup(<WithdrawalView state={{ phase, failure, ended: phase === "complete" }} onNext={noop} onVerify={noop} onWithdraw={noop} onBack={noop} onCancel={noop} onFinish={noop}/>); }
test("L09 preserves public/anonymous/aggregate policy and replaces password with Privy", () => {
  const html = view("caution"); for (const text of ["탈퇴 전 확인", "회원 탈퇴한 사용자", "익명", "북마크", "문의 이메일", "반응·평가·투표", "Privy 본인 확인", "취소"]) assert.match(html, new RegExp(text)); assert.doesNotMatch(html, /type="password"|비밀번호/);
});
test("L10 exposes Provider confirmation and failure Retry, no local OTP/password form", () => {
  const html = view("verification", "failed"); assert.match(html, /본인 확인 다시 시도/); assert.match(html, /기존 로그인 상태/); assert.match(html, /role="alert"/); assert.doesNotMatch(html, /<input|비밀번호/);
  for (const failure of ["expired", "cancelled", "unavailable"] as const) assert.match(view("verification", failure), /role="alert"/);
});
for (const phase of ["verifying", "withdrawing"] as const) test(`${phase} disables action, announces Pending and allows cancellation`, () => { const html = view(phase); assert.match(html, /disabled=""/); assert.match(html, /aria-busy="true"/); assert.match(html, /role="status"/); assert.match(html, /취소/); });
test("L11 final consent and L12 explicit A02, no cancellable completed withdrawal", () => { assert.match(view("final"), /정말 회원 탈퇴/); const html = view("complete"); assert.match(html, /회원 이용 종료/); assert.match(html, /로그인 화면으로 이동/); assert.doesNotMatch(html, /취소|aria-label="뒤로/); });
test("production logical withdrawal intent opens only member scope, cancellation keeps Navigation", () => {
  let navigation!: ReturnType<typeof useNavigation>, withdrawal!: NonNullable<ReturnType<typeof useWithdrawal>>;
  function Capture() { navigation = useNavigation(); withdrawal = useWithdrawal()!; return null; }
  renderToStaticMarkup(<SessionProvider session={{ status: "member", capabilities: { status: "ready", grants: [] } }}><WithdrawalSessionProvider subjectKey="a"><SettingsNavigation subjectKey="a" currentDestination={{ id: "me" }} onNavigate={noop}><Capture/></SettingsNavigation></WithdrawalSessionProvider></SessionProvider>);
  navigation.navigate({ destination: { id: "settings" }, origin: { id: "me" } }); const before = navigation.state;
  navigation.navigate({ destination: { id: "withdrawal" }, origin: { id: "settings" } }); assert.equal(withdrawal.store.getState().phase, "caution"); withdrawal.store.cancel(); assert.equal(withdrawal.store.getState().phase, "closed"); assert.equal(navigation.state, before);
});
for (const status of ["guest", "loading", "error", "signup-incomplete"] as const) test(`${status} logical intent cannot open protected withdrawal`, () => {
  let navigation!: ReturnType<typeof useNavigation>, withdrawal!: NonNullable<ReturnType<typeof useWithdrawal>>;
  function Capture() { navigation = useNavigation(); withdrawal = useWithdrawal()!; return null; }
  renderToStaticMarkup(<SessionProvider session={{ status } as SessionState}><WithdrawalSessionProvider subjectKey="a"><SettingsNavigation subjectKey="a" currentDestination={{ id: "me" }} onNavigate={noop}><Capture/></SettingsNavigation></WithdrawalSessionProvider></SessionProvider>);
  navigation.navigate({ destination: { id: "withdrawal" }, origin: { id: "settings" } }); assert.equal(withdrawal.store.getState().phase, "closed");
});
