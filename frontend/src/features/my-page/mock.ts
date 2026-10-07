import {activityLabels,type ActivityKind,type ActivityService,type ActivitySummary} from "./model";
export const activityScenarios=["success","empty","zero","error-retry","loading"] as const;
export type ActivityScenario=typeof activityScenarios[number];
export type MockActivityEvent={category:ActivityKind;relation:string;kind:"register"|"cancel"|"change"|"retry"};
/** Fixture-only policy simulation; not a product mutation API. */
export function createMockActivityService(scenario:ActivityScenario="success"):ActivityService&{simulate:(event:MockActivityEvent)=>void} {
 let attempts=0;const keys=Object.keys(activityLabels) as ActivityKind[];
 const counts={created:3,bookmarked:5,reacted:12,commented:4,evaluated:6,voted:2};if(scenario==="zero")keys.forEach(k=>counts[k]=0);
 const current:Partial<Record<ActivityKind,number>>={bookmarked:2,reacted:4,voted:2};if(scenario==="zero")keys.forEach(k=>current[k]=0);const relations=new Set<string>();
 function simulate(event:MockActivityEvent){const key=event.category+":"+event.relation;if(event.kind==="register"&&!relations.has(key)){relations.add(key);counts[event.category]++;current[event.category]=(current[event.category]??0)+1;}else if(event.kind==="cancel"&&relations.delete(key))current[event.category]=Math.max(0,(current[event.category]??0)-1);}
 return {simulate,async get(signal){attempts++;await new Promise<void>((resolve,reject)=>{if(signal.aborted){reject(new Error("Aborted"));return;}const abort=()=>{clearTimeout(timer);reject(new Error("Aborted"));};const timer=setTimeout(()=>{signal.removeEventListener("abort",abort);resolve();},scenario==="loading"?60000:100);signal.addEventListener("abort",abort,{once:true});});if(scenario==="error-retry"&&attempts===1)throw new Error("Mock failure");if(scenario==="empty")return null;const result:ActivitySummary={totalCount:keys.reduce((n,k)=>n+counts[k],0),counts:{...counts},currentRelations:{...current}};return result;}};
}
