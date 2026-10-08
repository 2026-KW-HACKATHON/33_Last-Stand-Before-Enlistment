"use client";
/* eslint-disable @next/next/no-img-element -- profile images are supplied by the existing profile adapter. */
import {useEffect,useRef,useState} from "react";
import {SettingRow} from "../../components/ui/SettingRow";
import {Button} from "../../components/ui/Button";
import {Notice} from "../../components/ui/Notice";
import {Header,HeaderAction} from "../../components/layout/Header";
import {MobileLayout} from "../../components/layout/MobileLayout";
import {BottomNavigation} from "../../components/layout/BottomNavigation";
import {AccessGuard,useNavigation,useSession,bottomNavigationDestinations} from "../../lib/navigation";
import {useProfile} from "../profile/provider";
import {NeighborStatus} from "../neighbor/NeighborStatus";
import {InstitutionStatus,InstitutionBadge} from "../institution/InstitutionStatus";
import {useInstitution} from "../institution/provider";
import {institutionAccess} from "../institution/model";
import {activityLabels,menus,menuEntry,type MyMenuId,type ActivitySummary} from "./model";
import {useOpenInterestRegions} from "../interest-regions/InterestRegionsHost";
import {usePersonalLists} from "../personal-lists/provider";
import {useBookmarks} from "../bookmarks/provider";
import {useMyPage} from "./provider";
export function ActivityCounts({summary}:{summary:ActivitySummary}){return <><p className="text-caption">실제 행동 횟수</p><span className="sr-only">누적 전체 {summary.totalCount}회</span><div className="grid grid-cols-3 gap-internal">{(Object.keys(activityLabels) as (keyof typeof activityLabels)[]).map(key=><div key={key} className="rounded-chip bg-[#F3F4F6] px-page py-internal text-center text-caption text-[#4B5563]">{activityLabels[key]} {summary.counts[key]}</div>)}</div></>;}
function Menu({id,onClick}:{id:MyMenuId;onClick:()=>void}){const menu=menus.find(m=>m.id===id)!;return <SettingRow label={menu.label} onClick={onClick}/>;}
function Content(){const lists=usePersonalLists(); const bookmarks=useBookmarks();const openInterestRegions=useOpenInterestRegions();const navigation=useNavigation();const {session}=useSession();const profile=useProfile();const mine=useMyPage();const institution=useInstitution();const shell=useRef<HTMLDivElement>(null);const [panel,setPanel]=useState<"neighbor"|"institution"|null>(null);const [notice,setNotice]=useState("");
 useEffect(()=>{profile.store.setActive(true);if(profile.store.getState().phase==="idle")void profile.store.load();if(mine.store.getState().phase==="idle")void mine.store.load();},[profile.store,mine.store]);
 useEffect(()=>{const main=shell.current?.querySelector("main");if(!main||panel)return;main.scrollTop=mine.store.getState().scroll;const record=()=>mine.store.setScroll(main.scrollTop);main.addEventListener("scroll",record);return()=>main.removeEventListener("scroll",record);},[mine.store,panel,profile.state.phase,mine.state.phase]);
 const saved=profile.state.saved;const region=saved?.activityRegion;const work=institutionAccess(institution.state,mine.subjectId,session);
 function open(id:MyMenuId){if(id==="bookmarks"&&bookmarks){bookmarks.store.open("me");void bookmarks.store.load();navigation.navigate(menuEntry(id)!);return;}setNotice("");if((id==="myPosts"||id==="participations")&&lists){lists.store.open(id,"me");navigation.navigate(menuEntry(id)!);return;}if(id==="myVotes"||id==="account"||id==="settings"||id==="notifications"){navigation.navigate(menuEntry(id)!);return;}if(id==="neighbor"||id==="institution"){setPanel(id);return;}if(id==="interestRegions"&&openInterestRegions){openInterestRegions();return;}const entry=menuEntry(id);if(mine.onMenu){mine.onMenu(id,entry);return;}if((id==="profile"||id==="activityRegion")&&entry){navigation.navigate(entry);return;}const menu=menus.find(m=>m.id===id)!;setNotice(menu.label+" 화면 연결 준비 중입니다. 담당 Issue #"+menu.issue+"의 연결 콜백을 기다립니다.");}
 const back=()=>{if(panel)setPanel(null);else navigation.back();};
 return <div ref={shell}><MobileLayout header={<Header title={panel?panel==="neighbor"?"이웃 완료 지역":"기관 자격 상태":"마이페이지"} onBack={back} rightAction={!panel&&<HeaderAction action="settings" onAction={()=>open("settings")}/>}/>} bottomNavigation={<BottomNavigation activeItem="my" onNavigate={item=>{if(item==="my"){setPanel(null);return;}const entry={destination:bottomNavigationDestinations[item],origin:{id:"me" as const}};if(item==="notification"){open("notifications");return;}navigation.navigate(entry);}}/>}>
 {panel==="neighbor"?<NeighborStatus targetRegion={{id:region?.reference??"unresolved",name:region?.label??"지역 미설정"}} onBack={back} onAuthenticate={()=>navigation.beginAuthentication()}/>
 :panel==="institution"?<InstitutionStatus subjectId={mine.subjectId} onBack={back} onEnterWork={()=>{if(mine.onMenu)mine.onMenu("institution",{destination:{id:"officerAgendas"},origin:{id:"me"}});else navigation.navigate({destination:{id:"officerAgendas"},origin:{id:"me"}});}}/>
 :<>
 {profile.state.phase==="loading"||profile.state.phase==="idle"?<Notice role="status">프로필 불러오는 중</Notice>:profile.state.phase==="error"?<><Notice tone="error">프로필을 불러오지 못했습니다.</Notice><Button onClick={()=>void profile.store.load()}>프로필 다시 조회</Button></>:!saved?<Notice>저장된 프로필이 없습니다.</Notice>:<>
 {saved.photoUrl?<img src={saved.photoUrl} alt="프로필 사진" className="h-[72px] w-full rounded-card border border-border object-cover"/>:<Notice>프로필 사진 없음</Notice>}
 <div className="flex items-center gap-internal"><h2 className="text-section">{saved.nickname}</h2><InstitutionBadge state={institution.state} subjectId={mine.subjectId}/></div>
 <Notice>{saved.attributes.join(" · ")||"이웃 속성 미설정"} | 기본 활동 지역 {region?.label??"미설정"}</Notice><p className="text-body">{saved.bio||"한 줄 소개가 없습니다."}</p></>}
 <Menu id="profile" onClick={()=>open("profile")}/><h2 className="text-section">나의 활동</h2>
 {mine.state.phase==="loading"||mine.state.phase==="idle"?<Notice role="status">활동 횟수 불러오는 중</Notice>:mine.state.phase==="error"?<><Notice tone="error">활동 횟수를 불러오지 못했습니다.</Notice><Button onClick={()=>void mine.store.load()}>활동 다시 조회</Button></>:mine.state.summary?<ActivityCounts summary={mine.state.summary}/>:<Notice>활동 집계 정보가 없습니다. 0회로 추정하지 않습니다.</Notice>}
 {menus.filter(m=>m.id!=="profile").map(menu=><Menu key={menu.id} id={menu.id} onClick={()=>open(menu.id)}/>)}
 {work.status==="allowed"&&<Button variant="secondary" onClick={()=>{if(mine.onMenu)mine.onMenu("institution",{destination:{id:"officerAgendas"},origin:{id:"me"}});else navigation.navigate({destination:{id:"officerAgendas"},origin:{id:"me"}});}}>기관 업무로 이동</Button>}
 <details className="text-caption text-secondary"><summary>누적 활동과 현재 관계 수 안내</summary><p>작성·등록·최초 참여는 +1, 취소·수정·직접 전환·투표 변경·동일 상태 재시도는 +0입니다. 취소 후 재등록은 다시 +1이며 누적 횟수는 차감하지 않습니다.</p>{mine.state.summary?.currentRelations&&<p>현재 관계 수 (누적과 별개): {Object.entries(mine.state.summary.currentRelations).map(([key,n])=>activityLabels[key as keyof typeof activityLabels]+" "+n).join(" · ")}</p>}</details>
 </>}
 {notice&&<Notice role="status">{notice}</Notice>}
 </MobileLayout></div>;
}
export function MyPageScreen(){const navigation=useNavigation();return <AccessGuard destination={{id:"me"}} fallback={(result,retry)=><MobileLayout header={<Header title="마이페이지" onBack={()=>navigation.back()}/>}><Notice>{result.status==="loading"?"인증 상태 확인 중":result.status==="error"?"인증 상태 확인 실패":"로그인과 가입 완료가 필요합니다."}</Notice>{retry&&<Button onClick={()=>void retry()}>다시 확인</Button>}{result.status==="login-required"&&<Button onClick={()=>navigation.beginAuthentication()}>로그인</Button>}{result.status==="signup-required"&&<Button onClick={()=>navigation.beginAuthentication(undefined,"signup")}>가입 계속하기</Button>}</MobileLayout>}><Content/></AccessGuard>;}
