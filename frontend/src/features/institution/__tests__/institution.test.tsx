import test from "node:test";
import assert from "node:assert/strict";
import {renderToStaticMarkup} from "react-dom/server";
import {createInstitutionStore,institutionAccess,isCurrentInstitution,validateQualification,withInstitutionBadge,type InstitutionQualification,type InstitutionState} from "../model";
import {InstitutionBadge,InstitutionStatusView} from "../InstitutionStatus";
import {mockQualification,mockSubjectId as subject,mockResponsibleRegion as region,mockInstitutionSession,createMockInstitutionService} from "../mock";
import {evaluateGuard,type SessionState} from "../../../lib/navigation/guard";
const clock=Date.parse("2026-10-08T00:00:00Z");
const ready=(qualification=mockQualification("valid"),checkedAt=clock):InstitutionState=>({status:"ready",qualification,checkedAt});
const member=mockInstitutionSession("valid");
test("only a current completed qualification derives a badge, including exact time boundaries",()=>{
 const q=mockQualification("valid");assert.ok(isCurrentInstitution(ready(q),subject));assert.equal(isCurrentInstitution(ready(q),"other-author"),false);
 if(!("validUntil" in q))assert.fail();
 assert.equal(isCurrentInstitution(ready(q,Date.parse(q.completedAt)-1),subject),false);
 assert.equal(isCurrentInstitution(ready(q,Date.parse(q.completedAt)),subject),true);
 assert.equal(isCurrentInstitution(ready(q,Date.parse(q.validUntil)),subject),false);
 for(const status of ["expired","none","unknown"] as const)assert.equal(isCurrentInstitution(ready(mockQualification(status)),subject),false);
 for(const status of ["idle","loading","error"] as const)assert.equal(isCurrentInstitution({status},subject),false);
});
test("institution access and target region need separate scoped capabilities",()=>{
 assert.equal(institutionAccess(ready(),subject,member).status,"allowed");
 assert.equal(institutionAccess(ready(),subject,member,region.id).status,"allowed");
 assert.equal(institutionAccess(ready(),subject,member,"mock-hagye2").status,"forbidden");
 const outside:SessionState={status:"member",capabilities:{status:"ready",grants:[{capability:{kind:"institution"},allowed:true},{capability:{kind:"responsible-region",regionId:"outside"},allowed:true}]}};
 assert.equal(institutionAccess(ready(),subject,outside,"outside").status,"forbidden");
 const missing:SessionState={status:"member",capabilities:{status:"ready",grants:[]}};
 assert.equal(institutionAccess(ready(),subject,missing).status,"error");
 assert.equal(institutionAccess(ready(),subject,member,"missing").status,"error");
});
test("expired/unknown/guest/signup and unavailable states cannot enter institution work",()=>{
 for(const scenario of ["expired","none","unknown"] as const)assert.notEqual(institutionAccess(ready(mockQualification(scenario)),subject,member).status,"allowed");
 for(const status of ["guest","signup-incomplete","loading","error"] as const)assert.notEqual(institutionAccess(ready(),subject,{status}).status,"allowed");
 assert.equal(institutionAccess({status:"loading"},subject,member).status,"loading");assert.equal(institutionAccess({status:"error"},subject,member).status,"error");
});
test("institution does not grant neighbor participation or remove ordinary member access",()=>{
 assert.equal(evaluateGuard({id:"newPost"},member,{regionId:region.id}).status,"forbidden");
 const expired=mockInstitutionSession("expired");assert.equal(evaluateGuard({id:"home"},expired).status,"allowed");assert.equal(evaluateGuard({id:"profile"},expired).status,"allowed");
});
test("author projection removes stale badges without changing names/content/other authors",()=>{
 const author={id:subject,displayName:"작성자",badge:"institution" as const,extra:"원문 유지"};
 assert.equal(withInstitutionBadge(author,ready()).badge,"institution");
 const projected=withInstitutionBadge(author,ready(mockQualification("expired")));assert.equal(projected.badge,undefined);assert.equal(projected.extra,"원문 유지");assert.equal(author.badge,"institution");
 assert.equal(withInstitutionBadge({...author,id:"another-author"},ready()).badge,undefined);
 assert.equal(withInstitutionBadge(author,{status:"error"}).badge,undefined);
});
test("adapter data rejects wrong identity, missing institution, invalid dates and duplicate regions",()=>{
 const q=mockQualification("valid");assert.deepEqual(validateQualification(q,subject),q);
 assert.throws(()=>validateQualification(q,"other"));
 if(!("validUntil" in q))assert.fail();
 for(const invalid of [{...q,validUntil:"invalid"},{...q,validUntil:q.completedAt},{...q,institution:{id:"",name:"x"}},{...q,responsibleRegions:[]},{...q,responsibleRegions:[region,region]}])assert.throws(()=>validateQualification(invalid,subject));
});
test("query error→explicit retry succeeds; pending hides prior qualification",async()=>{
 const store=createInstitutionStore(createMockInstitutionService("error-retry"),subject,()=>clock);
 await store.load();assert.equal(store.getState().status,"error");const retry=store.load();assert.deepEqual(store.getState(),{status:"loading"});await retry;assert.ok(isCurrentInstitution(store.getState(),subject));store.dispose();
});
test("clock refresh removes expired badge and work without deleting the qualification",async()=>{
 let now=clock;const q=mockQualification("valid");const store=createInstitutionStore({async getQualification(){return q;}},subject,()=>now);await store.load();assert.ok(isCurrentInstitution(store.getState(),subject));
 if(!("validUntil" in q))assert.fail();now=Date.parse(q.validUntil);store.refreshValidity();assert.equal(isCurrentInstitution(store.getState(),subject),false);assert.equal(institutionAccess(store.getState(),subject,member).status,"forbidden");assert.equal(store.getState().status,"ready");store.dispose();
});
test("clear/dispose aborts account query and ignores late completion",async()=>{
 let resolve!:(q:InstitutionQualification)=>void;let signal!:AbortSignal;const store=createInstitutionStore({getQualification(s){signal=s;return new Promise(r=>{resolve=r;});}},subject,()=>clock);
 const pending=store.load();store.clear();assert.equal(signal.aborted,true);resolve(mockQualification("valid"));await pending;assert.deepEqual(store.getState(),{status:"idle"});store.dispose();
});
test("newest query wins, invalid subject and absent adapter fail closed",async()=>{
 const resolvers:((q:InstitutionQualification)=>void)[]=[];const store=createInstitutionStore({getQualification(){return new Promise(r=>resolvers.push(r));}},subject,()=>clock);const first=store.load(),second=store.load();resolvers[1](mockQualification("none"));await second;resolvers[0](mockQualification("valid"));await first;assert.equal(isCurrentInstitution(store.getState(),subject),false);store.dispose();
 const wrong=createInstitutionStore({async getQualification(){return {...mockQualification("valid"),subjectId:"wrong"};}},subject);await wrong.load();assert.equal(wrong.getState().status,"error");wrong.dispose();const missing=createInstitutionStore(null,subject);await missing.load();assert.equal(missing.getState().status,"error");missing.dispose();
});
const render=(state:InstitutionState,session=member,target=region.id)=>renderToStaticMarkup(<InstitutionStatusView state={state} session={session} subjectId={subject} targetRegionId={target} onBack={()=>{}} onRetry={()=>{}} onEnterWork={()=>{}}/>);
test("valid UI has institution/regions/dates and disabled other-region work",()=>{
 const html=render(ready());assert.match(html,/시연용 기관/);assert.match(html,/월계1동/);assert.match(html,/2027-01-01/);assert.match(html,/aria-label="현재 유효한 기관 인증"/);assert.match(html,/기관 업무로 이동/);
 const restricted=render(ready(),member,"mock-hagye2");assert.match(restricted,/담당 지역이 아닙니다/);assert.match(restricted,/disabled=""/);
});
test("unknown/error/loading/expired views hide badge and never offer evidence/application",()=>{
 for(const state of [{status:"loading" as const},{status:"error" as const},ready(mockQualification("unknown")),ready(mockQualification("expired")),ready(mockQualification("none"))]){const html=render(state);assert.doesNotMatch(html,/aria-label="현재 유효한 기관 인증"/);assert.doesNotMatch(html,/<form|<input|신청하기|제출하기/);assert.match(html,/돌아가기/);}
 assert.match(render({status:"error"}),/다시 조회/);assert.match(render(ready(mockQualification("none"))),/기관 자격이 없습니다/);
 assert.doesNotMatch(render(ready(),{status:"guest"}),/시연용 기관/);
 assert.equal(renderToStaticMarkup(<InstitutionBadge state={ready()} subjectId="another-author"/>),"");
});

test("scheduled expiration removes current badge while the screen remains open",async()=>{
 const q=mockQualification("valid");if(!("validUntil" in q))assert.fail();const current=Date.now();const short={...q,completedAt:new Date(current-1000).toISOString(),validUntil:new Date(current+100).toISOString()};
 const store=createInstitutionStore({async getQualification(){return short;}},subject);
 try{await store.load();assert.ok(isCurrentInstitution(store.getState(),subject));await new Promise<void>((resolve,reject)=>{const timeout=setTimeout(()=>reject(new Error("Expiration did not refresh")),1000);const unsub=store.subscribe(()=>{if(!isCurrentInstitution(store.getState(),subject)){clearTimeout(timeout);unsub();resolve();}});});assert.equal(institutionAccess(store.getState(),subject,member).status,"forbidden");}
 finally{store.dispose();}
});
