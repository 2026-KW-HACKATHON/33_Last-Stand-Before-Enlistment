import type { ApiClient } from "../../lib/api/types";
import { decodeNoContent } from "../../lib/api/response";
import type { PostService } from "../post/service";
import type { CommentDisplay, CommentThread } from "../comment/model";
import type { CommentService } from "../comment/service";
import type { EvaluationService } from "../comment/evaluation-service";
import type { EvaluationSnapshot } from "../comment/evaluation";
import type { ReactionService } from "../reaction/service";
import type { VoteService } from "../vote/service";
import type { SummaryService } from "../summary/service";
import type { ShareService } from "../share/service";
import type { ExploreService } from "../explore/service";
import type { MapService } from "../map/service";
import { array, allPages, bool, count, data, enumValue, id, inputId, object, page, query, region, text, timestamp, url } from "./wire";
import { postDisplay, reactions, topicCode, voteSnapshot } from "./post-decoder";

export function commentDisplay(value: unknown): CommentDisplay {
  const row = object(value), author = object(row.author);
  bool(author.isGuest); bool(author.institutionVerified);
  const at = timestamp(row.createdAt);
  const reply = row.replyTo === null ? null : object(row.replyTo);
  if (reply) id(reply.commentId);
  return { id: id(row.id), postId: id(row.postId), content: text(row.content), likeCount: count(row.likeCount), dislikeCount: count(row.dislikeCount), authorName: text(author.displayName), createdAtLabel: at, createdAtOrder: Date.parse(at), parentCommentId: row.parentCommentId === null ? undefined : id(row.parentCommentId), targetAuthorName: reply ? text(reply.displayName) : undefined };
}
function evaluation(value: unknown, fromComment = false): EvaluationSnapshot {
  const row = object(value);
  // Omitted guest selection is never passed to the member evaluation service.
  const selected = row.myEvaluation === null ? "NONE" : enumValue(row.myEvaluation, ["LIKE", "DISLIKE"] as const);
  return { commentId: id(fromComment ? row.id : row.commentId), selected, likeCount: count(row.likeCount), dislikeCount: count(row.dislikeCount) };
}
export function createContentServices(client: ApiClient, options: { mapRegionIds?: (viewport: import("../map/model").MapViewport) => readonly string[]; writeClipboard?: (value: string) => Promise<void>; mapRenderer?: import("../map/MapRenderer").MapRenderer; loadBoundaries?: (signal: AbortSignal) => Promise<unknown> } = {}) {
  const rawPost = (postId: string, signal?: AbortSignal) => data(client, `posts/${inputId(postId)}`, value => { const row = object(value); postDisplay(row); if (id(row.id) !== postId) throw new Error("Wrong post"); return row; }, { signal });
  const postService: PostService = { async getPost(postId, signal) { return postDisplay(await rawPost(postId, signal)); } };
  const commentService: CommentService = {
    async list(postId, signal) {
      const rows = await allPages(client, `posts/${inputId(postId)}/comments`, signal);
      return rows.map(value => {
        const row = object(value), root = commentDisplay(row);
        if (root.postId !== postId || root.parentCommentId) throw new Error("Wrong root comment");
        const replies = array(row.replies).map(commentDisplay);
        if (replies.some(reply => reply.postId !== postId || reply.parentCommentId !== root.id)) throw new Error("Wrong reply parent");
        return { root, replies } satisfies CommentThread;
      });
    },
    async create(postId, input) {
      return data(client, `posts/${inputId(postId)}/comments`, value => { const result = commentDisplay(value); if (result.postId !== postId || result.parentCommentId) throw new Error("Wrong created comment"); return result; }, { method: "POST", json: { content: input.content } });
    },
    async createReply(postId, input) {
      inputId(postId);
      return data(client, `comments/${inputId(input.parentCommentId)}/replies`, value => { const result = commentDisplay(value); if (result.postId !== postId || !result.parentCommentId) throw new Error("Wrong reply"); return result; }, { method: "POST", json: { content: input.content, ...(input.replyToCommentId ? { replyToCommentId: inputId(input.replyToCommentId) } : {}) } });
    },
  };
  const evaluationService: EvaluationService = {
    async list(postId, signal) {
      const rows = await allPages(client, `posts/${inputId(postId)}/comments`, signal);
      return rows.flatMap(value => { const row = object(value); return [row, ...array(row.replies)].map(value => { if (commentDisplay(value).postId !== postId) throw new Error("Wrong evaluation post"); return evaluation(value, true); }); });
    },
    async set(commentId, selected) {
      return data(client, `comments/${inputId(commentId)}/evaluation`, value => { const result = evaluation(value); if (result.commentId !== commentId) throw new Error("Wrong evaluation"); return result; }, selected === "NONE" ? { method: "DELETE" } : { method: "PUT", json: { type: selected } });
    },
  };
  const reactionService: ReactionService = {
    async get(postId) { const row = await rawPost(postId); return reactions(row.reactionCounts, object(row.myState).reactions); },
    async set(postId, type, selected) {
      enumValue(type, ["EMPATHY", "NEEDED", "CURIOUS"] as const);
      return data(client, `posts/${inputId(postId)}/reactions/${type}`, value => { const row = object(value); if (id(row.postId) !== postId) throw new Error("Wrong reaction post"); return reactions(row.reactionCounts, row.myReactions); }, { method: selected ? "PUT" : "DELETE" });
    },
  };
  const voteService: VoteService = {
    async get(postId) { return voteSnapshot((await rawPost(postId)).vote); },
    async submit(postId, optionId) {
      return data(client, `posts/${inputId(postId)}/vote`, value => { const row = object(value); if (id(row.postId) !== postId) throw new Error("Wrong vote post"); return voteSnapshot(row); }, { method: "PUT", json: { optionId: inputId(optionId) } });
    },
  };
  const summaryService: SummaryService = {
    async getSummary(post, signal) {
      return data(client, `posts/${inputId(post.id)}/summary`, value => {
        const row = object(value); if (id(row.postId) !== post.id) throw new Error("Wrong summary");
        const status = enumValue(row.status, ["PENDING", "SUCCEEDED", "SOURCE_TOO_SHORT", "FAILED"] as const);
        if (status === "SUCCEEDED") return { status, summary: text(row.summary) };
        return { status: status === "SOURCE_TOO_SHORT" ? status : "FAILED", message: status === "PENDING" ? "요약 생성 중입니다. 원문을 확인하거나 다시 조회해 주세요." : status === "SOURCE_TOO_SHORT" ? "원문이 짧아 요약 대신 원문을 표시합니다." : "요약을 불러오지 못했습니다. 원문을 확인해 주세요." };
      }, { signal });
    },
  };
  const shareService: ShareService = {
    async copy(postId) {
      const link = await data(client, `posts/${inputId(postId)}/share-link`, value => { const row = object(value); if (id(row.postId) !== postId) throw new Error("Wrong share post"); return url(row.shareUrl); });
      await (options.writeClipboard ?? (value => navigator.clipboard.writeText(value)))(link);
    },
  };
  const exploreService: ExploreService = {
    async getHome(context) {
      return data(client, query("home", { regionId: String(inputId(context.currentExploreRegion.id)) }), value => {
        const row = object(value); region(row.region); const counts = object(row.boardCounts);
        for (const type of ["LOCAL_AGENDA", "LOCAL_ACTIVITY", "VOTE"]) count(counts[type]);
        return { regionPosts: array(row.posts).map(value => postDisplay(value, false)), openVotes: array(row.openVotes).map(value => { const post = postDisplay(value, false); if (post.type !== "VOTE" || post.vote.status !== "OPEN") throw new Error("Wrong open vote"); return post; }) };
      });
    },
    async getBoard(filters, cursor) {
      const result = await client.request(query("posts", { regionId: String(inputId(filters.regionId)), type: filters.type === "ALL" ? undefined : filters.type, topic: filters.topic === "ALL" ? undefined : topicCode(filters.topic), cursor }), { decode: body => { const result = page(body); return { posts: result.rows.map(value => postDisplay(value, false)), pageInfo: { nextCursor: result.nextCursor } }; } });
      return result;
    },
  };
  const mapService: MapService = {
    async getMap(viewport, signal) {
      const ids = options.mapRegionIds?.(viewport) ?? viewport.regionIds;
      if (!ids?.length || new Set(ids).size !== ids.length) throw new Error("실제 지도 viewport 지역 ID 연결이 필요합니다.");
      const result = await data(client, query("map/dongs", { centerRegionId: String(inputId(viewport.centerRegionId)), regionIds: ids.map(inputId).join(",") }), value => {
        const row = object(value);
        return { centerRegion: region(row.centerRegion), rows: array(row.dongs).map(value => { const dong = object(value); return { region: region(dong.region), postId: dong.representativePost === null ? null : id(object(dong.representativePost).id) }; }) };
      }, { signal });
      if (result.centerRegion.id !== viewport.centerRegionId || result.rows.length !== ids.length || result.rows.some((row, index) => row.region.id !== ids[index])) throw new Error("Wrong map regions");
      // The map wire is a minimal card, never invent a full detail from missing fields.
      const dongs = await Promise.all(result.rows.map(async row => ({ region: row.region, representativePost: row.postId === null ? null : await postService.getPost(row.postId, signal) })));
      return { centerRegion: result.centerRegion, dongs };
    },
  };
  return { postService, commentService, evaluationService, reactionService, voteService, summaryService, shareService, exploreService, mapService, rawPost };
}
export { decodeNoContent };
