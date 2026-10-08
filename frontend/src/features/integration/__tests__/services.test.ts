import { test } from "node:test";
import assert from "node:assert/strict";
import { createApiClient } from "../../../lib/api/client";
import { ApiError } from "../../../lib/api/error";
import { createFeatureServices } from "../index";
import { postDisplay, voteSnapshot } from "../post-decoder";
import { emptyPostForm } from "../../post-editor/model";
import { selectItems } from "../../personal-lists/model";
import { allPages, inputId } from "../wire";
import { photoView } from "../editor-services";

const at = "2026-10-08T12:00:00+09:00";
const place = { id: 1, name: "등록된 시험 지역" };
const counts = { EMPATHY: 2, NEEDED: 1, CURIOUS: 0, total: 3 };
const poll = { status: "OPEN", question: "질문", endsAt: "2026-12-01T18:00:00+09:00", participantCount: 3, myOptionId: 12, options: [{ id: 11, content: "하나", voteCount: 2, votePercentage: 66.67 }, { id: 12, content: "둘", voteCount: 1, votePercentage: 33.33 }] };
const detail = {
  id: 101, type: "LOCAL_AGENDA", topic: "ENVIRONMENT", title: "지역 안건", content: "원문", status: "PUBLISHED", region: place,
  author: { displayName: "공개 작성자", profileImageUrl: null, institutionVerified: false }, images: [], reactionCounts: counts, commentCount: 0,
  comments: [], commentsMeta: { sort: "LIKES", hasNext: false, nextCursor: null }, adoptions: [], myState: { reactions: ["EMPATHY"], isBookmarked: false },
  capabilities: { canEdit: true, canDelete: true, canBookmark: true, canReact: true, canComment: true, canVote: false, canEvaluateComment: true, canAdopt: false, canCancelAdoption: false }, createdAt: at, updatedAt: at,
};
const card = { id: 101, type: "LOCAL_AGENDA", topic: "ENVIRONMENT", title: "지역 안건", excerpt: "일부 원문", region: place, author: detail.author, thumbnailUrl: null, reactionCounts: counts, commentCount: 0, createdAt: at, updatedAt: at, capabilities: { canEdit: true, canDelete: true } };
const comment = { id: 301, postId: 101, parentCommentId: null, content: "의견", author: { displayName: "회원", isGuest: false, institutionVerified: false }, replyTo: null, likeCount: 2, dislikeCount: 1, myEvaluation: "LIKE", createdAt: at, replies: [] };
const profile = { id: 23, email: "fixture@example.invalid", profile: { nickname: "주민", bio: null, profileImageUrl: null, residentAttributes: ["RESIDENT", "STUDENT"], activityRegion: place } };
const view = { fileId: 50, status: "UNLINKED", contentType: "image/png", sizeBytes: 4, url: "https://example.invalid/photo.png", createdAt: at, uploadedAt: at, uploadAuthorizationExpiresAt: at, cleanupEligibleAt: at, linkExpiresAt: at, canAttach: true, deletionCompleted: false, deleteRequestedAt: null, deletedAt: null };
const envelope = (value: unknown) => ({ data: value });
const paged = (value: unknown[], nextCursor: string | null = null) => ({ data: value, meta: { hasNext: nextCursor !== null, nextCursor } });
function fixture(handler: (request: Request) => unknown | Response | Promise<unknown | Response>) {
  const requests: Request[] = [];
  const client = createApiClient({ baseUrl: "https://api.example.invalid/api/v1", prepareRequest: () => ({ headers: { Authorization: "Bearer synthetic-test-only" } }), transport: async request => {
    requests.push(request); const body = await handler(request);
    return body instanceof Response ? body : Response.json(body);
  } });
  return { client, services: createFeatureServices(client), requests };
}

