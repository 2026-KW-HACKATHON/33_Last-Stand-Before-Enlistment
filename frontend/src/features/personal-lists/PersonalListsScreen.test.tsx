import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { SessionProvider, NavigationProvider, type SessionState } from "../../lib/navigation";
import { PersonalListsScreen } from "./PersonalListsScreen";
import { createPersonalListsStore } from "./model";
import { createMockPersonalListsService, mockSubject } from "./mock";
async function render(kind: "myPosts" | "participations", scenario: "normal" | "empty" | "error", session: SessionState = { status: "member", capabilities: { status: "ready", grants: [] } }) { const store = createPersonalListsStore(createMockPersonalListsService(scenario).service, mockSubject); store.open(kind, "me"); await store.load(); return renderToStaticMarkup(<SessionProvider session={session}><NavigationProvider currentDestination={{ id: kind }} onNavigate={() => {}}><PersonalListsScreen store={store} subjectKey={mockSubject} onBack={() => {}}/></NavigationProvider></SessionProvider>); }
test("shared PostCard, only type filters, aggregated participation labels and five navigation items", async () => { const html = await render("participations", "normal"); for (const text of ["참여한 게시물", "지역 안건", "지역 활동 정보", "투표", "댓글 좋아요·싫어요", "댓글 작성", "내 선택:", "게시판으로"]) assert.ok(html.includes(text)); assert.equal((html.match(/aria-pressed=/g) ?? []).length, 4); assert.equal((html.match(/상세 열기/g) ?? []).length, 6); });
test("empty participation CTA and authored empty are distinct", async () => { assert.ok((await render("participations", "empty")).includes("아직 참여한 게시물이 없습니다")); assert.ok((await render("myPosts", "empty")).includes("작성한 게시물이 없습니다")); });
test("error state offers Retry without pretending to be empty", async () => { const html = await render("myPosts", "error"); assert.ok(html.includes("다시 시도")); assert.ok(!html.includes("작성한 게시물이 없습니다")); });
test("guest and incomplete session cannot render personal data", async () => { for (const session of [{ status: "guest" }, { status: "signup-incomplete" }] as SessionState[]) { const html = await render("myPosts", "normal", session); assert.ok(html.includes("로그인 / 가입")); assert.ok(!html.includes("상세 열기")); } });
