import type { InstitutionQualification, InstitutionService } from "./model";
import type { SessionState } from "../../lib/navigation/guard";
export const institutionScenarios=["valid","expired","none","unknown","other-region","loading","error-retry"] as const;
export type InstitutionScenario=typeof institutionScenarios[number];
export const mockSubjectId="mock-institution-member";
export const mockResponsibleRegion={id:"mock-wolgye1",name:"월계1동"};
export function mockQualification(scenario:InstitutionScenario):InstitutionQualification {
 if(scenario==="none"||scenario==="unknown")return {subjectId:mockSubjectId,status:scenario};
 return {subjectId:mockSubjectId,status:scenario==="expired" ? "expired" : "completed",institution:{id:"mock-office",name:"시연용 기관"},responsibleRegions:[mockResponsibleRegion],completedAt:scenario==="expired" ? "2024-01-01T00:00:00Z" : "2026-01-01T00:00:00Z",validUntil:scenario==="expired" ? "2025-01-01T00:00:00Z" : "2027-01-01T00:00:00Z"};
}
/** Development fixtures only; never registered as product permissions. */
export function mockInstitutionSession(scenario:InstitutionScenario):SessionState {
 const allowed=!(["expired","none","unknown"].includes(scenario));
 return {status:"member",capabilities:{status:"ready",grants:[{capability:{kind:"institution"},allowed},{capability:{kind:"responsible-region",regionId:mockResponsibleRegion.id},allowed},{capability:{kind:"responsible-region",regionId:"mock-hagye2"},allowed:false},{capability:{kind:"neighbor-region",regionId:mockResponsibleRegion.id},allowed:false}]}};
}
export function createMockInstitutionService(scenario:InstitutionScenario):InstitutionService {
 let attempts=0;return {async getQualification(signal){attempts++;await new Promise<void>((resolve,reject)=>{if(signal.aborted){reject(new Error("Aborted"));return;}const abort=()=>{clearTimeout(timer);reject(new Error("Aborted"));};const timer=setTimeout(()=>{signal.removeEventListener("abort",abort);resolve();},scenario==="loading" ? 60000 : 150);signal.addEventListener("abort",abort,{once:true});});if(scenario==="error-retry"&&attempts===1)throw new Error("Mock query error");return mockQualification(scenario);}};
}
