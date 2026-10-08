import type { ActivityStatus, PostDisplayModel } from "../post/model";
import type { ReactionSnapshot } from "../reaction/model";
import type { VoteSnapshot } from "../vote/model";
import { array, bool, count, enumValue, id, nullableText, object, region, text, timestamp, url } from "./wire";

export const topics = { TRANSPORTATION: "교통", HOUSING: "주거", SAFETY: "안전", WELFARE: "복지", LIVING_INFORMATION: "생활정보", ENVIRONMENT: "환경", OTHER: "기타" } as const;
export function topicName(value: unknown) {
  const key = enumValue(value, Object.keys(topics) as (keyof typeof topics)[]);
  return topics[key];
}
export function topicCode(value: string): keyof typeof topics {
  const key = (Object.keys(topics) as (keyof typeof topics)[]).find(key => topics[key] === value);
  if (!key) throw new Error("Unknown topic");
  return key;
}
export const activityStates = { SCHEDULED: "UPCOMING", IN_PROGRESS: "ONGOING", ENDED: "ENDED", CANCELED: "CANCELLED" } as const;
export function activityStatus(value: unknown): ActivityStatus {
  return activityStates[enumValue(value, Object.keys(activityStates) as (keyof typeof activityStates)[])];
}
export function reactions(counts: unknown, selected: unknown): ReactionSnapshot {
  const row = object(counts);
  const result = { EMPATHY: count(row.EMPATHY), NEEDED: count(row.NEEDED), CURIOUS: count(row.CURIOUS) };
  const total = count(row.total);
  if (total !== result.EMPATHY + result.NEEDED + result.CURIOUS) throw new Error("Reaction total mismatch");
  const own = array(selected).map(v => enumValue(v, ["EMPATHY", "NEEDED", "CURIOUS"] as const));
  if (new Set(own).size !== own.length) throw new Error("Duplicate reaction");
  return { counts: result, total, selected: own };
}
export function voteSnapshot(value: unknown): VoteSnapshot {
  const row = object(value);
  enumValue(row.status, ["OPEN", "CLOSED"] as const); timestamp(row.endsAt);
  const options = array(row.options).map(value => {
    const option = object(value), percentage = option.votePercentage;
    if (typeof percentage !== "number" || !Number.isFinite(percentage) || percentage < 0 || percentage > 100) throw new Error("Invalid vote percentage");
    return { id: id(option.id), label: text(option.content), count: count(option.voteCount) };
  });
  if (options.length < 2 || options.length > 10 || new Set(options.map(o => o.id)).size !== options.length || options.reduce((sum, o) => sum + o.count, 0) !== count(row.participantCount)) throw new Error("Invalid vote options/count");
  const submittedOptionId = row.myOptionId === undefined || row.myOptionId === null ? undefined : id(row.myOptionId);
  if (submittedOptionId && !options.some(o => o.id === submittedOptionId)) throw new Error("Unknown selected option");
  return { options, submittedOptionId };
}
/** List and detail DTOs have different fields. Missing public author IDs stay absent. */
export function postDisplay(value: unknown, detail = true): PostDisplayModel {
  const row = object(value), type = enumValue(row.type, ["LOCAL_AGENDA", "LOCAL_ACTIVITY", "VOTE"] as const);
  if (detail && row.status !== "PUBLISHED") throw new Error("Unpublished detail");
  const place = region(row.region), author = object(row.author);
  const grants = row.capabilities === undefined && !detail ? {} : object(row.capabilities);
  const grant = (key: string) => grants[key] === undefined && !detail ? false : bool(grants[key]);
  const member = detail ? row.myState !== undefined : true;
  if (detail && member) { const own = object(row.myState); reactions(row.reactionCounts, own.reactions); bool(own.isBookmarked); }
  const counts = reactions(row.reactionCounts, []);
  const images = detail ? array(row.images).map(value => {
    const image = object(value);
    count(image.sizeBytes); enumValue(image.contentType, ["image/jpeg", "image/png"] as const);
    if (image.fileId !== undefined) id(image.fileId);
    return { id: id(image.photoId), url: url(image.url), alt: text(row.title) };
  }) : row.thumbnailUrl === null ? [] : [{ id: `thumbnail:${id(row.id)}`, url: url(row.thumbnailUrl), alt: text(row.title) }];
  const base = {
    id: id(row.id), title: text(row.title), content: text(detail ? row.content : row.excerpt),
    metadata: { regionId: place.id, regionName: place.name, topic: topicName(row.topic), createdAtLabel: timestamp(row.createdAt), updatedAtLabel: timestamp(row.updatedAt) },
    author: { displayName: text(author.displayName), ...(bool(author.institutionVerified) ? { badge: "institution" as const } : {}) },
    anonymous: false, images, reactionCount: counts.total, commentCount: count(row.commentCount),
    adoptions: detail ? array(row.adoptions).map(value => { const a = object(value); return { institutionName: text(a.institutionName), adoptedAtLabel: timestamp(a.adoptedAt) }; }) : [],
    capabilities: { canEdit: grant("canEdit"), canDelete: grant("canDelete"), canBookmark: detail && grant("canBookmark"), canReact: detail && grant("canReact"), canEvaluateComment: detail && grant("canEvaluateComment"), canComment: detail && grant("canComment"), canVote: detail && grant("canVote"), canShare: member, canReport: false },
    viewer: { mode: member ? "member" as const : "guest" as const, ...(detail && member && !grant("canReact") ? { participationRestriction: "이 지역의 이웃 완료 자격이 있어야 참여할 수 있습니다." } : {}) },
  };
  if (type === "LOCAL_AGENDA") return { ...base, type };
  if (type === "LOCAL_ACTIVITY") {
    const activity = detail ? object(row.activity) : null;
    const link = activity ? nullableText(activity.externalParticipationUrl) : undefined;
    if (activity) bool(activity.externalParticipationEnabled);
    return { ...base, type, activity: { source: activity ? text(activity.source) : "", scheduleLabel: activity ? text(activity.schedule) : "", location: activity ? text(activity.place) : "", status: activityStatus(activity ? activity.status : row.activityStatus), contactEmail: activity ? nullableText(activity.organizerEmail) : undefined, ...(link && activity && bool(activity.externalParticipationEnabled) ? { participationLink: { label: "외부 참여", href: url(link) } } : {}) } };
  }
  const vote = object(row.vote); voteSnapshot(vote);
  return { ...base, type, vote: { status: enumValue(vote.status, ["OPEN", "CLOSED"] as const) === "OPEN" ? "OPEN" : "ENDED", participationLabel: `${count(vote.participantCount)}명 참여 · ${timestamp(vote.endsAt)} 종료` } };
}
