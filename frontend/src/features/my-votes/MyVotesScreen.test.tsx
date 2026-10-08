import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { SessionProvider, NavigationProvider, type SessionState } from "../../lib/navigation";
import { createMyVotesStore, type VoteFilter } from "./model";
import { createMyVotesMock, mockVoteSubject, type MyVotesScenario } from "./mock";
import { MyVotesScreen } from "./MyVotesScreen";
async function render(filter: VoteFilter = "ALL", scenario: MyVotesScenario = "normal", session: SessionState = { status: "member", capabilities: { status: "ready", grants: [] } }, hide = false) {
  const mock = createMyVotesMock(scenario);
  if (hide) { mock.hide("vote"); mock.hide("vote-ended"); }
  const store = createMyVotesStore(mock.service, mockVoteSubject);
  store.open(); store.filter(filter); await store.load();
  return renderToStaticMarkup(<SessionProvider session={session}><NavigationProvider currentDestination={{ id: "myVotes" }} onNavigate={() => {}}><MyVotesScreen store={store} subjectKey={mockVoteSubject} onBack={() => {}}/></NavigationProvider></SessionProvider>);
}
test("three status filters and actual choice/final result, not submission controls", async () => {
  const html = await render();
  assert.equal((html.match(/aria-pressed=/g) ?? []).length, 3);
  for (const text of ["참여한 투표", "진행 중", "종료", "내 선택: 토요일 오전", "최다: 평일 저녁", "최종 결과:", "접근 불가 안내"]) assert.ok(html.includes(text));
  assert.ok(!html.includes("투표 제출")); assert.ok(!html.includes("선택 변경"));
});
test("status conditions do not mix open and ended content", async () => {
  const open = await render("OPEN"); const closed = await render("CLOSED");
  assert.ok(open.includes("주말 주민 휴식 공간 운영 시간 투표")); assert.ok(!open.includes("주민 휴식 공간 운영 시간 결과"));
  assert.ok(closed.includes("주민 휴식 공간 운영 시간 결과")); assert.ok(!closed.includes("주말 주민 휴식 공간 운영 시간 투표"));
});
test("unavailable history retains metadata only and hides all titles/body/options/images/results", async () => {
  const html = await render("ALL", "normal", undefined, true);
  assert.ok(html.includes("개인 참여 기록 유지")); assert.ok(html.includes("참여 시각"));
  for (const text of ["주말 주민", "주민 휴식", "평일 저녁", "토요일 오전", "내 선택:", "최다:", "images.unsplash.com"]) assert.ok(!html.includes(text), text);
});
test("empty, failure/retry and choice retrieval waiting have distinct feedback", async () => {
  assert.ok((await render("OPEN", "empty")).includes("해당 조건의 참여한 투표가 없습니다"));
  const error = await render("CLOSED", "error"); assert.ok(error.includes("다시 시도")); assert.ok(!error.includes("참여한 투표가 없습니다"));
  const unknown = await render("ALL", "unknown"); assert.ok(unknown.includes("본인 선택과 결과 조회 대기")); assert.ok(!unknown.includes("내 선택:"));
});
test("guest and incomplete signup never see private history, choice or result", async () => {
  for (const session of [{ status: "guest" }, { status: "signup-incomplete" }] as SessionState[]) {
    const html = await render("ALL", "normal", session);
    assert.ok(html.includes("로그인 / 가입")); assert.ok(!html.includes("내 선택:")); assert.ok(!html.includes("참여 시각")); assert.ok(!html.includes("주말 주민"));
  }
});
test("Loading and missing adapter do not expose ready records", async () => {
  const store = createMyVotesStore(null, mockVoteSubject);
  store.open();
  const markup = () => renderToStaticMarkup(<SessionProvider session={{ status: "member", capabilities: { status: "ready", grants: [] } }}><NavigationProvider currentDestination={{ id: "myVotes" }} onNavigate={() => {}}><MyVotesScreen store={store} subjectKey={mockVoteSubject} onBack={() => {}}/></NavigationProvider></SessionProvider>);
  assert.ok(markup().includes("참여한 투표 불러오는 중"));
  await store.load();
  assert.ok(markup().includes("실 API 연동 대기"));
  assert.ok(!markup().includes("내 선택:"));
});
