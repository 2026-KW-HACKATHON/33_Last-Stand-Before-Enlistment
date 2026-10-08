import type { VoteSnapshot } from "./model";
import type { VoteService } from "./service";

export type VoteMockMode = "success" | "load-error" | "submit-error" | "unknown-result" | "submitted";

function initial(mode: VoteMockMode): VoteSnapshot {
  return {
    options: [
      { id: "weekday-evening", label: "평일 저녁", count: 12 },
      { id: "saturday-morning", label: "토요일 오전", count: 8 },
      { id: "sunday-afternoon", label: "일요일 오후", count: 5 },
    ],
    submittedOptionId: mode === "submitted" ? "saturday-morning" : undefined,
  };
}

/** Development-only mock. It intentionally exposes no HTTP path or backend DTO. */
export function createVoteMockService(mode: VoteMockMode = "success"): VoteService {
  let value = initial(mode);
  let failOnce = mode === "submit-error" || mode === "unknown-result";
  return {
    async get() {
      if (mode === "load-error") throw new Error("투표 결과를 불러오지 못했습니다.");
      return value;
    },
    async submit(_postId, optionId) {
      await new Promise((resolve) => setTimeout(resolve, 350));
      if (failOnce) {
        failOnce = false;
        if (mode === "unknown-result") throw new Error("제출 결과를 확인할 수 없습니다.");
        throw new Error("투표 제출에 실패했습니다.");
      }
      const previous = value.submittedOptionId;
      value = {
        submittedOptionId: optionId,
        options: value.options.map((option) => ({
          ...option,
          count: Math.max(0, option.count + (option.id === optionId ? 1 : option.id === previous ? -1 : 0)),
        })),
      };
      return value;
    },
  };
}
