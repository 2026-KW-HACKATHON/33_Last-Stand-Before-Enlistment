import type { SummaryService } from "./service";

export type SummaryMockMode = "success" | "provider-error" | "timeout" | "quota" | "source-too-short";

export function createSummaryMockService(mode: SummaryMockMode = "success"): SummaryService {
  return {
    async getSummary(_post, signal) {
      signal?.throwIfAborted();
      await new Promise((resolve) => setTimeout(resolve, 350));
      signal?.throwIfAborted();
      if (mode === "success") return { status: "SUCCEEDED", summary: "공릉길의 야간 보행 안전을 높이기 위해 조명과 안전 시설을 보완하자는 지역 안건입니다. 주민이 안전하게 이동할 수 있는 구체적인 개선 방안을 함께 논의하자는 내용입니다. 원문에 제시된 보행 환경 개선 요청을 중심으로 의견을 모읍니다." };
      if (mode === "source-too-short") return { status: "SOURCE_TOO_SHORT", message: "원문이 짧아 요약을 만들지 않았습니다. 원문을 확인해 주세요." };
      if (mode === "timeout") throw new Error("요약 요청 시간이 초과되었습니다.");
      if (mode === "quota") throw new Error("현재 요약 요청 한도에 도달했습니다.");
      throw new Error("요약 제공자를 일시적으로 사용할 수 없습니다.");
    },
  };
}
