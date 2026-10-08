"use client";
import { useState } from "react";
import { SessionProvider, destinationFromPathname, useNavigation, type SessionState } from "../../../lib/navigation";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { PostDetail } from "../../../features/post/PostDetail";
import { VotePanel } from "../../../features/vote/VotePanel";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { MyVotesProvider } from "../../../features/my-votes/provider";
import { createMyVotesMock, mockVoteSubject, type MyVotesScenario } from "../../../features/my-votes/mock";
import { postFixtures } from "../../../features/post/mock";

function Entry() {
  const navigation = useNavigation();
  return <MobileLayout header={<Header title="I01 · 개발 진입 Stub"/>}><Button onClick={() => navigation.navigate({ destination: { id: "myVotes" }, origin: { id: "me" } })}>참여한 투표</Button></MobileLayout>;
}
function Harness({ scenario, role }: { scenario: MyVotesScenario; role: string }) {
  const [mock] = useState(() => createMyVotesMock(scenario));
  const [target, setTarget] = useState("/me");
  const subject = role === "other" ? "other-member" : mockVoteSubject;
  const session: SessionState = role === "guest" ? { status: "guest" } : role === "incomplete" ? { status: "signup-incomplete" } : { status: "member", capabilities: { status: "ready", grants: [] } };
  return <SessionProvider session={session}><MyVotesProvider subjectKey={subject} service={mock.service}><SettingsNavigation subjectKey={subject} currentDestination={destinationFromPathname(target)} onNavigate={setTarget} renderPersonalDetail={(postId, onReturn) => {
    const post = postFixtures[postId];
    const service = mock.voteService(postId);
    return <MobileLayout header={<Header title="동일 투표 상세 · Mock" onBack={onReturn}/>}>
      {post?.type === "VOTE" && service && <PostDetail post={post} slots={{ vote: <VotePanel postId={postId} status={post.vote.status} service={service} canVote={true} viewerMode="member"/> }}/>}
      <Button variant="secondary" onClick={onReturn}>원 탭으로 복귀</Button>
      <Button variant="secondary" onClick={() => { mock.hide(postId); onReturn(); }}>개발용 삭제 후 복귀</Button>
    </MobileLayout>;
  }}><Entry/></SettingsNavigation></MyVotesProvider></SessionProvider>;
}
export function MyVotesPreview() {
  const [scenario, setScenario] = useState<MyVotesScenario>("normal");
  const [role, setRole] = useState("member");
  return <><div className="mx-auto flex max-w-mobile flex-col gap-internal p-page"><Notice>#48 개발 Mock · 실제 저장/Integration 완료가 아닙니다.</Notice>
    <label>조회 상태<select value={scenario} onChange={e => setScenario(e.target.value as MyVotesScenario)}>{["normal", "empty", "error", "unknown"].map(value => <option key={value}>{value}</option>)}</select></label>
    <label>계정<select value={role} onChange={e => setRole(e.target.value)}>{["member", "other", "guest", "incomplete"].map(value => <option key={value}>{value}</option>)}</select></label>
  </div><Harness key={scenario + role} scenario={scenario} role={role}/></>;
}
