"use client";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useSession } from "../../lib/navigation";
import type { SessionState } from "../../lib/navigation/guard";
import { institutionAccess,isCurrentInstitution,type InstitutionState } from "./model";
import { useInstitution } from "./provider";
/** Text badge; current author state only, never a stored/manual boolean. */
export function InstitutionBadge({state,subjectId}:{state:InstitutionState;subjectId:string}) {
 if(!isCurrentInstitution(state,subjectId))return null;
 return <span className="text-caption text-blue-600" aria-label="현재 유효한 기관 인증">기관 인증</span>;
}
type Props={subjectId:string;targetRegionId?:string;onBack:()=>void;onEnterWork?:()=>void;onAuthenticate?:()=>void};
export function InstitutionStatus(props:Props){const {state,retry}=useInstitution();const {session,retry:retrySession}=useSession();return <InstitutionStatusView {...props} state={state} session={session} onRetry={()=>{void retrySession?.();void retry();}}/>;}
export function InstitutionStatusView({subjectId,targetRegionId,onBack,onEnterWork,onAuthenticate,state,session,onRetry}:Props&{state:InstitutionState;session:SessionState;onRetry:()=>void}) {
 const access=institutionAccess(state,subjectId,session);const scoped=institutionAccess(state,subjectId,session,targetRegionId);
 const known=session.status==="member"&&state.status==="ready"&&state.qualification.subjectId===subjectId&&access.status!=="loading"&&access.status!=="error";
 const value=known&&state.status==="ready" ? state.qualification : null;
 const active=value&&isCurrentInstitution(state,subjectId);const details=value&&"institution" in value ? value : null;
 const busy=access.status==="loading";
 return <section aria-label="기관 자격 상태" aria-busy={busy} className="flex flex-col gap-section">
  <h2 className="text-section-title">기관 자격 상태</h2>
  <div role="status" aria-live="polite"><Notice tone={access.status==="error" ? "error" : "info"}>
   {busy ? "기관 자격을 확인하고 있습니다." : access.status==="error" ? "기관 자격을 확인하지 못했습니다. 다시 조회해 주세요." : access.status==="login-required" ? "로그인 후 본인의 기관 자격을 확인해 주세요." : access.status==="signup-required" ? "가입 완료 후 본인의 기관 자격을 확인해 주세요." : value?.status==="none" ? "기관 자격이 없습니다. 일반 회원 기능은 계속 이용할 수 있습니다." : active ? "현재 유효한 기관 자격입니다." : "기관 자격이 만료되었거나 유효하지 않습니다. 기관 배지와 업무 접근이 제한됩니다."}
  </Notice></div>
  {details&&<div className="flex flex-col gap-internal rounded-card border border-border bg-surface px-page py-section">
   <div className="flex flex-wrap items-center gap-internal"><p className="text-body">{details.institution.name}</p>{active&&<InstitutionBadge state={state} subjectId={subjectId}/>}</div>
   <dl className="text-body"><dt className="text-secondary">담당 지역</dt><dd>{details.responsibleRegions.map(r=>r.name).join(" · ")}</dd><dt className="text-secondary">완료일</dt><dd><time dateTime={details.completedAt}>{details.completedAt.slice(0,10)}</time></dd><dt className="text-secondary">유효기간</dt><dd><time dateTime={details.validUntil}>{details.validUntil.slice(0,10)}</time>까지</dd></dl>
  </div>}
  {active&&access.status==="forbidden"&&<Notice>현재 계정의 기관 업무 권한이 없습니다. 실제 권한 확인이 필요합니다.</Notice>}
  {active&&targetRegionId&&scoped.status==="forbidden"&&<Notice>해당 지역은 기관 담당 지역이 아닙니다. 담당 지역 업무만 수행할 수 있습니다.</Notice>}
  {active&&scoped.status==="error"&&<Notice tone="error">업무 권한을 확인하지 못했습니다. 다시 조회해 주세요.</Notice>}
  {access.status==="error"||scoped.status==="error" ? <Button onClick={onRetry}>다시 조회</Button> : null}
  {active&&onEnterWork&&<Button disabled={scoped.status!=="allowed"} onClick={onEnterWork}>기관 업무로 이동</Button>}
  {(access.status==="login-required"||access.status==="signup-required")&&onAuthenticate&&<Button onClick={onAuthenticate}>{access.status==="login-required" ? "로그인" : "가입 계속하기"}</Button>}
  <p className="text-caption text-secondary">기관 자격은 주민 참여의 이웃 자격과 별개입니다. 파란 배지는 게시물의 사실성을 보증하지 않습니다. 증빙 신청·심사는 이번 MVP에서 제공하지 않습니다.</p>
  <Button variant="secondary" onClick={onBack}>돌아가기</Button>
 </section>;
}
