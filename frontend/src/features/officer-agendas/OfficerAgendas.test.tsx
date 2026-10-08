import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { NavigationProvider, SessionProvider } from "../../lib/navigation";
import { InstitutionProvider } from "../institution/provider";
import { createMockAdoptionService, mockOfficerAuthority } from "./mock";
import { createOfficerStore, type OfficerAuthority, type OfficerStore } from "./model";
import { OfficerAgendasScreen } from "./OfficerAgendas";

function render(store: OfficerStore, authority = mockOfficerAuthority(), postId?: string) {
  return renderToStaticMarkup(<SessionProvider session={authority.session}><NavigationProvider currentDestination={{ id: "officerAgendas" }} onNavigate={() => {}}><InstitutionProvider subjectKey={authority.subjectId}><OfficerAgendasScreen store={store} authority={authority} postId={postId} onOpen={() => {}} onBack={() => {}}/></InstitutionProvider></NavigationProvider></SessionProvider>);
}
async function ready() { const authority = mockOfficerAuthority(); const mock = createMockAdoptionService(() => authority, "normal", 0); const store = createOfficerStore(mock.service, () => authority); await store.load(); return { authority, mock, store }; }
test("all/adopted and region filters compose the existing PostCard", async () => {
  const { authority, store } = await ready(); let html = render(store);
  for (const text of ["기관 담당자", "지역 필터", "전체 공개 안건", "채택한 게시물", "반응 수 내림차순", "결과 2개"]) assert.ok(html.includes(text));
  store.filter("all", "mock-hagye2"); html = render(store); assert.ok(html.includes("결과 1개")); assert.ok(!html.includes(store.getState().items[0].post.title));
  store.filter("adopted", ""); assert.ok(render(store).includes("해당 조건의 안건이 없습니다"));
  await store.read("agenda-photo"); await store.change(true); assert.ok(render(store).includes("결과 1개"));
  assert.ok(render(store, authority).includes("채택한 게시물"));
});
test("review exposes read-only resident comments/replies, scoped controls and record", async () => {
  const { authority, store } = await ready(); await store.read("agenda-photo"); let html = render(store, authority, "agenda-photo");
  for (const text of ["주민 의견", "공감해요 20", "↳ 답글", "이 안건 채택", "기관명과 채택 시각만 공개"]) assert.ok(html.includes(text));
  assert.ok(!html.includes("댓글 작성")); assert.ok(!html.includes("mock-other-office")); assert.ok(!html.includes("other-0"));
  await store.change(true); html = render(store, authority, "agenda-photo"); assert.ok(html.includes("채택 기록")); assert.ok(html.includes("본 기관 채택 취소")); assert.ok(html.includes("다른 기관"));
  await store.change(false); assert.ok(render(store, authority, "agenda-photo").includes("이 안건 채택"));
  await store.read("agenda-anonymous"); html = render(store, authority, "agenda-anonymous"); assert.ok(html.includes("채택 불가 · 담당 지역 밖")); assert.ok(html.includes("disabled"));
});
test("loading/error/retry/empty/missing port never masquerade as success", async () => {
  const { mock, store } = await ready(); const pending = store.load(); assert.ok(render(store).includes("목록 불러오는 중")); await pending;
  mock.scenario("error"); await store.load(); assert.ok(render(store).includes("다시 시도")); assert.ok(!render(store).includes(store.getState().items[0].post.title));
  mock.scenario("empty"); await store.load(); assert.ok(render(store).includes("해당 조건의 안건이 없습니다"));
  mock.scenario("deleted"); await store.read("agenda-photo"); assert.ok(render(store, mockOfficerAuthority(), "agenda-photo").includes("삭제되었거나"));
  const missing = createOfficerStore(null, mockOfficerAuthority); await missing.load(); assert.ok(render(missing).includes("실 API 연동 대기"));
});
test("expired/guest/loading/error authority never leaks cached originals", async () => {
  const { store } = await ready(); const original = store.getState().items[0].post.title;
  const authorities: OfficerAuthority[] = [mockOfficerAuthority("expired"), { ...mockOfficerAuthority(), session: { status: "guest" } }, { ...mockOfficerAuthority(), institution: { status: "loading" } }, { ...mockOfficerAuthority(), institution: { status: "error" } }];
  for (const authority of authorities) assert.ok(!render(store, authority).includes(original));
  assert.ok(render(store, authorities[1]).includes("로그인 / 가입")); assert.ok(render(store, authorities[3]).includes("다시 확인"));
});
