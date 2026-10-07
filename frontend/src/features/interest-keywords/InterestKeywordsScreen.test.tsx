import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { InterestKeywordsView, InterestKeywordsScreen } from "./InterestKeywordsScreen";
import { createKeywordStore, type KeywordState } from "./model";
import { SessionProvider } from "../../lib/navigation";
const noop = () => {};
const state: KeywordState = { saved: ["교통", "주거"], draft: ["교통", "주거"], phase: "ready", scroll: 0 };
const render = (patch: Partial<KeywordState> = {}) => renderToStaticMarkup(<InterestKeywordsView state={{ ...state, ...patch }} onToggle={noop} onSave={noop} onRetryLoad={noop} onCancel={noop}/>);
test("Figma L07 has seven accessible chips in a three-column layout and save, no free input or BottomNavigation", () => {
  const html = render(); assert.equal((html.match(/aria-pressed=/g) ?? []).length, 7); assert.equal((html.match(/aria-pressed="true"/g) ?? []).length, 2);
  for (const text of ["선택 2/4", "grid-cols-3", "추천에 사용할 키워드를 최대 4개 선택해 주세요.", "선택한 키워드는 추천에만 사용됩니다.", "저장"]) assert.ok(html.includes(text));
  assert.ok(!html.includes("<input")); assert.ok(!html.includes("<nav"));
});
test("four-selection disables only unselected chips; pending disables all mutations but permits cancel", () => {
  assert.equal((render({ draft: ["교통", "주거", "안전", "복지"] }).match(/disabled=""/g) ?? []).length, 3);
  const pending = render({ phase: "pending" }); assert.equal((pending.match(/disabled=""/g) ?? []).length, 8); assert.ok(pending.includes("취소"));
});
test("failure Frame shows restored selection and Retry; load-error and success remain distinct", () => {
  const html = render({ phase: "save-error" }); assert.ok(html.includes("기존 선택을 유지했습니다.")); assert.ok(html.includes("다시 시도"));
  assert.ok(render({ phase: "load-error" }).includes("다시 조회")); assert.ok(render({ phase: "success" }).includes("관심 키워드가 저장되었습니다."));
});
test("guest and incomplete accounts cannot view or edit member interest keywords", () => {
  for (const status of ["guest", "signup-incomplete"] as const) {
    const html = renderToStaticMarkup(<SessionProvider session={{ status }}><InterestKeywordsScreen store={createKeywordStore(null)} visible onReturn={noop}/></SessionProvider>);
    assert.ok(html.includes("로그인과 가입 완료가 필요합니다.")); assert.ok(!html.includes("aria-pressed"));
  }
});
