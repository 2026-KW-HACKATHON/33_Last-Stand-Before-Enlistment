import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { InterestRegionsView, InterestRegionsScreen } from "./InterestRegionsScreen";
import { createInterestRegionStore, type InterestRegionState } from "./model";
import { interestRegionFixtures } from "./mock";
import { SessionProvider } from "../../lib/navigation";
const state: InterestRegionState = { saved: [interestRegionFixtures[0]], candidates: interestRegionFixtures, query: "", phase: "ready", searchPhase: "ready", mutation: "idle", pending: null, scroll: 0 };
const noop = () => {};
const render = (patch: Partial<InterestRegionState> = {}) => renderToStaticMarkup(<InterestRegionsView state={{ ...state, ...patch }} onQuery={noop} onToggle={noop} onLoad={noop} onSearch={noop} onRetry={noop} onBack={noop} onCancel={noop}/>);
test("Figma I08 labels/actions and no BottomNavigation or separate Save button", () => {
  const html = render();
  for (const text of ["관심 지역", "관심 지역 찾기", "추가·삭제는 즉시 반영됩니다. 0개도 가능합니다.", "월계1동 제거", "월계2동 추가", "하계1동 추가", "돌아가기"]) assert.ok(html.includes(text));
  assert.equal((html.match(/aria-pressed=/g) ?? []).length, 3);
  assert.ok(!html.includes("<nav")); assert.ok(!html.includes(">저장</button>"));
});
test("pending, empty, success, error and retry render their actual states", () => {
  const pending = render({ mutation: "pending", pending: interestRegionFixtures[1] });
  assert.equal((pending.match(/disabled=""/g) ?? []).length, 3); assert.ok(pending.includes("변경 취소"));
  assert.ok(render({ saved: [], candidates: [] }).includes("검색 결과가 없습니다."));
  assert.ok(render({ mutation: "error" }).includes("저장 다시 시도"));
  assert.ok(render({ mutation: "success" }).includes("관심 지역이 반영되었습니다."));
  assert.ok(render({ phase: "loading" }).includes("관심 지역 불러오는 중"));
});
test("guest/incomplete session never renders saved member list or mutation actions", () => {
  for (const status of ["guest", "signup-incomplete"] as const) {
    const html = renderToStaticMarkup(<SessionProvider session={{ status }}><InterestRegionsScreen store={createInterestRegionStore(null)} visible onBack={noop}/></SessionProvider>);
    assert.ok(html.includes("로그인과 가입 완료가 필요합니다.")); assert.ok(!html.includes("지역 검색"));
  }
});