test("detail translates numeric IDs, topic, permissions and leaves absent author ID absent", () => {
  const post = postDisplay(detail);
  assert.equal(post.id, "101"); assert.equal(post.metadata.regionId, "1"); assert.equal(post.metadata.topic, "환경"); assert.equal(post.author.id, undefined); assert.equal(post.capabilities.canEdit, true); assert.equal(post.viewer.mode, "member");
});
test("guest detail never manufactures member state", () => {
  const row = { ...detail, myState: undefined, capabilities: { ...detail.capabilities, canEdit: false, canDelete: false, canBookmark: false, canReact: false } };
  const post = postDisplay(row); assert.equal(post.viewer.mode, "guest"); assert.equal(post.capabilities.canBookmark, false);
});
for (const [field, value] of [["id", "101"], ["id", 0], ["id", 9007199254740992], ["topic", "UNKNOWN"], ["createdAt", "2026-10-08"], ["status", "DELETED"]] as const) {
  test(`reject malformed detail ${field}=${value}`, () => assert.throws(() => postDisplay({ ...detail, [field]: value })));
}
test("CANCELED wire maps to CANCELLED display and external links stay disabled", () => {
  const post = postDisplay({ ...detail, type: "LOCAL_ACTIVITY", activity: { source: "출처", schedule: "일정", place: "장소", status: "CANCELED", externalParticipationUrl: "https://example.invalid/event", externalParticipationEnabled: false, organizerEmail: null } });
  assert.equal(post.type, "LOCAL_ACTIVITY"); if (post.type === "LOCAL_ACTIVITY") { assert.equal(post.activity.status, "CANCELLED"); assert.equal(post.activity.participationLink, undefined); }
});
test("vote selection uses server own choice rather than leader", () => assert.equal(voteSnapshot(poll).submittedOptionId, "12"));
test("vote CLOSED maps to ENDED", () => {
  const post = postDisplay({ ...detail, type: "VOTE", vote: { ...poll, status: "CLOSED" } });
  assert.equal(post.type, "VOTE"); if (post.type === "VOTE") assert.equal(post.vote.status, "ENDED");
});
test("vote count mismatch and non-option own selection are rejected", () => {
  assert.throws(() => voteSnapshot({ ...poll, participantCount: 10 })); assert.throws(() => voteSnapshot({ ...poll, myOptionId: 99 }));
});
test("reaction get reads detail, PUT/DELETE have no body and use A authentication", async () => {
  const f = fixture(request => request.method === "GET" ? envelope(detail) : envelope({ postId: 101, reactionCounts: counts, myReactions: request.method === "PUT" ? ["NEEDED"] : [] }));
  assert.deepEqual((await f.services.reactionService.get("101")).selected, ["EMPATHY"]);
  await f.services.reactionService.set("101", "NEEDED", true); await f.services.reactionService.set("101", "NEEDED", false);
  assert.equal(new URL(f.requests[1].url).pathname, "/api/v1/posts/101/reactions/NEEDED"); assert.equal(await f.requests[1].text(), ""); assert.equal(f.requests[2].method, "DELETE"); assert.equal(f.requests[1].headers.get("Authorization"), "Bearer synthetic-test-only");
});
test("reaction total mismatch is rejected", async () => {
  const f = fixture(() => envelope({ postId: 101, reactionCounts: { ...counts, total: 999 }, myReactions: [] }));
  await assert.rejects(f.services.reactionService.set("101", "EMPATHY", true), (error: unknown) => error instanceof ApiError && error.kind === "invalid-response");
});
test("vote submits numeric optionId and validates target", async () => {
  const f = fixture(() => envelope({ ...poll, postId: 101 })); await f.services.voteService.submit("101", "12");
  assert.deepEqual(await f.requests[0].json(), { optionId: 12 });
});
test("comment pagination retains replies and target identity", async () => {
  const reply = { ...comment, id: 302, parentCommentId: 301, replyTo: { commentId: 301, displayName: "회원" }, replies: [] };
  const f = fixture(request => request.method === "GET" ? paged([{ ...comment, replies: [reply] }]) : envelope(reply));
  const rows = await f.services.commentService.list("101"); assert.equal(rows[0].replies[0].parentCommentId, "301");
  await f.services.commentService.createReply("101", { content: "답글", parentCommentId: "301", replyToCommentId: "302", targetAuthorName: "표시명" });
  assert.deepEqual(await f.requests[1].json(), { content: "답글", replyToCommentId: 302 });
});
test("evaluation list flattens comments, NONE sends DELETE with no body", async () => {
  const f = fixture(request => request.method === "GET" ? paged([comment]) : envelope({ commentId: 301, myEvaluation: null, likeCount: 1, dislikeCount: 1 }));
  assert.equal((await f.services.evaluationService.list("101"))[0].selected, "LIKE");
  assert.equal((await f.services.evaluationService.set("301", "NONE")).selected, "NONE"); assert.equal(f.requests[1].method, "DELETE"); assert.equal(await f.requests[1].text(), "");
});
test("omitted guest evaluation is never decoded as member NONE", async () => {
  const f = fixture(() => paged([{ ...comment, myEvaluation: undefined }])); await assert.rejects(f.services.evaluationService.list("101"));
});
test("flat backend error keeps domain code for profile nickname failure", async () => {
  const f = fixture(() => Response.json({ code: "NICKNAME_ALREADY_IN_USE", message: "중복", details: [], traceId: "test" }, { status: 409 }));
  const result = await f.services.profileService.save({ nickname: "주민", bio: "", attributes: [], activityRegion: { reference: "1", label: "지역" } }, new AbortController().signal);
  assert.deepEqual(result, { kind: "failure", reason: "nickname", retryable: false });
});
test("profile PATCH maps labels/region IDs without identity or photo fields", async () => {
  const f = fixture(() => envelope(profile)); const signal = new AbortController().signal;
  const loaded = await f.services.profileService.load(signal); assert.equal(loaded.kind, "success");
  if (loaded.kind !== "success" || !loaded.value) throw new Error("Load failed");
  await f.services.profileService.save(loaded.value, signal);
  assert.deepEqual(await f.requests[1].json(), { nickname: "주민", bio: "", residentAttributes: ["RESIDENT", "STUDENT"], activityRegionId: 1 });
});
test("profile photo remains explicitly unsupported", async () => {
  const f = fixture(() => { throw new Error("Must not request"); });
  const result = await f.services.profileService.save({ nickname: "주민", bio: "", attributes: [], activityRegion: null, photo: { kind: "remove" } }, new AbortController().signal); assert.equal(result.kind, "failure"); assert.equal(f.requests.length, 0);
});
test("neighbor adapter returns completed regions and does not grant extra regions", async () => {
  const f = fixture(() => envelope({ verifiedRegions: [{ ...place, verifiedAt: at }], maxVerifiedRegions: 3, targetRegion: null }));
  assert.deepEqual(await f.services.neighborService.getCompletedRegions(new AbortController().signal), [{ id: "1", name: place.name }]);
});
test("institution expiration maps distinctly and preserves private institution ID", async () => {
  const f = fixture(() => envelope({ id: 23, institutionVerification: { status: "EXPIRED", institutionId: 5, institutionName: "기관", responsibleRegion: place, completedAt: at, validUntil: "2027-10-08T12:00:00+09:00", isActive: false }, institutionVerified: false }));
  const result = await f.services.institutionService.getQualification(new AbortController().signal); assert.equal(result.subjectId, "23"); assert.equal(result.status, "expired"); if ("institution" in result) assert.equal(result.institution.id, "5");
});
test("bookmark 404 returns unavailable rather than successful saved state", async () => {
  const f = fixture(() => new Response(null, { status: 404 })); assert.deepEqual(await f.services.bookmarkService.get("ignored-scope", "101", new AbortController().signal), { postId: "101", available: false, isBookmarked: false });
});
test("bookmark desired state sends no userId and preserves failures", async () => {
  const f = fixture(() => envelope({ postId: 101, isBookmarked: true })); await f.services.bookmarkService.set("private-scope", "101", true, new AbortController().signal);
  assert.equal(new URL(f.requests[0].url).pathname, "/api/v1/posts/101/bookmark"); assert.equal(await f.requests[0].text(), "");
});
test("personal lists use me endpoint ownership without inventing public author ID", async () => {
  const f = fixture(request => new URL(request.url).pathname.endsWith("/posts") ? paged([card]) : paged([{ ...card, myParticipation: { reactions: ["NEEDED"], hasCommentOrReply: false, hasCommentOrReplyEvaluation: false, hasVote: false }, participatedAt: at }]));
  const rows = await f.services.personalListsService.list("23", new AbortController().signal);
  assert.equal(rows[0].post.author.id, undefined); assert.equal(selectItems(rows, "23", "myPosts", "ALL").length, 1); assert.deepEqual(selectItems(rows, "23", "participations", "ALL")[0].actions, ["NECESSARY"]);
});
test("deleted vote history keeps no post/options and remains ALL-only in store", async () => {
  const f = fixture(() => paged([{ postId: 101, availability: "UNAVAILABLE", participatedAt: at, post: null, vote: null }]));
  const rows = await f.services.myVotesService.list("23", new AbortController().signal); assert.deepEqual(rows[0], { postId: "101", subjectKey: "23", participatedAt: at, availability: "unavailable", status: null });
});
test("available vote history maps split post/vote wire", async () => {
  const f = fixture(() => paged([{ postId: 101, availability: "AVAILABLE", participatedAt: at, post: { ...card, type: "VOTE" }, vote: poll }]));
  const row = (await f.services.myVotesService.list("23", new AbortController().signal))[0]; assert.equal(row.availability, "available"); if (row.availability === "available") assert.equal(row.snapshot?.submittedOptionId, "12");
});
test("home is a single home API call with temporary region, no PATCH", async () => {
  const f = fixture(() => envelope({ region: place, posts: [card], openVotes: [], boardCounts: { LOCAL_AGENDA: 1, LOCAL_ACTIVITY: 0, VOTE: 0 } }));
  const r = { id: "1", name: "지역", neighborVerified: false };
  assert.equal((await f.services.exploreService.getHome({ currentExploreRegion: r, defaultActivityRegion: r, exploreRegionCandidates: [], neighborVerifiedRegions: [], institutionRegions: [] })).regionPosts.length, 1);
  assert.equal(new URL(f.requests[0].url).searchParams.get("regionId"), "1"); assert.equal(f.requests.length, 1);
});
test("board translates topic and passes opaque cursor", async () => {
  const f = fixture(() => paged([card])); await f.services.exploreService.getBoard({ regionId: "1", type: "LOCAL_AGENDA", topic: "환경" }, "opaque/+=cursor");
  const q = new URL(f.requests[0].url).searchParams; assert.equal(q.get("topic"), "ENVIRONMENT"); assert.equal(q.get("cursor"), "opaque/+=cursor");
});
test("repeated cursor fails instead of infinite loop", async () => {
  const f = fixture(() => paged([], "same")); await assert.rejects(allPages(f.client, "users/me/bookmarks"), (e: unknown) => e instanceof ApiError && e.kind === "invalid-response"); assert.equal(f.requests.length, 2);
});
test("map requires actual viewport region mapping", async () => {
  const f = fixture(() => { throw new Error("No request"); }); await assert.rejects(f.services.mapService.getMap({ centerRegionId: "1", zoom: 13 })); assert.equal(f.requests.length, 0);
});
test("map retains empty dong and hydrates minimal representative from actual detail", async () => {
  const f = fixture(request => new URL(request.url).pathname.endsWith("/dongs") ? envelope({ centerRegion: place, dongs: [{ region: place, representativePost: { id: 101 } }, { region: { id: 2, name: "빈 지역" }, representativePost: null }] }) : envelope(detail));
  const services = createFeatureServices(f.client, { mapRegionIds: () => ["1", "2"] }); const map = await services.mapService.getMap({ centerRegionId: "1", zoom: 13 });
  assert.equal(map.dongs[0].representativePost?.id, "101"); assert.equal(map.dongs[1].representativePost, null);
});
test("shared adapter carries only scoped share header, blocks unrelated requests", async () => {
  const f = fixture(() => envelope({ ...detail, myState: undefined }));
  const services = f.services.createSharedServices("101", "synthetic-share-only"); await services.postService.getPost("101");
  assert.equal(f.requests[0].headers.get("X-Post-Share-Token"), "synthetic-share-only"); await assert.rejects(services.postService.getPost("102"));
});
test("share copies actual returned URL rather than guessed URL", async () => {
  const f = fixture(() => envelope({ postId: 101, shareUrl: "https://web.example.invalid/shared/posts/101?token=synthetic" })); let copied = "";
  await createFeatureServices(f.client, { writeClipboard: async value => { copied = value; } }).shareService.copy("101"); assert.equal(copied, "https://web.example.invalid/shared/posts/101?token=synthetic");
});
test("AI pending/failed preserves original source fallback", async () => {
  const f = fixture(() => envelope({ postId: 101, status: "PENDING", summary: null })); const post = postDisplay(detail); if (post.type !== "LOCAL_AGENDA") throw new Error();
  assert.equal((await f.services.summaryService.getSummary(post)).status, "FAILED");
});
test("create success + failed detail never invites second POST", async () => {
  const f = fixture(request => request.method === "POST" ? envelope({ postId: 101 }) : new Response(null, { status: 503 }));
  const form = { ...emptyPostForm(), regionId: "1", title: "제목", content: "본문" }; assert.deepEqual(await f.services.postEditorService.create(form), { postId: "101" }); assert.equal(f.requests.filter(request => request.method === "POST").length, 1);
});
test("activity create omits empty external URL; canceled state uses actual schema spelling", async () => {
  const f = fixture(request => request.method === "POST" ? envelope({ postId: 101 }) : new Response(null, { status: 503 }));
  const form = { ...emptyPostForm("LOCAL_ACTIVITY"), regionId: "1", title: "제목", content: "본문", activity: { source: "출처", schedule: "일정", location: "장소", status: "CANCELLED" as const, link: "" } };
  await f.services.postEditorService.create(form); const body = await f.requests[0].json(); assert.equal(body.details.activityStatus, "CANCELED"); assert.equal("externalParticipationUrl" in body.details, false);
});
test("edit preserves photoOrder and removed-file pending metadata", async () => {
  const f = fixture(() => ({ data: detail, meta: { photoDeletion: { status: "PENDING", fileIds: [50] } } }));
  await f.services.postEditorService.update("101", { ...emptyPostForm(), regionId: "1", title: "제목", content: "내용", photos: [{ id: "801", photoId: "801", name: "기존", size: 4, type: "image/png", status: "ready" }] });
  assert.deepEqual((await f.requests[0].json()).photoOrder, [{ photoId: 801 }]); assert.deepEqual(f.services.getPendingPhotoDeletions("101"), ["50"]);
});
test("delete captures file IDs before 204 and does not claim storage deletion", async () => {
  const f = fixture(request => request.method === "DELETE" ? new Response(null, { status: 204 }) : envelope({ ...detail, images: [{ photoId: 801, fileId: 50, url: view.url, contentType: "image/png", sizeBytes: 4 }] }));
  await f.services.postEditorService.remove("101"); assert.deepEqual(f.services.getPendingPhotoDeletions("101"), ["50"]); assert.equal(f.requests[1].method, "DELETE");
});
test("photo reservation/RAW transfer/complete uses only trusted relative relay endpoint", async () => {
  const f = fixture(request => request.method === "POST" && new URL(request.url).pathname.endsWith("/photo-uploads") ? envelope({ fileId: 50, status: "UPLOADING", createdAt: at, cleanupEligibleAt: at, upload: { url: "/api/v1/photo-uploads/50/content", method: "PUT", bodyMode: "RAW", headers: { "Content-Type": "image/png" }, expiresAt: at } }) : envelope(view));
  const file = new File([new Uint8Array([1, 2, 3, 4])], "a.png", { type: "image/png" });
  const result = await f.services.photoService.process({ id: "selection", name: file.name, type: file.type, size: file.size, file, status: "selected" });
  assert.equal(result?.fileId, "50"); assert.equal(f.requests[1].method, "PUT"); assert.equal((await f.requests[1].arrayBuffer()).byteLength, 4); assert.deepEqual(await f.requests[2].json(), {});
});
test("photo refuses foreign upload URL before sending credentials or bytes", async () => {
  const f = fixture(() => envelope({ fileId: 50, status: "UPLOADING", upload: { url: "https://evil.example.invalid/content", method: "PUT", bodyMode: "RAW" } }));
  const file = new File([new Uint8Array([1])], "a.png", { type: "image/png" }); await assert.rejects(f.services.photoService.process({ id: "selection", file, name: file.name, type: file.type, size: file.size, status: "selected" })); assert.equal(f.requests.length, 1);
});
test("pending photo deletion cannot be decoded as completed", () => {
  assert.equal(photoView({ ...view, status: "DELETE_PENDING", canAttach: false }).deletionCompleted, false); assert.throws(() => photoView({ ...view, deletionCompleted: true }));
});
test("id input rejects fixture names, unsafe IDs and fractions", () => {
  for (const value of ["gongneung-2", "0", "1.5", "9007199254740992", "001"]) assert.throws(() => inputId(value)); assert.equal(inputId("1"), 1);
});

