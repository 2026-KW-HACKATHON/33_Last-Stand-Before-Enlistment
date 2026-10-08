import assert from "node:assert/strict";
import { test } from "node:test";
import { renderToStaticMarkup } from "react-dom/server";
import { openPost, PostCard } from "../PostCard";
import { PostDetailStateView } from "../PostDetail";
import { authorLabel, isParticipationLinkAvailable, type PostDisplayModel } from "../model";
import { createMockPostService, postFixtures } from "../mock";

const agenda = postFixtures["agenda-photo"];
const anonymous = postFixtures["agenda-anonymous"];
const activity = postFixtures.activity as Extract<PostDisplayModel, { type: "LOCAL_ACTIVITY" }>;
const endedActivity = postFixtures["activity-ended"] as Extract<PostDisplayModel, { type: "LOCAL_ACTIVITY" }>;
const vote = postFixtures.vote;
const endedVote = postFixtures["vote-ended"];

test("all fixture variants use one discriminated Post display model", () => {
  const models: readonly PostDisplayModel[] = [agenda, anonymous, activity, endedActivity, vote, endedVote];
  assert.deepEqual(models.map((post) => post.type), ["LOCAL_AGENDA", "LOCAL_AGENDA", "LOCAL_ACTIVITY", "LOCAL_ACTIVITY", "VOTE", "VOTE"]);
  assert.ok(models.every((post) => post.id && post.author.id && Array.isArray(post.images)));
  assert.ok("activity" in activity && !("vote" in activity));
  assert.ok("vote" in vote && !("activity" in vote));
});

test("anonymous public label preserves the actual author relation and is agenda-only", () => {
  assert.equal(authorLabel(anonymous), "익명");
  assert.equal(anonymous.author.id, "member-02");
  assert.equal(authorLabel({ ...activity, anonymous: true }), activity.author.displayName);
  assert.equal(authorLabel({ ...vote, anonymous: true }), vote.author.displayName);
});

test("PostCard uses the same model, only renders a photo when one exists, and does not own bookmark controls", () => {
  const withPhoto = renderToStaticMarkup(<PostCard post={agenda} onOpen={() => {}} />);
  const withoutPhoto = renderToStaticMarkup(<PostCard post={anonymous} />);
  assert.match(withPhoto, /골목길 보행로 안전을 개선해 주세요/);
  assert.match(withPhoto, /<img/);
  assert.doesNotMatch(withoutPhoto, /<img/);
  assert.doesNotMatch(withPhoto, /북마크/);
  assert.match(withPhoto, /상세 열기/);
  let opened: string | undefined;
  openPost((postId) => { opened = postId; }, agenda.id);
  assert.equal(opened, agenda.id);
});

test("activity keeps source, external participation and common reference link separate", () => {
  const html = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: activity }} />);
  assert.match(html, /노원구 자원봉사센터/);
  assert.match(html, /외부 참여 안내 열기/);
  assert.doesNotMatch(html, /보행 안전 참고 자료/);
  assert.equal(isParticipationLinkAvailable(activity), true);
  assert.equal(isParticipationLinkAvailable(endedActivity), false);
  const ended = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: endedActivity }} />);
  assert.doesNotMatch(ended, /외부 참여 안내 열기/);
});

test("agenda renders optional reference and adoption, and omits both when absent", () => {
  const rich = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: agenda }} />);
  const plain = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: anonymous }} />);
  assert.match(rich, /보행 안전 참고 자료/);
  assert.match(rich, /기관 채택/);
  assert.match(rich, /노원구청/);
  assert.doesNotMatch(plain, /기관 채택/);
  assert.doesNotMatch(plain, /보행 안전 참고 자료/);
});

test("vote is a shell and consumes an injected slot without implementing a vote state machine", () => {
  const open = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: vote }} slots={{ vote: <div>투표 기능 slot</div> }} />);
  const ended = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: endedVote }} />);
  assert.match(open, /투표 진행 중/);
  assert.match(open, /투표 기능 slot/);
  assert.match(ended, /투표 종료/);
  assert.doesNotMatch(open, /선택 제출/);
});

test("participation, comment, summary, share and recommendation compose as supplied slots", () => {
  const html = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: agenda }} slots={{
    summary: <div>요약 slot</div>, reaction: <div>반응 slot</div>, comment: <div>댓글 slot</div>, share: <div>공유 slot</div>, recommendation: <div>추천 slot</div>,
  }} />);
  for (const text of ["요약 slot", "반응 slot", "댓글 slot", "공유 slot", "추천 slot"]) assert.match(html, new RegExp(text));
});

test("bookmark and report are injected boundaries; Bookmark is absent from cards", () => {
  const html = renderToStaticMarkup(<PostDetailStateView state={{ kind: "success", post: agenda }} bookmark={{ isBookmarked: true, feedback: "저장됨", onToggle: () => {} }} onReport={() => {}} />);
  assert.match(html, /북마크 해제/);
  assert.match(html, /저장됨/);
  assert.match(html, /신고/);
});

test("loading, error with Retry, unavailable and restricted stay distinct", () => {
  const loading = renderToStaticMarkup(<PostDetailStateView state={{ kind: "loading" }} />);
  const error = renderToStaticMarkup(<PostDetailStateView state={{ kind: "error", message: "조회 오류", onRetry: () => {} }} />);
  const unavailable = renderToStaticMarkup(<PostDetailStateView state={{ kind: "unavailable" }} />);
  const restricted = renderToStaticMarkup(<PostDetailStateView state={{ kind: "restricted", post: agenda, message: "열람만 가능합니다." }} />);
  assert.match(loading, /aria-busy="true"/);
  assert.match(error, /다시 시도/);
  assert.match(unavailable, /더 이상 볼 수 없는 게시물/);
  assert.doesNotMatch(unavailable, /골목길 보행로/);
  assert.match(restricted, /열람만 가능합니다/);
  assert.match(restricted, /골목길 보행로/);
});

test("mock service shares the PostService interface and never invents a network endpoint", async () => {
  const service = createMockPostService();
  assert.equal((await service.getPost("vote")).id, "vote");
  await assert.rejects(service.getPost("missing"), /unavailable/);
  const controller = new AbortController();
  controller.abort(new Error("cancelled"));
  await assert.rejects(service.getPost("vote", controller.signal));
});
