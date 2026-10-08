"use client";
import { useState } from "react";
import { SessionProvider, destinationFromPathname, useNavigation, type SessionState } from "../../../lib/navigation";
import { Header } from "../../../components/layout/Header";
import { MobileLayout } from "../../../components/layout/MobileLayout";
import { Button } from "../../../components/ui/Button";
import { Notice } from "../../../components/ui/Notice";
import { PostDetail } from "../../../features/post/PostDetail";
import { SettingsNavigation } from "../../../features/settings/SettingsNavigation";
import { BookmarksProvider, useBookmarks } from "../../../features/bookmarks/provider";
import { createBookmarksMock, bookmarkMockSubject, type BookmarkScenario } from "../../../features/bookmarks/mock";

function Entry() {
  const navigation = useNavigation(); const bookmarks = useBookmarks();
  return <MobileLayout header={<Header title="I01 · 개발 Mock"/>}><Button onClick={() => { bookmarks?.store.open("me"); navigation.navigate({ destination: { id: "bookmarks" }, origin: { id: "me" } }); }}>북마크</Button><Button variant="secondary" onClick={() => navigation.navigate({ destination: { id: "settings" }, origin: { id: "me" } })}>설정에서 진입</Button></MobileLayout>;
}
function Harness({ role }: { role: string }) {
  const [mock] = useState(() => createBookmarksMock("normal", 200));
  const [target, setTarget] = useState("/me");
  const [scenario, setScenario] = useState<BookmarkScenario>("normal");
  const subject = role === "other" ? "other-member" : bookmarkMockSubject;
  const session: SessionState = role === "guest" ? { status: "guest" } : role === "incomplete" ? { status: "signup-incomplete" } : { status: "member", capabilities: { status: "ready", grants: [] } };
  return <><div className="mx-auto flex max-w-mobile flex-col gap-internal p-page"><Notice>#49 개발 Mock · 실 API 연동 대기</Notice>
    <label>조회/변경 상태<select value={scenario} onChange={e => { const next = e.target.value as BookmarkScenario; mock.scenario(next); setScenario(next); }}>{["normal", "empty", "error", "mutation-error"].map(value => <option key={value}>{value}</option>)}</select></label>
  </div><SessionProvider session={session}><BookmarksProvider subjectKey={subject} service={mock.service}><SettingsNavigation subjectKey={subject} currentDestination={destinationFromPathname(target)} onNavigate={setTarget} renderBookmarkDetail={({ postId, bookmark, onReturn }) => {
    const post = mock.post(postId);
    return <MobileLayout header={<Header title="동일 원 상세 · Mock" onBack={onReturn}/>}>
      {post ? <PostDetail post={post} bookmark={bookmark}/> : <Notice>삭제되었거나 접근할 수 없는 게시물입니다.</Notice>}
      <Button variant="secondary" onClick={onReturn}>목록 조건 그대로 복귀</Button>
      <Button variant="secondary" onClick={() => { mock.removeOriginal(postId); onReturn(); }}>개발용 원본 삭제 후 복귀</Button>
    </MobileLayout>;
  }}><Entry/></SettingsNavigation></BookmarksProvider></SessionProvider></>;
}
export function BookmarksPreview() {
  const [role, setRole] = useState("member");
  return <><div className="mx-auto max-w-mobile p-page"><label>계정<select value={role} onChange={e => setRole(e.target.value)}>{["member", "other", "guest", "incomplete"].map(value => <option key={value}>{value}</option>)}</select></label></div><Harness key={role} role={role}/></>;
}