test("officer cancellation uses private current-institution relation identity, not public names", async () => {
  let adopted = true;
  const f = fixture(request => {
    const path = new URL(request.url).pathname;
    if (request.method === "DELETE") { assert.equal(path, "/api/v1/posts/101/adoptions/77"); adopted = false; return new Response(null, { status: 204 }); }
    if (path.endsWith("/agendas")) return paged([{ postId: 101, type: "LOCAL_AGENDA", regionId: 1, reactionCounts: { empathy: 2, needed: 1, curious: 0, total: 3 }, myInstitutionAdoption: adopted ? { id: 77, adoptedAt: at } : null }]);
    if (path.endsWith("institution-verifications")) return envelope({ id: 23, institutionVerification: { status: "COMPLETED", institutionId: 5, institutionName: "기관", responsibleRegion: place, completedAt: at, validUntil: "2027-10-08T12:00:00+09:00", isActive: true }, institutionVerified: true });
    if (path.endsWith("/comments")) return paged([]);
    return envelope({ ...detail, adoptions: [{ institutionName: "다른 기관", adoptedAt: at }] });
  });
  const result = await f.services.adoptionService.setAdopted("101", false, new AbortController().signal);
  assert.equal(result.relations.length, 0); assert.equal(result.post.adoptions[0].institutionName, "다른 기관");
});
test("adoption POST carries no client institution ID, then rereads actual state", async () => {
  let adopted = false;
  const f = fixture(request => {
    const path = new URL(request.url).pathname;
    if (request.method === "POST") { adopted = true; return envelope({ id: 77, postId: 101, institutionName: "기관", adoptedAt: at }); }
    if (path.endsWith("/agendas")) return paged([{ postId: 101, type: "LOCAL_AGENDA", regionId: 1, reactionCounts: { empathy: 2, needed: 1, curious: 0, total: 3 }, myInstitutionAdoption: adopted ? { id: 77, adoptedAt: at } : null }]);
    if (path.endsWith("institution-verifications")) return envelope({ id: 23, institutionVerification: { status: "COMPLETED", institutionId: 5, institutionName: "기관", responsibleRegion: place, completedAt: at, validUntil: "2027-10-08T12:00:00+09:00", isActive: true }, institutionVerified: true });
    if (path.endsWith("/comments")) return paged([]);
    return envelope(detail);
  });
  const result = await f.services.adoptionService.setAdopted("101", true, new AbortController().signal);
  assert.equal(result.relations[0].id, "77"); assert.equal(result.relations[0].institutionId, "5");
  assert.equal(await f.requests.find(r => r.method === "POST")!.text(), "");
});
test("institution inconsistent badge/current-status is rejected", async () => {
  const f = fixture(() => envelope({ id: 23, institutionVerification: { status: "NOT_SUBMITTED", isActive: false, institutionId: null, institutionName: null, responsibleRegion: null, completedAt: null, validUntil: null }, institutionVerified: true })); await assert.rejects(f.services.institutionService.getQualification(new AbortController().signal));
});
test("photo known reservation retry completes existing upload without duplicate reserve/PUT", async () => {
  const f = fixture(() => envelope(view));
  const file = new File([new Uint8Array([1])], "a.png", { type: "image/png" });
  const result = await f.services.photoService.process({ id: "selection", fileId: "50", name: file.name, size: file.size, type: file.type, file, status: "error" });
  assert.equal(result?.fileId, "50"); assert.equal(f.requests.length, 1); assert.equal(f.requests[0].method, "GET");
});
test("post type VOTE edit excludes immutable region/topic/options", async () => {
  const f = fixture(() => envelope({ ...detail, type: "VOTE", vote: poll }));
  const form = { ...emptyPostForm("VOTE"), regionId: "1", title: "제목", content: "내용", vote: { question: "질문", options: ["하나", "둘"], endsAt: poll.endsAt, status: "OPEN" as const } };
  await f.services.postEditorService.update("101", form);
  assert.deepEqual(await f.requests[0].json(), { title: "제목", content: "내용", photoOrder: [], details: { endsAt: poll.endsAt } });
});
test("server forbidden cannot be transformed into successful bookmark", async () => {
  const f = fixture(() => Response.json({ code: "NEIGHBOR_VERIFICATION_REQUIRED", message: "거부", details: [], traceId: "test" }, { status: 403 })); await assert.rejects(f.services.bookmarkService.set("23", "101", true, new AbortController().signal), (error: unknown) => error instanceof ApiError && error.kind === "forbidden");
});
