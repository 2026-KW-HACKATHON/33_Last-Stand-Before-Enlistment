import type { ApiClient } from "../../lib/api/types";
import { ApiError } from "../../lib/api/error";
import type { ProfileService, ProfileValue, Result } from "../profile/contracts";
import type { NeighborService } from "../neighbor/model";
import type { InstitutionService, InstitutionQualification } from "../institution/model";
import type { BookmarkService } from "../bookmarks/model";
import type { PersonalListsService, PersonalRecord, Participation } from "../personal-lists/model";
import type { MyVotesService, MyVoteRecord } from "../my-votes/model";
import type { AdoptionService, AgendaReview, AdoptionRelation } from "../officer-agendas/model";
import type { createContentServices } from "./content-services";
import { decodeNoContent } from "../../lib/api/response";
import { allPages, array, bool, data, enumValue, id, inputId, nullableText, object, query, region, text, timestamp } from "./wire";
import { postDisplay, reactions, voteSnapshot } from "./post-decoder";

const attributes = { RESIDENT: "거주자", STUDENT: "학생", WORKER: "직장인", MERCHANT: "상인" } as const;
export function profileValue(value: unknown): ProfileValue {
  const row = object(value); id(row.id); text(row.email);
  const profile = object(row.profile), activityRegion = region(profile.activityRegion);
  if (profile.profileImageUrl !== null) throw new Error("Profile image contract is unavailable");
  return { nickname: text(profile.nickname), bio: nullableText(profile.bio) ?? "", attributes: array(profile.residentAttributes).map(value => attributes[enumValue(value, Object.keys(attributes) as (keyof typeof attributes)[])]), activityRegion: { reference: activityRegion.id, label: activityRegion.name } };
}
function failure(error: unknown): Extract<Result<never>, { kind: "failure" }> {
  const reason = error instanceof ApiError ? error.code === "NICKNAME_ALREADY_IN_USE" ? "nickname" : error.code === "REGION_NOT_FOUND" ? "region" : error.kind === "forbidden" || error.kind === "unauthorized" ? "forbidden" : error.kind === "unavailable" ? "unavailable" : "failed" : "unknown";
  return { kind: "failure", reason, retryable: !(error instanceof ApiError) || ["network", "server", "rate-limit"].includes(error.kind) };
}
/** subjectKey is only a local cache scope; no request sends a client identity. */
export function createMemberServices(client: ApiClient, content: ReturnType<typeof createContentServices>) {
  const profileService: ProfileService = {
    source: "api",
    async load(signal) { try { return { kind: "success", value: await data(client, "users/me", profileValue, { signal }) }; } catch (error) { return failure(error); } },
    async save(value, signal) {
      if (value.photo) return { kind: "failure", reason: "unavailable", retryable: false };
      try {
        const residentAttributes = value.attributes.map(value => { const key = (Object.keys(attributes) as (keyof typeof attributes)[]).find(key => attributes[key] === value); if (!key) throw new Error("Unknown resident attribute"); return key; });
        if (!value.activityRegion) throw new Error("Missing activity region");
        return { kind: "success", value: await data(client, "users/me", profileValue, { signal, method: "PATCH", json: { nickname: value.nickname, bio: value.bio, residentAttributes, activityRegionId: inputId(value.activityRegion.reference) } }) };
      } catch (error) { return failure(error); }
    },
    async search(search, signal) {
      try {
        // Keep the search query fixed across all cursor pages.
        const rows: unknown[] = []; let cursor: string | undefined; const seen = new Set<string>();
        do {
          const result = await client.request(query("regions", { q: search, cursor }), { signal, decode: body => { const envelope = object(body), meta = object(envelope.meta); const next = nullableText(meta.nextCursor); if (bool(meta.hasNext) !== Boolean(next)) throw new Error("Invalid regions cursor"); return { rows: array(envelope.data), next }; } });
          rows.push(...result.rows); cursor = result.next;
          if (cursor && seen.has(cursor)) throw new Error("Repeated regions cursor");
          if (cursor) seen.add(cursor);
        } while (cursor);
        return { kind: "success", value: rows.map(value => { const item = region(value); return { reference: item.id, label: item.name }; }) };
      } catch (error) { return failure(error); }
    },
    async locate() { return { kind: "unavailable" }; },
  };
  const neighborService: NeighborService = {
    async getCompletedRegions(signal) {
      return data(client, "users/me/neighbor-verifications", value => { const row = object(value); if (row.maxVerifiedRegions !== 3) throw new Error("Unexpected region limit"); const regions = array(row.verifiedRegions).map(value => { const r = object(value); timestamp(r.verifiedAt); return region(r); }); if (regions.length > 3 || new Set(regions.map(r => r.id)).size !== regions.length) throw new Error("Invalid completed regions"); return regions; }, { signal });
    },
  };
  const institutionService: InstitutionService = {
    async getQualification(signal): Promise<InstitutionQualification> {
      return data(client, "institution-verifications", value => {
        const row = object(value), subjectId = id(row.id), verification = object(row.institutionVerification);
        const status = enumValue(verification.status, ["NOT_SUBMITTED", "COMPLETED", "EXPIRED"] as const);
        if (bool(row.institutionVerified) !== bool(verification.isActive)) throw new Error("Conflicting institution state");
        if (status === "NOT_SUBMITTED") { if (row.institutionVerified || ["institutionId", "institutionName", "responsibleRegion", "completedAt", "validUntil"].some(key => verification[key] !== null)) throw new Error("Invalid absent credential"); return { subjectId, status: "none" }; }
        if (status === "EXPIRED" && row.institutionVerified) throw new Error("Expired credential active");
        return { subjectId, status: status === "COMPLETED" ? "completed" : "expired", institution: { id: id(verification.institutionId), name: text(verification.institutionName) }, responsibleRegions: [region(verification.responsibleRegion)], completedAt: timestamp(verification.completedAt), validUntil: timestamp(verification.validUntil) };
      }, { signal });
    },
  };
  const bookmarkService: BookmarkService = {
    source: "api",
    async list(_subject, signal) {
      const rows = await allPages(client, "users/me/bookmarks", signal);
      const posts = await Promise.all(rows.map(value => content.postService.getPost(id(object(value).postId), signal).catch(error => { if (error instanceof ApiError && error.status === 404) return null; throw error; })));
      return posts.filter(value => value !== null);
    },
    async get(_subject, postId, signal) {
      try { const row = await content.rawPost(postId, signal); return { postId, available: true, isBookmarked: bool(object(row.myState).isBookmarked) }; }
      catch (error) { if (error instanceof ApiError && error.status === 404) return { postId, available: false, isBookmarked: false }; throw error; }
    },
    async set(_subject, postId, isBookmarked, signal) {
      return data(client, `posts/${inputId(postId)}/bookmark`, value => { const row = object(value); if (id(row.postId) !== postId || bool(row.isBookmarked) !== isBookmarked) throw new Error("Wrong bookmark response"); return { postId, available: true, isBookmarked }; }, { signal, method: isBookmarked ? "PUT" : "DELETE" });
    },
  };
  const personalListsService: PersonalListsService = {
    source: "api",
    async list(subjectKey, signal) {
      const [owned, participated] = await Promise.all([allPages(client, "users/me/posts", signal), allPages(client, "users/me/participations", signal)]);
      const records = new Map<string, PersonalRecord>();
      for (const value of owned) { const post = postDisplay(value, false); records.set(post.id, { post, visible: true, ownedBySubjectKey: subjectKey, participation: [] }); }
      for (const value of participated) {
        const row = object(value), post = postDisplay(value, false), mine = object(row.myParticipation);
        timestamp(row.participatedAt);
        const actions: Participation[] = array(mine.reactions).map(value => { const action = enumValue(value, ["EMPATHY", "NEEDED", "CURIOUS"] as const); return action === "NEEDED" ? "NECESSARY" : action; });
        if (bool(mine.hasCommentOrReply)) actions.push("COMMENT");
        if (bool(mine.hasCommentOrReplyEvaluation)) actions.push("EVALUATION");
        if (bool(mine.hasVote)) actions.push("VOTE");
        records.set(post.id, { ...records.get(post.id), post, visible: true, participation: actions.map(action => ({ relationId: `${post.id}:${action}`, subjectKey, action, active: true })) });
      }
      return [...records.values()];
    },
  };
  const myVotesService: MyVotesService = {
    source: "api",
    async list(subjectKey, signal): Promise<MyVoteRecord[]> {
      return (await allPages(client, "users/me/votes", signal)).map(value => {
        const row = object(value), postId = id(row.postId), participatedAt = timestamp(row.participatedAt);
        if (enumValue(row.availability, ["AVAILABLE", "UNAVAILABLE"] as const) === "UNAVAILABLE") {
          if (row.post !== null || row.vote !== null) throw new Error("Unavailable vote leaked content");
          return { postId, subjectKey, participatedAt, availability: "unavailable", status: null };
        }
        const vote = object(row.vote), post = postDisplay({ ...object(row.post), vote }, false);
        if (post.type !== "VOTE" || post.id !== postId) throw new Error("Wrong personal vote");
        return { postId, subjectKey, participatedAt, availability: "available", post, snapshot: voteSnapshot(vote), timingLabel: `${timestamp(vote.endsAt)} 종료` };
      });
    },
  };
  async function officerRows(signal: AbortSignal) { return (await allPages(client, "officer/agendas", signal)).map(object); }
  async function review(postId: string, signal: AbortSignal, supplied?: Record<string, unknown>[]): Promise<AgendaReview | null> {
    const rows = supplied ?? await officerRows(signal), own = rows.find(row => id(row.postId) === postId);
    if (!own) return null;
    const [post, qualification, threads] = await Promise.all([content.postService.getPost(postId, signal), institutionService.getQualification(signal), content.commentService.list(postId, signal)]);
    if (post.type !== "LOCAL_AGENDA" || !("institution" in qualification)) throw new Error("Invalid officer source");
    const relations: AdoptionRelation[] = [];
    if (own.myInstitutionAdoption !== null) { const adoption = object(own.myInstitutionAdoption); relations.push({ id: id(adoption.id), institutionId: qualification.institution.id, institutionName: qualification.institution.name, adoptedAtLabel: timestamp(adoption.adoptedAt) }); }
    const counts = object(own.reactionCounts);
    const snapshot = reactions({ EMPATHY: counts.empathy, NEEDED: counts.needed, CURIOUS: counts.curious, total: counts.total }, []);
    return { post, regionId: id(own.regionId), relations, reactions: { empathy: snapshot.counts.EMPATHY, need: snapshot.counts.NEEDED, curious: snapshot.counts.CURIOUS }, opinions: threads.map(thread => ({ id: thread.root.id, authorLabel: thread.root.authorName, createdAtLabel: thread.root.createdAtLabel, body: thread.root.content, replies: thread.replies.map(reply => `${reply.authorName}: ${reply.content}`) })) };
  }
  const adoptionService: AdoptionService = {
    async list(signal) {
      const rows = await officerRows(signal);
      return (await Promise.all(rows.map(row => review(id(row.postId), signal, rows)))).filter(value => value !== null);
    },
    get: review,
    async setAdopted(postId, adopted, signal) {
      // Obtain cancellation identity from the private officer list, never from public names.
      const rows = await officerRows(signal), own = rows.find(row => id(row.postId) === postId);
      if (!own) throw new ApiError("unavailable", "안건을 찾을 수 없습니다.");
      if (adopted) await data(client, `posts/${inputId(postId)}/adoptions`, value => { const row = object(value); id(row.id); if (id(row.postId) !== postId) throw new Error("Wrong adoption post"); timestamp(row.adoptedAt); text(row.institutionName); }, { method: "POST", signal });
      else if (own.myInstitutionAdoption !== null) await client.request(`posts/${inputId(postId)}/adoptions/${id(object(own.myInstitutionAdoption).id)}`, { method: "DELETE", signal, decode: decodeNoContent });
      const next = await review(postId, signal);
      if (!next) throw new Error("Updated agenda unavailable");
      return next;
    },
  };
  return { profileService, neighborService, institutionService, bookmarkService, personalListsService, myVotesService, adoptionService };
}
