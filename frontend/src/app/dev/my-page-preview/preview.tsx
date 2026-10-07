"use client";
import {useState} from "react";
import {Button} from "../../../components/ui/Button";
import {Notice} from "../../../components/ui/Notice";
import {NavigationProvider,SessionProvider,destinationFromPathname,useNavigation,type NavigationEntry,type SessionState} from "../../../lib/navigation";
import {LoginProvider} from "../../../features/auth/provider";
import {ProfileProvider} from "../../../features/profile/provider";
import {ProfileScreen} from "../../../features/profile/ProfileScreen";
import {createMockProfileService} from "../../../features/profile/mock";
import {NeighborProvider} from "../../../features/neighbor/provider";
import {InstitutionProvider} from "../../../features/institution/provider";
import {createMockInstitutionService,mockInstitutionSession,mockSubjectId,type InstitutionScenario} from "../../../features/institution/mock";
import {MyPageProvider,useMyPage} from "../../../features/my-page/provider";
import {MyPageScreen} from "../../../features/my-page/MyPageScreen";
import {createMockActivityService,activityScenarios,type ActivityScenario} from "../../../features/my-page/mock";
import {signupRegionFixtures} from "../../../features/signup/mock";
import type {MyMenuHandler} from "../../../features/my-page/model";
function Stub({label,onReturn}:{label:string;onReturn:()=>void}){const nav=useNavigation();return <div className="mx-auto flex max-w-mobile flex-col gap-section p-page"><Notice>메뉴 Mock stub · {label} · 대상 Page/Service를 구현하지 않습니다.</Notice><Button onClick={()=>{nav.back();onReturn();}}>원 마이페이지로 돌아가기</Button></div>;}
function Pages({screen,stub,onReturn}:{screen:string;stub:string;onReturn:()=>void}){if(stub)return <Stub label={stub} onReturn={onReturn}/>;if(screen==="/me/profile")return <ProfileScreen/>;if(screen==="/me/region")return <ProfileScreen region/>;return <MyPageScreen/>;}
function Simulator({service}:{service:ReturnType<typeof createMockActivityService>}){const mine=useMyPage();return <div className="mx-auto flex max-w-mobile gap-internal p-page">{["register","cancel","change","retry"].map(kind=><Button key={kind} variant="secondary" onClick={()=>{service.simulate({category:"bookmarked",relation:"dev-item",kind:kind as "register"|"cancel"|"change"|"retry"});void mine.store.load();}}>{kind}</Button>)}</div>;}
function Harness({scenario,role}:{scenario:ActivityScenario;role:string}) {
 const [service]=useState(()=>createMockActivityService(scenario));const [profile]=useState(()=>createMockProfileService());const [institution]=useState(()=>createMockInstitutionService(role==="institution"?"valid":role==="expired"?"expired":"none"));
 const [screen,setScreen]=useState("/me");const [stub,setStub]=useState("");const [entry,setEntry]=useState<NavigationEntry|null>(null);
 const [neighborService]=useState(()=>({async getCompletedRegions(){return role==="neighbor"?[{id:signupRegionFixtures[0].reference,name:signupRegionFixtures[0].label}]:[];}}));
 const member=mockInstitutionSession(role==="institution"?"valid":role==="expired"?"expired":"none" as InstitutionScenario);
 if(member.status==="member"&&member.capabilities.status==="ready") member.capabilities={status:"ready",grants:[...member.capabilities.grants,{capability:{kind:"neighbor-region",regionId:signupRegionFixtures[0].reference},allowed:role==="neighbor"}]};
 const session:SessionState=role==="guest"?{status:"guest"}:role==="incomplete"?{status:"signup-incomplete"}:member;
 const handler:MyMenuHandler=(id,target)=>{if(id==="profile"||id==="activityRegion"){if(target){setEntry(target);setScreen(id==="profile"?"/me/profile":"/me/region");}return;}setEntry(target);setStub(id);};
 return <SessionProvider session={session}><NavigationProvider currentDestination={entry?.destination??destinationFromPathname(screen)} onNavigate={href=>setScreen(href)} onIntent={target=>{if(target.destination.id==="settings"){setEntry(target);setStub("settings");}}}><LoginProvider><ProfileProvider service={profile}><NeighborProvider subjectKey={mockSubjectId} service={neighborService}><InstitutionProvider service={institution} subjectKey={mockSubjectId}><MyPageProvider service={service} subjectKey={mockSubjectId} onMenu={handler}><Simulator service={service}/><Pages screen={screen} stub={stub} onReturn={()=>{setEntry(null);setStub("");setScreen("/me");}}/></MyPageProvider></InstitutionProvider></NeighborProvider></ProfileProvider></LoginProvider></NavigationProvider></SessionProvider>;
}
export function MyPagePreview(){const [scenario,setScenario]=useState<ActivityScenario>("success");const [role,setRole]=useState("general");return <><div className="mx-auto flex max-w-mobile flex-col gap-internal bg-surface p-page"><Notice>#46 개발 Mock · 실 API/권한/저장 완료가 아닙니다.</Notice><label>활동 조회<select value={scenario} onChange={e=>setScenario(e.currentTarget.value as ActivityScenario)}>{activityScenarios.map(s=><option key={s}>{s}</option>)}</select></label><label>계정 fixture<select value={role} onChange={e=>setRole(e.currentTarget.value)}>{["general","neighbor","institution","expired","guest","incomplete"].map(s=><option key={s}>{s}</option>)}</select></label></div><Harness key={scenario+role} scenario={scenario} role={role}/></>;}
