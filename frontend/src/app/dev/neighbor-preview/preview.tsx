"use client";
import { useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { BottomNavigation } from "../../../components/layout/BottomNavigation";
import { SessionProvider } from "../../../lib/navigation";
import type { SessionState } from "../../../lib/navigation/guard";
import { NeighborProvider } from "../../../features/neighbor/provider";
import { NeighborStatus } from "../../../features/neighbor/NeighborStatus";
import { createMockNeighborService, mockCompletedRegions, mockTargetRegion, mockOtherRegion, neighborScenarios, type NeighborScenario } from "../../../features/neighbor/mock";
function Harness({ scenario, access }: { scenario: NeighborScenario; access: string }) {
 const [service] = useState(() => createMockNeighborService(scenario));
 const [target, setTarget] = useState(mockOtherRegion); const [open, setOpen] = useState(true);
 const [filter, setFilter] = useState("교통"); const [auth, setAuth] = useState(false);
 const completed = mockCompletedRegions(scenario);
 const session: SessionState = access === "guest" ? { status: "guest" } : access === "incomplete" ? { status: "signup-incomplete" } : { status: "member", capabilities: { status: "ready", grants: [mockTargetRegion,mockOtherRegion].map(region => ({ capability: { kind: "neighbor-region", regionId: region.id }, allowed: completed.some(r => r.id === region.id) })) } };
 const back = () => setOpen(false);
 return <SessionProvider session={session}><NeighborProvider service={service} subjectKey={access === "member" ? "dev-fixture-member" : null}>
 <MobileLayout contentClassName={open ? "!px-0 !pt-0" : ""} header={<Header title={open ? "지역 참여 안내" : "Mock 원 화면"} onBack={back} />} bottomNavigation={<BottomNavigation activeItem="main" />}>
 {auth ? <><Notice>Mock 인증 복귀 확인: 대상 {target.name}, 필터 {filter}를 보존합니다. 실제 OTP나 참여 행동은 실행하지 않습니다.</Notice><Button onClick={() => setAuth(false)}>취소하고 원 화면 복귀</Button></>
 : open ? <NeighborStatus targetRegion={target} activityRegion={mockTargetRegion} onBack={back} onReturnToActivity={() => { setTarget(mockTargetRegion); back(); }} onAuthenticate={() => setAuth(true)} />
 : <><Notice>원 화면 · {target.name} · 필터 {filter}</Notice><label>보존할 필터<input value={filter} onChange={e => setFilter(e.currentTarget.value)} /></label><Button onClick={() => setOpen(true)}>자격 안내 확인</Button><Button variant="secondary" onClick={() => setTarget(target.id === mockTargetRegion.id ? mockOtherRegion : mockTargetRegion)}>탐색 지역 전환</Button></>}
 </MobileLayout></NeighborProvider></SessionProvider>;
}
export function NeighborPreview() {
 const [scenario, setScenario] = useState<NeighborScenario>("completed"); const [access, setAccess] = useState("member");
 return <><div className="mx-auto flex max-w-mobile flex-col gap-internal bg-surface p-page"><Notice>#44 개발 전용 Mock. 실제 권한·인증·저장에 영향을 주지 않습니다.</Notice><label>조회 상태<select value={scenario} onChange={e => setScenario(e.currentTarget.value as NeighborScenario)}>{neighborScenarios.map(s => <option key={s}>{s}</option>)}</select></label><label>세션<select value={access} onChange={e => setAccess(e.currentTarget.value)}>{["member","guest","incomplete"].map(s => <option key={s}>{s}</option>)}</select></label></div><Harness key={scenario+access} scenario={scenario} access={access} /></>;
}
