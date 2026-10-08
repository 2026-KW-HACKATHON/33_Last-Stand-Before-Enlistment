import type { ActivityPostDisplay, PostDisplayModel, VotePostDisplay } from "./model";
import type { PostService } from "./service";

const memberCapabilities = {
  canEdit: false, canDelete: false, canBookmark: true, canReact: false,
  canComment: false, canVote: false, canReport: true, canShare: false,
} as const;

const base = {
  metadata: { regionName: "하계2동", topic: "환경", createdAtLabel: "2026. 10. 7." },
  author: { id: "member-01", displayName: "김이웃" },
  anonymous: false,
  images: [],
  reactionCount: 12,
  commentCount: 4,
  adoptions: [],
  capabilities: memberCapabilities,
  viewer: { mode: "member" as const },
};

/** Development/test fixtures only. They are never registered in the production app root. */
export const postFixtures: Readonly<Record<string, PostDisplayModel>> = {
  "agenda-photo": {
    ...base,
    id: "agenda-photo", type: "LOCAL_AGENDA", title: "골목길 보행로 안전을 개선해 주세요",
    content: "저녁 시간 보행자가 많은 골목길의 조명이 부족합니다. 주민이 안전하게 다닐 수 있는 개선 방안을 함께 논의하고 싶습니다.",
    images: [{ id: "photo-01", url: "https://images.unsplash.com/photo-1497250681960-ef046c08a56e?auto=format&fit=crop&w=1200&q=80", alt: "나무가 있는 산책로" }],
    referenceLink: { label: "보행 안전 참고 자료", href: "https://www.molit.go.kr/" },
    adoptions: [{ institutionName: "노원구청", adoptedAtLabel: "2026. 10. 6. 채택" }],
  },
  "agenda-anonymous": {
    ...base,
    id: "agenda-anonymous", type: "LOCAL_AGENDA", title: "어린이 보호구역 속도 안내를 보완해 주세요",
    content: "등하교 시간 차량 속도를 안내하는 표지가 더 잘 보이면 좋겠습니다.",
    anonymous: true,
    author: { id: "member-02", displayName: "실제 작성자" },
    metadata: { regionName: "월계2동", topic: "안전", createdAtLabel: "2026. 10. 6." },
  },
  activity: {
    ...base,
    id: "activity", type: "LOCAL_ACTIVITY", title: "하계천 가을 정화 활동 참가 안내",
    content: "주민과 함께 하계천 주변을 정리하는 활동입니다. 장갑과 집게는 현장에서 제공합니다.",
    metadata: { regionName: "하계1동", topic: "환경", createdAtLabel: "2026. 10. 5." },
    activity: {
      source: "노원구 자원봉사센터", scheduleLabel: "2026. 10. 12. 오전 10:00", location: "하계천 산책로 입구",
      status: "UPCOMING", contactEmail: "volunteer@example.invalid",
      participationLink: { label: "외부 참여 안내 열기", href: "https://www.nowon.kr/" },
    },
  } satisfies ActivityPostDisplay,
  "activity-ended": {
    ...base,
    id: "activity-ended", type: "LOCAL_ACTIVITY", title: "동네 독서 모임 안내",
    content: "지난 독서 모임의 활동 정보를 확인할 수 있습니다.",
    activity: { source: "하계도서관", scheduleLabel: "2026. 9. 20. 오후 2:00", location: "하계도서관", status: "ENDED" },
  } satisfies ActivityPostDisplay,
  vote: {
    ...base,
    id: "vote", type: "VOTE", title: "주말 주민 휴식 공간 운영 시간 투표",
    content: "주민 휴식 공간의 주말 운영 시간을 정하기 위한 투표입니다.",
    metadata: { regionName: "하계2동", topic: "생활", createdAtLabel: "2026. 10. 4." },
    vote: { status: "OPEN", participationLabel: "투표 진행 중" },
  } satisfies VotePostDisplay,
  "vote-ended": {
    ...base,
    id: "vote-ended", type: "VOTE", title: "주민 휴식 공간 운영 시간 결과",
    content: "투표가 종료되어 결과를 확인할 수 있습니다.",
    vote: { status: "ENDED", participationLabel: "투표 종료 · 결과 확인" },
  } satisfies VotePostDisplay,
};

export function createMockPostService(posts: Readonly<Record<string, PostDisplayModel>> = postFixtures): PostService {
  return {
    async getPost(postId, signal) {
      signal?.throwIfAborted();
      const post = posts[postId];
      if (!post) throw new Error("Mock post is unavailable");
      return post;
    },
  };
}
