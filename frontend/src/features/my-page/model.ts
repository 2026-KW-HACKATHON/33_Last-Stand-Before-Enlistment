import type { Destination,NavigationEntry } from "../../lib/navigation";
export const activityLabels={created:"작성",bookmarked:"북마크",reacted:"반응",commented:"댓글·답글",evaluated:"평가",voted:"투표"} as const;
export type ActivityKind=keyof typeof activityLabels;
export type ActivitySummary={totalCount:number;counts:Record<ActivityKind,number>;currentRelations?:Partial<Record<ActivityKind,number>>};
/** Display port only; the real #5/#66 adapter maps agreed event categories. */
export interface ActivityService {get(signal:AbortSignal):Promise<ActivitySummary|null>}
export const menus=[
 {id:"profile",label:"프로필 / 한 줄 소개 수정",issue:43,destination:{id:"profile"}},
 {id:"bookmarks",label:"북마크",issue:49,destination:{id:"bookmarks"}},
 {id:"myPosts",label:"내가 만든 게시물",issue:47,destination:{id:"myPosts"}},
 {id:"participations",label:"반응한 게시물",issue:47,destination:{id:"participations"}},
 {id:"myVotes",label:"참여한 투표",issue:48,destination:{id:"myVotes"}},
 {id:"neighbor",label:"이웃 완료 지역",issue:44},
 {id:"institution",label:"기관 자격 상태",issue:45},
 {id:"account",label:"계정",issue:92,destination:{id:"account"}},
 {id:"activityRegion",label:"활동 지역",issue:43,destination:{id:"activityRegion"}},
 {id:"interestRegions",label:"관심 지역",issue:87,destination:{id:"interestRegions"}},
 {id:"interestKeywords",label:"관심 키워드",issue:88,destination:{id:"interestKeywords"}},
 {id:"notifications",label:"알림 및 활동 내역",issue:89,destination:{id:"notifications"}},
 {id:"settings",label:"설정",issue:91,destination:{id:"settings"}},
 {id:"withdrawal",label:"회원 탈퇴",issue:95,destination:{id:"withdrawal"}},
] as const satisfies readonly {id:string;label:string;issue:number;destination?:Destination}[];
export type MyMenuId=typeof menus[number]["id"];
export type MyMenuHandler=(id:MyMenuId,entry:NavigationEntry|null)=>void;
export function menuEntry(id:MyMenuId):NavigationEntry|null{const menu=menus.find(m=>m.id===id);return menu&&"destination" in menu ? {destination:menu.destination,origin:{id:"me"}} : null;}
export function validateActivity(value:ActivitySummary):ActivitySummary {
 const keys=Object.keys(activityLabels) as ActivityKind[];
 if(!value||!Number.isSafeInteger(value.totalCount)||value.totalCount<0||keys.some(k=>!Number.isSafeInteger(value.counts?.[k])||value.counts[k]<0)||keys.reduce((n,k)=>n+value.counts[k],0)!==value.totalCount||Object.values(value.currentRelations??{}).some(n=>!Number.isSafeInteger(n)||Number(n)<0))throw new Error("Invalid activity summary");
 return structuredClone(value);
}
export function createActivityStore(service:ActivityService|null) {
 let state:{phase:"idle"|"loading"|"ready"|"empty"|"error";summary:ActivitySummary|null;scroll:number}={phase:"idle",summary:null,scroll:0};let controller:AbortController|null=null;let generation=0;
 const listeners=new Set<()=>void>();const publish=(patch:Partial<typeof state>)=>{state={...state,...patch};listeners.forEach(fn=>fn());};
 const clear=()=>{generation++;controller?.abort();publish({phase:"idle",summary:null,scroll:0});};
 return {getState:()=>state,subscribe:(fn:()=>void)=>{listeners.add(fn);return()=>{listeners.delete(fn);};},clear,setScroll:(scroll:number)=>{if(Number.isFinite(scroll)&&scroll>=0)publish({scroll});},
 async load(){const version=++generation;controller?.abort();controller=new AbortController();const signal=controller.signal;publish({phase:"loading",summary:null});try{if(!service)throw new Error("Adapter unavailable");const value=await service.get(signal);const summary=value===null?null:validateActivity(value);if(version===generation&&!signal.aborted)publish({phase:summary?"ready":"empty",summary});}catch{if(version===generation&&!signal.aborted)publish({phase:"error",summary:null});}},
 dispose(){generation++;controller?.abort();listeners.clear();},};
}
