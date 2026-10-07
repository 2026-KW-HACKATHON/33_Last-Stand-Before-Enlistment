import assert from "node:assert/strict";
import { test } from "node:test";
import { Children, isValidElement, type ReactElement, type ReactNode } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { BottomNavigation, type NavigationItem } from "../layout/BottomNavigation";
import { Header, HeaderAction } from "../layout/Header";
import { MobileLayout } from "../layout/MobileLayout";

const ids: NavigationItem[] = ["main", "map", "write", "notification", "my"];
const labels = ["메인", "지도", "글쓰기", "알림", "마이"];
const text = (node: ReactNode) => renderToStaticMarkup(node).replace(/<[^>]*>/g, "");
const html = (node: ReactNode) => renderToStaticMarkup(node);

function children(node: ReactElement): ReactElement<Record<string, unknown>>[] {
  return Children.toArray((node.props as { children?: ReactNode }).children)
    .filter(isValidElement<Record<string, unknown>>);
}

function invoke(node: ReactElement<Record<string, unknown>>) {
  const callback = node.props.onClick;
  assert.equal(typeof callback, "function");
  (callback as () => void)();
}

test("five navigation items keep the product order, accessible names and callbacks", () => {
  const calls: NavigationItem[] = [];
  const nav = BottomNavigation({ onNavigate: (id) => calls.push(id) });
  const controls = children(nav);
  assert.deepEqual(controls.map(text), labels);
  for (const control of controls) {
    assert.equal(control.type, "button");
    assert.equal(control.props.type, "button");
    assert.equal(control.props.disabled, false);
    invoke(control);
  }
  assert.deepEqual(calls, ids);
  assert.equal(nav.props["aria-label"], "하단 메뉴");
});

test("every menu can be active without changing the five-item order", () => {
  for (const id of ids) {
    const controls = children(BottomNavigation({ activeItem: id, onNavigate() {} }));
    const selected = controls.filter((node) => node.props["aria-current"] === "page");
    assert.equal(selected.length, 1);
    assert.equal(text(selected[0]), labels[ids.indexOf(id)]);
    assert.match(String(selected[0].props.className), /text-primary/);
    for (const node of controls.filter((node) => node !== selected[0])) {
      assert.equal(node.props["aria-current"], undefined);
      assert.match(String(node.props.className), /text-secondary/);
    }
  }
});

test("injected destinations render five real anchors without invoking navigation callbacks", () => {
  const destinations = Object.fromEntries(ids.map((id) => [id, `#test-${id}`]));
  const markup = html(<BottomNavigation destinations={destinations} onNavigate={() => assert.fail("Unexpected callback")} />);
  assert.equal((markup.match(/<a\b/g) ?? []).length, 5);
  assert.equal((markup.match(/<button\b/g) ?? []).length, 0);
  for (const id of ids) assert.ok(markup.includes(`href="#test-${id}"`));
});

test("missing destination and callback disables each menu; partial destinations stay partial", () => {
  assert.ok(children(BottomNavigation({})).every((node) => node.type === "button" && node.props.disabled === true));
  const markup = html(<BottomNavigation destinations={{ notification: "#test-notification" }} />);
  assert.equal((markup.match(/<a\b/g) ?? []).length, 1);
  assert.equal((markup.match(/disabled=""/g) ?? []).length, 4);
  assert.ok(markup.includes('href="#test-notification"'));
});

test("write keeps the original 40px asset and a visually hidden accessible name", () => {
  const write = children(BottomNavigation({ activeItem: "write", onNavigate() {} }))[2];
  const markup = html(write);
  assert.match(markup, /src="\/icons\/write.svg"/);
  assert.match(markup, /width="40" height="40"/);
  assert.match(markup, /class="sr-only">글쓰기/);
  assert.match(markup, /ring-2 ring-primary/);
});

test("default Header preserves title, back callback and caller-supplied right action", () => {
  let back = 0;
  let action = 0;
  const header = Header({ title: "마이페이지", onBack: () => back++, rightAction: <button onClick={() => action++}>테스트 액션</button> });
  const controls = children(header);
  const backButton = controls.find((node) => node.props["aria-label"] === "뒤로가기");
  assert.ok(backButton);
  invoke(backButton);
  const rightSlot = controls.at(-1);
  assert.ok(rightSlot);
  invoke(children(rightSlot)[0]);
  assert.equal(back, 1);
  assert.equal(action, 1);
  assert.match(text(header), /마이페이지/);
  const plain = html(<Header title="제목" />);
  assert.match(plain, /<h1[^>]*>제목<\/h1>/);
  assert.doesNotMatch(plain, /<button|<a\b/);
});

test("brand Header composes Figma branding and independently supplied settings/notification actions", () => {
  const markup = html(<Header variant="brand" rightAction={<>
    <HeaderAction action="settings" variant="main" destination="#test-settings" />
    <HeaderAction action="notification" variant="main" destination="#test-notification" />
  </>} />);
  assert.match(markup, /src="\/icons\/brand.png"/);
  assert.match(markup, /width="35" height="32"/);
  assert.match(markup.replace(/<[^>]*>/g, ""), /Discushion우리의 이야기가, 더 나은 동네를 만듭니다\./);
  const anchors = markup.match(/<a\b[^>]*>/g) ?? [];
  assert.ok(anchors.some((tag) => tag.includes('href="#test-settings"') && tag.includes('aria-label="설정"')));
  assert.ok(anchors.some((tag) => tag.includes('href="#test-notification"') && tag.includes('aria-label="알림"')));
  assert.doesNotMatch(markup, /aria-label="뒤로가기"/);
});

test("Header actions expose named callbacks, icon slots and disabled fallbacks", () => {
  for (const action of ["settings", "notification"] as const) {
    let calls = 0;
    const node = HeaderAction({ action, onAction: () => calls++ });
    invoke(node);
    assert.equal(calls, 1);
    assert.equal(node.props["aria-label"], action === "settings" ? "설정" : "알림");
    assert.match(node.props.className, /w-14/);
    assert.match(html(node), /width="32" height="32"/);
    assert.match(HeaderAction({ action, variant: "main" }).props.className, /w-8/);
    assert.equal(HeaderAction({ action }).props.disabled, true);
    const disabled = HeaderAction({ action, destination: "#test-target", disabled: true });
    assert.equal(disabled.type, "button");
    assert.equal(disabled.props.disabled, true);
  }
});

test("Layout keeps optional slots, one main, scrolling and safe-area ownership", () => {
  const guest = html(<MobileLayout><p>게스트 검증 내용</p></MobileLayout>);
  assert.doesNotMatch(guest, /<nav\b|<header\b/);
  assert.equal((guest.match(/<main\b/g) ?? []).length, 1);
  assert.match(guest, /safe-area-inset-top/);
  assert.match(guest, /safe-area-inset-bottom/);
  for (const header of [undefined, <Header key="header" title="테스트" />]) {
    for (const nav of [undefined, <BottomNavigation key="nav" />]) {
      const markup = html(<MobileLayout header={header} bottomNavigation={nav}>검증 내용</MobileLayout>);
      assert.equal((markup.match(/<header\b/g) ?? []).length, header ? 1 : 0);
      assert.equal((markup.match(/<nav\b/g) ?? []).length, nav ? 1 : 0);
      assert.equal((markup.match(/<main\b/g) ?? []).length, 1);
      assert.match(markup, /h-dvh.*max-w-mobile/);
      assert.match(markup, /min-h-0.*overflow-y-auto/);
      assert.match(markup, /safe-area-inset-top/);
      assert.match(markup, /safe-area-inset-bottom/);
    }
  }
});
