"use client";
import { useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { SessionProvider, useSession, type SessionState } from "../../../lib/navigation";
import { InterestKeywordsHost, useOpenInterestKeywords } from "../../../features/interest-keywords/InterestKeywordsHost";
import { createMockKeywordService, keywordScenarios, type KeywordScenario } from "../../../features/interest-keywords/mock";
function Entry() {
  const open = useOpenInterestKeywords(); const { session } = useSession();
  return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page">{!open && <Notice>{session.status === "loading" ? "인증 상태 확인 중" : session.status === "error" ? "인증 상태 확인 실패" : "로그인과 가입 완료가 필요합니다."}</Notice>}<Button disabled={!open} onClick={() => open?.()}>설정 → 관심 키워드</Button></div>;
}
function Harness({ scenario, role }: { scenario: KeywordScenario; role: string }) {
  const [service] = useState(() => createMockKeywordService(scenario));
  const session: SessionState = role === "member" ? { status: "member", capabilities: { status: "ready", grants: [] } } : { status: role as "guest" | "signup-incomplete" | "loading" | "error" };
  return <SessionProvider session={session}><InterestKeywordsHost subjectKey="preview-member" service={service}><Entry/></InterestKeywordsHost></SessionProvider>;
}
export function KeywordPreview() {
  const [scenario, setScenario] = useState<KeywordScenario>("success"); const [role, setRole] = useState("member");
  return <><div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>#88 개발 Mock · 실 API 저장·추천 완료가 아닙니다. 실제 L01 연결은 /dev/settings-preview에서 확인합니다.</Notice><label>상태 <select value={scenario} onChange={event => setScenario(event.currentTarget.value as KeywordScenario)}>{keywordScenarios.map(value => <option key={value}>{value}</option>)}</select></label><label>세션 <select value={role} onChange={event => setRole(event.currentTarget.value)}>{["member", "guest", "signup-incomplete", "loading", "error"].map(value => <option key={value}>{value}</option>)}</select></label></div><Harness key={scenario + role} scenario={scenario} role={role}/></>;
}
