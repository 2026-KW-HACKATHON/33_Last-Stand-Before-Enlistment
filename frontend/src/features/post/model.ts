/**
 * FE2 display contract. This intentionally is not a Spring/API DTO.
 * An integration adapter maps an agreed response into this shape later.
 */
export type PostType = "LOCAL_AGENDA" | "LOCAL_ACTIVITY" | "VOTE";
export type ActivityStatus = "UPCOMING" | "ONGOING" | "ENDED" | "CANCELLED";
export type VoteStatus = "OPEN" | "ENDED";

export type PostImage = { id: string; url: string; alt: string };
export type AuthorDisplay = {
  id?: string;
  displayName: string;
  institutionName?: string;
  badge?: "institution";
};
export type PostCapabilities = {
  canEdit: boolean;
  canDelete: boolean;
  canBookmark: boolean;
  canReact: boolean;
  canEvaluateComment?: boolean;
  canComment: boolean;
  canVote: boolean;
  canReport: boolean;
  canShare: boolean;
};
export type AdoptionDisplay = { institutionName: string; adoptedAtLabel: string };
export type PostViewerState = {
  mode: "member" | "guest";
  /** Reading may remain possible while a regional participation action is restricted. */
  participationRestriction?: string;
};
export type PostMetadata = {
  regionId?: string;
  regionName: string;
  topic: string;
  createdAtLabel: string;
  updatedAtLabel?: string;
};

type PostBase = {
  id: string;
  title: string;
  content: string;
  metadata: PostMetadata;
  /** Keep the actual author relationship even when public display is anonymous. */
  author: AuthorDisplay;
  anonymous: boolean;
  images: readonly PostImage[];
  referenceLink?: { label: string; href: string };
  reactionCount: number;
  commentCount: number;
  adoptions: readonly AdoptionDisplay[];
  capabilities: PostCapabilities;
  viewer: PostViewerState;
};

export type AgendaPostDisplay = PostBase & {
  type: "LOCAL_AGENDA";
};
export type ActivityPostDisplay = PostBase & {
  type: "LOCAL_ACTIVITY";
  activity: {
    source: string;
    scheduleLabel: string;
    location: string;
    status: ActivityStatus;
    contactEmail?: string;
    participationLink?: { label: string; href: string };
  };
};
export type VotePostDisplay = PostBase & {
  type: "VOTE";
  vote: {
    status: VoteStatus;
    participationLabel: string;
  };
};

export type PostDisplayModel = AgendaPostDisplay | ActivityPostDisplay | VotePostDisplay;

export type PostDetailState =
  | { kind: "loading" }
  | { kind: "success"; post: PostDisplayModel }
  | { kind: "restricted"; post: PostDisplayModel; message: string }
  | { kind: "error"; message: string; onRetry?: () => void }
  | { kind: "unavailable"; message?: string };

export function postTypeLabel(type: PostType): string {
  return { LOCAL_AGENDA: "지역 안건", LOCAL_ACTIVITY: "지역 활동 정보", VOTE: "투표" }[type];
}

export function activityStatusLabel(status: ActivityStatus): string {
  return { UPCOMING: "예정", ONGOING: "진행", ENDED: "종료", CANCELLED: "취소" }[status];
}

/** Anonymous public text is valid only for a regular member's local agenda. */
export function authorLabel(post: PostDisplayModel): string {
  return post.type === "LOCAL_AGENDA" && post.anonymous ? "익명" : post.author.displayName;
}

export function isParticipationLinkAvailable(post: ActivityPostDisplay): boolean {
  return Boolean(post.activity.participationLink) && !["ENDED", "CANCELLED"].includes(post.activity.status);
}
