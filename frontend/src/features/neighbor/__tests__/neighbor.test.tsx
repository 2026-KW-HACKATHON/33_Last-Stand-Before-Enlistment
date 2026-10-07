import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { createNeighborStore, neighborParticipation, validateRegions, type CompletedRegion, type NeighborState } from "../model";
import { NeighborStatusView } from "../NeighborStatus";
import { createMockNeighborService, mockTargetRegion as target, mockOtherRegion as other } from "../mock";
import type { SessionState } from "../../../lib/navigation/guard";
const member = (allowed: boolean): SessionState => ({ status: "member", capabilities: { status: "ready", grants: [{ capability: { kind: "neighbor-region", regionId: target.id }, allowed }] } });
const ready = (regions: readonly CompletedRegion[]): NeighborState => ({ status: "ready", regions });
test("only an explicit matching grant and completed region display allows participation", () => {
 assert.equal(neighborParticipation(ready([target]),member(true),target.id).status,"allowed");
 assert.equal(neighborParticipation(ready([other]),member(false),target.id).status,"forbidden");
 assert.equal(neighborParticipation(ready([]),member(false),target.id).status,"forbidden");
 assert.equal(neighborParticipation(ready([]),member(true),target.id).status,"error");
 assert.equal(neighborParticipation(ready([target]),member(false),target.id).status,"error");
});
test("login/signup/institution or unscoped permission cannot grant neighbor participation", () => {
 for (const status of ["guest","signup-incomplete","loading","error"] as const) assert.notEqual(neighborParticipation(ready([target]),{status},target.id).status,"allowed");
 const institution: SessionState = {status:"member",capabilities:{status:"ready",grants:[{capability:{kind:"institution"},allowed:true}]}};
 assert.equal(neighborParticipation(ready([target]),institution,target.id).status,"error");
 assert.equal(neighborParticipation(ready([target]),member(true),other.id).status,"error");
});
test("loading/error cannot reuse previously completed eligibility", () => {
 for (const status of ["idle","loading","error"] as const) assert.notEqual(neighborParticipation({status},member(true),target.id).status,"allowed");
});
test("max three, duplicate and malformed data are rejected without truncation", () => {
 assert.equal(validateRegions([target,other,{id:"third",name:"세 번째"}]).length,3);
 for(const regions of [[target,target],[target,other,{id:"third",name:"세 번째"},{id:"fourth",name:"네 번째"}],[{id:"",name:"빈 식별자"}],[{id:"x",name:""}]]) assert.throws(()=>validateRegions(regions));
});
test("retry recovers from query failure and hides cached regions while pending", async () => {
 const store=createNeighborStore(createMockNeighborService("error-retry"));
 await store.load(); assert.equal(store.getState().status,"error");
 const retry=store.load(); assert.deepEqual(store.getState(),{status:"loading"});
 await retry; assert.deepEqual(store.getState(),ready([target])); store.dispose();
});
test("clear/account disposal cancels requests and ignores late results", async () => {
 let resolve!: (r:readonly CompletedRegion[])=>void; let signal!:AbortSignal;
 const store=createNeighborStore({getCompletedRegions(s){signal=s;return new Promise(r=>{resolve=r;});}});
 const pending=store.load(); store.clear(); assert.equal(signal.aborted,true);
 resolve([target]); await pending; assert.deepEqual(store.getState(),{status:"idle"}); store.dispose();
});
test("latest requery wins and invalid or unavailable adapter fails closed", async () => {
 const resolutions: ((r:readonly CompletedRegion[])=>void)[]=[];
 const store=createNeighborStore({getCompletedRegions(){return new Promise(r=>resolutions.push(r));}});
 const old=store.load(); const current=store.load(); resolutions[1]([]);await current;resolutions[0]([target]);await old;assert.deepEqual(store.getState(),ready([]));store.dispose();
 const invalid=createNeighborStore({async getCompletedRegions(){return [target,target];}});await invalid.load();assert.equal(invalid.getState().status,"error");invalid.dispose();
 const missing=createNeighborStore(null);await missing.load();assert.equal(missing.getState().status,"error");missing.dispose();
});
const render = (state:NeighborState,session=member(false))=>renderToStaticMarkup(<NeighborStatusView state={state} session={session} targetRegion={target} onBack={()=>{}} onRetry={()=>{}} activityRegion={other} onReturnToActivity={()=>{}} />);
test("restricted/empty UI offers return paths without application controls", () => {
 const html=render(ready([])); assert.match(html,/완료된 이웃 지역이 없습니다/);assert.match(html,/0[/]3/);assert.match(html,/지역 참여를 할 수 없습니다/);assert.match(html,/기본 활동 지역 하계2동으로 돌아가기/);assert.doesNotMatch(html,/<input|<form|type="file"|신청하기|제출하기/);
});
test("loading/error/ready view never exposes stale qualification and has retry", () => {
 assert.match(render({status:"loading"}),/aria-busy="true"/);
 const error=render({status:"error"});assert.match(error,/다시 조회/);assert.doesNotMatch(error,/· 완료/);
 assert.match(render(ready([target]),member(true)),/지역 참여가 가능합니다/);
 assert.match(render(ready([target]),{status:"guest"}),/로그인 후/);
 assert.doesNotMatch(render(ready([target]),{status:"guest"}),/· 완료/);
});

test("capability requery hides completed badges while authority is unknown", () => {
 for (const status of ["loading","error"] as const) assert.doesNotMatch(render(ready([target]),{status:"member",capabilities:{status}}),/· 완료/);
});
