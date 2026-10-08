import { evaluateGuard, type SessionState } from "../../lib/navigation/guard";
import type { CompletedRegion } from "../neighbor/model";
import type { AuthorDisplay } from "../post/model";
/** FE display port, not a wire DTO. subjectId is the adapter's stable FE identity. */
export type InstitutionQualification = { subjectId: string } & (
 | { status: "none" | "unknown" }
 | { status: "completed" | "expired"; institution: { id: string; name: string }; responsibleRegions: readonly CompletedRegion[]; completedAt: string; validUntil: string }
);
export interface InstitutionService { getQualification(signal: AbortSignal): Promise<InstitutionQualification> }
export type InstitutionState = { status: "idle" | "loading" | "error" } | { status: "ready"; qualification: InstitutionQualification; checkedAt: number };
export function validateQualification(value: InstitutionQualification, subjectId: string): InstitutionQualification {
 if (!value || value.subjectId !== subjectId || !subjectId.trim()) throw new Error("Wrong subject");
 if (value.status === "none" || value.status === "unknown") return { subjectId, status: value.status };
 if (value.status !== "completed" && value.status !== "expired") throw new Error("Unknown qualification");
 const completed = Date.parse(value.completedAt), until = Date.parse(value.validUntil);
 if (!value.institution?.id?.trim() || !value.institution.name?.trim() || !Number.isFinite(completed) || !Number.isFinite(until) || completed >= until || !Array.isArray(value.responsibleRegions) || !value.responsibleRegions.length || Array.from(value.responsibleRegions).some(r => !r?.id?.trim() || !r.name?.trim()) || new Set(value.responsibleRegions.map(r=>r.id)).size !== value.responsibleRegions.length) throw new Error("Invalid qualification");
 return { ...value, institution: { ...value.institution }, responsibleRegions: value.responsibleRegions.map(r=>({ ...r })) };
}
export function isCurrentInstitution(state: InstitutionState, subjectId: string): boolean {
 if (state.status !== "ready" || state.qualification.subjectId !== subjectId || state.qualification.status !== "completed") return false;
 const value=state.qualification;
 return Date.parse(value.completedAt) <= state.checkedAt && state.checkedAt < Date.parse(value.validUntil);
}
export function institutionAccess(state: InstitutionState, subjectId: string, session: SessionState, regionId?: string) {
 const guard = evaluateGuard({ id: "officerAgendas" }, session);
 if (guard.status !== "allowed" && guard.status !== "forbidden") return guard;
 if (state.status === "idle" || state.status === "loading") return { status: "loading" } as const;
 if (state.status !== "ready" || state.qualification.subjectId !== subjectId || state.qualification.status === "unknown") return { status: "error", retryable: true } as const;
 if (!isCurrentInstitution(state,subjectId)) return { status: "forbidden" } as const;
 if (guard.status !== "allowed") return guard;
 if (!regionId) return guard;
 const scoped = evaluateGuard({ id: "officerAgendas" },session,{requirements:[{kind:"responsible-region",regionId}]});
 if (scoped.status !== "allowed") return scoped;
 const qualification=state.qualification;
 return {status: "responsibleRegions" in qualification && qualification.responsibleRegions.some(r=>r.id===regionId) ? "allowed" : "forbidden"} as const;
}
/** Author-specific projection: never apply the viewer's institution status to another author. */
export function withInstitutionBadge<T extends AuthorDisplay>(author:T,state:InstitutionState):T {
 return {...author,badge:isCurrentInstitution(state,author.id) ? "institution" : undefined};
}
export function createInstitutionStore(service: InstitutionService | null, subjectId: string, now:()=>number=Date.now) {
 let state:InstitutionState={status:"idle"}; let generation=0; let controller:AbortController|null=null; let timer:ReturnType<typeof setTimeout>|null=null;
 const listeners=new Set<()=>void>();
 const publish=(next:InstitutionState)=>{state=next;listeners.forEach(fn=>fn());};
 const stopTimer=()=>{if(timer) clearTimeout(timer);timer=null;};
 function refreshValidity() {
  stopTimer(); if(state.status!=="ready") return;
  publish({...state,checkedAt:now()});
  const qualification=state.qualification;
  if(qualification.status!=="completed") return;
  const boundary=now()<Date.parse(qualification.completedAt) ? Date.parse(qualification.completedAt) : Date.parse(qualification.validUntil);
  const remaining=boundary-now();
  if(remaining>0) timer=setTimeout(refreshValidity,Math.min(remaining,2147483647));
 }
 const clear=()=>{generation++;controller?.abort();controller=null;stopTimer();publish({status:"idle"});};
 return {getState:()=>state,subscribe:(fn:()=>void)=>{listeners.add(fn);return()=>{listeners.delete(fn);};},clear,refreshValidity,
  async load(){const request=++generation;controller?.abort();stopTimer();controller=new AbortController();const signal=controller.signal;publish({status:"loading"});
   try {if(!service) throw new Error("Adapter unavailable");const qualification=validateQualification(await service.getQualification(signal),subjectId);if(request===generation&&!signal.aborted){publish({status:"ready",qualification,checkedAt:now()});refreshValidity();}}
   catch {if(request===generation&&!signal.aborted) publish({status:"error"});}
  },dispose(){clear();listeners.clear();},
 };
}
