"use client";

import { useState } from "react";
import { BottomNavigation, type NavigationItem } from "../../../components/layout/BottomNavigation";
import { Header, HeaderAction } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Button } from "../../../components/ui/Button";
import { Input } from "../../../components/ui/Input";
import { TextArea } from "../../../components/ui/TextArea";
import { Notice } from "../../../components/ui/Notice";

const labels: Record<NavigationItem, string> = {
  main: "메인", map: "지도", write: "글쓰기", notification: "알림", my: "마이",
};

export function UiPreview() {
  const [activeItem, setActiveItem] = useState<NavigationItem>("main");
  const [headerVariant, setHeaderVariant] = useState("brand");
  const [navigationMode, setNavigationMode] = useState("enabled");
  const [disabled, setDisabled] = useState(false);
  const [message, setMessage] = useState("버튼과 메뉴를 눌러 동작을 확인하세요.");

  const actions = (
    <>
      <HeaderAction action="settings" variant={headerVariant === "brand" ? "main" : "default"}
        disabled={disabled} onAction={() => setMessage("설정 action 클릭")} />
      {headerVariant === "brand" && (
        <HeaderAction action="notification" variant="main" disabled={disabled}
          onAction={() => { setActiveItem("notification"); setMessage("Header 알림 action 클릭"); }} />
      )}
    </>
  );

  return (
    <div className="min-h-dvh bg-background px-4 py-6 text-body text-text">
      <div className="mx-auto flex max-w-5xl flex-col items-center gap-6 lg:flex-row lg:items-start lg:justify-center">
        <aside className="flex w-full max-w-mobile flex-col gap-section rounded-input border border-border bg-surface p-6 lg:w-72">
          <h1 className="text-xl font-bold">공통 UI 미리보기</h1>
          <p className="text-caption text-secondary">개발 전용 임시 페이지 · 396 × 852 기준</p>
          <label className="flex flex-col gap-internal">
            Header
            <select value={headerVariant} onChange={(event) => setHeaderVariant(event.target.value)}
              className="rounded-input border border-border bg-surface p-2">
              <option value="brand">메인 브랜드 Header</option>
              <option value="back">뒤로가기 + 설정</option>
              <option value="plain">제목만 표시</option>
            </select>
          </label>
          <label className="flex flex-col gap-internal">
            하단 메뉴
            <select value={navigationMode} onChange={(event) => setNavigationMode(event.target.value)}
              className="rounded-input border border-border bg-surface p-2">
              <option value="enabled">5개 메뉴 선택 가능</option>
              <option value="disabled">callback 없음 · 비활성</option>
              <option value="hidden">하단 메뉴 숨김</option>
            </select>
          </label>
          <label className="flex items-center gap-internal">
            <input type="checkbox" checked={disabled} onChange={(event) => setDisabled(event.target.checked)} />
            입력 / 버튼 / Header action 비활성
          </label>
          <Notice><span role="status" aria-live="polite">{message}</span></Notice>
          <p className="text-caption text-secondary">메뉴 선택과 action은 이 페이지에서만 반영됩니다. 실제 기능 페이지나 API로 이동하지 않습니다.</p>
        </aside>

        <div className="w-full max-w-mobile overflow-hidden rounded-input border border-border shadow-navigation">
          <MobileLayout style={{ height: "min(852px, 100dvh)" }}
            header={headerVariant === "brand"
              ? <Header variant="brand" rightAction={actions} />
              : <Header title={`${labels[activeItem]} 미리보기`}
                  onBack={headerVariant === "back" ? () => { setActiveItem("main"); setMessage("뒤로가기 action 클릭"); } : undefined}
                  rightAction={headerVariant === "back" ? actions : undefined} />}
            bottomNavigation={navigationMode === "hidden" ? undefined : (
              <BottomNavigation activeItem={activeItem}
                onNavigate={navigationMode === "enabled" ? (item) => {
                  setActiveItem(item); setMessage(`하단 메뉴 선택: ${labels[item]}`);
                } : undefined} />
            )}>
            <section className="flex flex-col gap-section">
              <h2 className="text-xl font-bold">{labels[activeItem]}</h2>
              <Notice>공통 컴포넌트의 모양과 상태를 확인하는 샘플입니다.</Notice>
              <Input label="제목" placeholder="제목을 입력하세요" disabled={disabled} />
              <TextArea label="내용" placeholder="동네 이야기를 입력하세요" disabled={disabled} />
              <Button disabled={disabled} onClick={() => setMessage("Primary 버튼 클릭")}>기본 버튼</Button>
              <Button variant="secondary" disabled={disabled} onClick={() => setMessage("Secondary 버튼 클릭")}>보조 버튼</Button>
              <Input label="오류 상태" defaultValue="입력 내용을 확인하세요" aria-invalid="true" disabled={disabled} />
              <Notice tone="warning">안내가 필요한 상태의 메시지입니다.</Notice>
              <Notice tone="error">오류가 발생한 상태의 메시지입니다.</Notice>
            </section>
            <section className="flex flex-col gap-section" aria-label="스크롤 확인용 콘텐츠">
              {[1, 2, 3, 4].map((number) => (
                <div key={number} className="rounded-input border border-border bg-surface p-4">
                  <h3 className="font-bold">스크롤 확인 {number}</h3>
                  <p className="mt-2 text-secondary">본문을 스크롤하면서 Header와 하단 메뉴가 고정되어 있는지 확인하세요.</p>
                </div>
              ))}
            </section>
          </MobileLayout>
        </div>
      </div>
    </div>
  );
}
