"use client";
import { useState } from "react";
import { SessionProvider, destinationFromPathname, type SessionState } from "../../../lib/navigation";
import { Notice } from "../../../components/ui/Notice";
import { Button } from "../../../components/ui/Button";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { PostDetail } from "../../../features/post/PostDetail";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { PersonalListsProvider, usePersonalLists } from "../../../features/personal-lists/provider";
import { createMockPersonalListsService, mockSubject } from "../../../features/personal-lists/mock";
function Entry() { const lists = usePersonalLists()!; return <MobileLayout header={<Header title="마이페이지 / I01 Mock"/>}><Button onClick={() => lists.store.open("myPosts", "me")}>내가 만든 게시물</Button><Button onClick={() => lists.store.open("participations", "me")}>참여한 게시물</Button><SettingsEntry/></MobileLayout>; }
function SettingsEntry() { const nav = useNavigation(); return <Button onClick={() => nav.navigate({ destination: { id: "settings" }, origin: { id: "me" } })}>설정 진입</Button>; }
import { useNavigation } from "../../../lib/navigation";
function Harness({ scenario, role }: { scenario: "normal" | "empty" | "error"; role: string }) {
 const [mock] = useState(() => createMockPersonalListsService(scenario)); const [target, setTarget] = useState("/me");
 const session: SessionState = role === "guest" ? { status: "guest" } : role === "incomplete" ? { status: "signup-incomplete" } : { status: "member", capabilities: { status: "ready", grants: [] } };
 const subject = role === "other" ? "other-member" : mockSubject;
 return <SessionProvider session={session}><PersonalListsProvider subjectKey={subject} service={mock.service}><SettingsNavigation subjectKey={subject} currentDestination={destinationFromPathname(target)} onNavigate={href => setTarget(href)} renderPersonalDetail={(postId, onReturn) => { const record = mock.records.find(r => r.post.id === postId); return <MobileLayout header={<Header title="원 게시물 상세 · Mock" onBack={onReturn}/>}>{record?.visible ? <PostDetail post={record.post} slots={{ vote: record.voteSummary ? <Notice>{record.voteSummary}</Notice> : undefined }}/> : <Notice>삭제되었거나 볼 수 없는 게시물입니다.</Notice>}<Button onClick={() => { mock.cancelAction(postId, "EMPATHY"); onReturn(); }}>공감 취소 후 목록</Button><Button onClick={() => { mock.hide(postId); onReturn(); }}>삭제 / 비노출 후 목록</Button><Button onClick={onReturn}>목록으로</Button></MobileLayout>; }}><Entry/><Notice>개발 navigation 목적지: {target}</Notice></SettingsNavigation></PersonalListsProvider></SessionProvider>;
}
export function PersonalListsPreview() { const [scenario, setScenario] = useState<"normal" | "empty" | "error">("normal"); const [role, setRole] = useState("member"); return <><div className="mx-auto flex max-w-mobile flex-col gap-internal p-page"><Notice>#47 개발 Mock · 실 API 및 Integration 완료가 아닙니다.</Notice><label>조회 상태<select value={scenario} onChange={e => setScenario(e.target.value as typeof scenario)}>{["normal", "empty", "error"].map(x => <option key={x}>{x}</option>)}</select></label><label>계정<select value={role} onChange={e => setRole(e.target.value)}>{["member", "other", "guest", "incomplete"].map(x => <option key={x}>{x}</option>)}</select></label></div><Harness key={scenario + role} scenario={scenario} role={role}/></>; }
