"use client";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useSession } from "../../lib/navigation";
import { neighborParticipation, type CompletedRegion, type NeighborState } from "./model";
import { useNeighbor } from "./provider";
import type { SessionState } from "../../lib/navigation/guard";
type Props = {
 targetRegion: CompletedRegion; onBack: () => void; activityRegion?: CompletedRegion;
 onReturnToActivity?: () => void; onAuthenticate?: () => void;
};
export function NeighborStatus(props: Props) {
 const { state, retry } = useNeighbor(); const { session, retry: retrySession } = useSession();
 return <NeighborStatusView {...props} state={state} session={session} onRetry={() => { void retrySession?.(); void retry(); }} />;
}
export function NeighborStatusView({ targetRegion, onBack, activityRegion, onReturnToActivity, onAuthenticate, state, session, onRetry }: Props & { state: NeighborState; session: SessionState; onRetry: () => void }) {
 const result = neighborParticipation(state, session, targetRegion.id);
 const busy = result.status === "loading";
 return <section aria-label="이웃 완료 지역·참여 자격" aria-busy={busy} className="flex flex-col gap-section rounded-[20px] border border-border bg-surface px-page pt-[38px] pb-[36px]">
  <div role="status" aria-live="polite">
   {busy ? <Notice>이웃 완료 지역과 참여 자격을 확인하고 있습니다.</Notice>
    : result.status === "error" ? <Notice tone="error">참여 자격을 확인하지 못했습니다. 다시 조회해 주세요.</Notice>
    : result.status === "login-required" ? <Notice>로그인 후 본인의 이웃 완료 지역을 확인할 수 있습니다. 로그인만으로 참여 자격이 부여되지 않습니다.</Notice>
    : result.status === "signup-required" ? <Notice>가입을 완료한 뒤 본인의 이웃 완료 지역을 확인해 주세요. 일반 가입은 이웃 자격과 별개입니다.</Notice>
    : result.status === "allowed" ? <Notice>{targetRegion.name} 이웃 인증이 완료되어 지역 참여가 가능합니다. 실제 요청 시 서버가 자격을 다시 확인합니다.</Notice>
    : <Notice>이 지역 이웃 인증이 없어 지역 참여를 할 수 없습니다.<br />게시물·댓글·투표 결과는 열람할 수 있습니다. 탐색 지역 변경은 참여 권한을 부여하지 않습니다.</Notice>}
  </div>
  {session.status === "member" && state.status === "ready" && (result.status === "allowed" || result.status === "forbidden") && <div className="flex flex-col gap-internal">
   <h2 className="text-body font-semibold">본인 완료 이웃 지역 ({state.regions.length}/3)</h2>
   {state.regions.length ? <ul className="flex flex-wrap gap-internal">{state.regions.map(region => <li key={region.id} className="rounded-chip bg-soft px-section py-1 text-caption text-primary">{region.name} · 완료</li>)}</ul> : <p className="text-body text-secondary">완료된 이웃 지역이 없습니다.</p>}
   <p className="text-caption text-secondary">완료 지역은 최대 3개입니다. 기본 활동 지역·Privy 로그인·기관 자격은 주민 참여 자격을 대신하지 않습니다. 증빙 신청은 이번 MVP에서 제공하지 않습니다.</p>
  </div>}
  {result.status === "error" && <Button disabled={busy} onClick={onRetry}>다시 조회</Button>}
  {(result.status === "login-required" || result.status === "signup-required") && onAuthenticate && <Button onClick={onAuthenticate}>{result.status === "login-required" ? "로그인" : "가입 계속하기"}</Button>}
  <Button variant="secondary" onClick={onBack}>돌아가기</Button>
  {activityRegion && onReturnToActivity && <Button variant="secondary" onClick={onReturnToActivity}>기본 활동 지역 {activityRegion.name}으로 돌아가기</Button>}
 </section>;
}
