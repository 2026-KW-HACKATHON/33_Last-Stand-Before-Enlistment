"use client";
import { useState } from "react";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { BottomNavigation } from "../../../components/layout/BottomNavigation";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { SessionProvider } from "../../../lib/navigation";
import { InstitutionProvider } from "../../../features/institution/provider";
import { InstitutionStatus,InstitutionBadge } from "../../../features/institution/InstitutionStatus";
import { useInstitution } from "../../../features/institution/provider";
import { createMockInstitutionService,institutionScenarios,mockSubjectId,mockResponsibleRegion,mockInstitutionSession,type InstitutionScenario } from "../../../features/institution/mock";
function AuthorPreview(){const {state}=useInstitution();return <div className="flex items-center gap-internal rounded-card border border-border bg-surface p-page"><span>과거 작성자 표시 · 시연 담당자</span><InstitutionBadge state={state} subjectId={mockSubjectId}/></div>;}
function Harness({scenario,access}:{scenario:InstitutionScenario;access:string}) {
 const [service]=useState(()=>createMockInstitutionService(scenario));const [screen,setScreen]=useState("status");const [filter,setFilter]=useState("전체");
 const session=access==="guest" ? {status:"guest" as const} : access==="incomplete" ? {status:"signup-incomplete" as const} : mockInstitutionSession(scenario);
 return <SessionProvider session={session}><InstitutionProvider service={service} subjectKey={access==="member" ? mockSubjectId : null}>
 <MobileLayout header={<Header title="기관 자격 상태" onBack={()=>setScreen("origin")}/>} bottomNavigation={<BottomNavigation activeItem="my"/>}>
 {screen==="status" ? <><InstitutionStatus subjectId={mockSubjectId} targetRegionId={scenario==="other-region" ? "mock-hagye2" : mockResponsibleRegion.id} onBack={()=>setScreen("origin")} onEnterWork={()=>setScreen("work")} onAuthenticate={()=>setScreen("auth")}/><AuthorPreview/></>
 : <><Notice>{screen==="work" ? "Mock 업무 진입: 목록·채택 기능은 #50 범위이며 여기서 실행하지 않습니다." : screen==="auth" ? "Mock 인증 취소 복귀: 실제 OTP나 자동 행동은 실행하지 않습니다." : "원 화면으로 복귀했습니다."} 필터: {filter}</Notice><label>원 화면 필터<input value={filter} onChange={e=>setFilter(e.currentTarget.value)}/></label><Button variant="secondary" onClick={()=>setScreen("status")}>{screen==="auth" ? "취소하고 복귀" : "기관 상태 다시 보기"}</Button></>}
 </MobileLayout></InstitutionProvider></SessionProvider>;
}
export function InstitutionPreview(){const [scenario,setScenario]=useState<InstitutionScenario>("valid");const [access,setAccess]=useState("member");return <><div className="mx-auto flex max-w-mobile flex-col gap-internal bg-surface p-page"><Notice>#45 개발 전용 Mock · 실제 기관 자격·저장·인증이 아닙니다.</Notice><label>기관 조회 상태<select value={scenario} onChange={e=>setScenario(e.currentTarget.value as InstitutionScenario)}>{institutionScenarios.map(s=><option key={s}>{s}</option>)}</select></label><label>세션<select value={access} onChange={e=>setAccess(e.currentTarget.value)}>{["member","guest","incomplete"].map(s=><option key={s}>{s}</option>)}</select></label></div><Harness key={scenario+access} scenario={scenario} access={access}/></>;}
