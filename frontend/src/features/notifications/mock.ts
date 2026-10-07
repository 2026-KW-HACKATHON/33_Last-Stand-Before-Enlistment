import type { NotificationItemModel, NotificationService, NotificationTargetResult } from "./model";

/** Development fixtures reference the existing post Mock originals, never generate events. */
export const notificationFixtures: readonly NotificationItemModel[] = [
  { id: "comment", category: "notification", typeLabel: "댓글", timeLabel: "5분 전", read: false, title: "새 댓글이 달렸어요", description: "보행로 개선 안건에 의견이 도착했습니다." },
  { id: "reminder", category: "notification", typeLabel: "투표", timeLabel: "1시간 전", read: false, title: "투표가 곧 종료돼요", description: "참여한 투표의 종료 전 알림입니다." },
  { id: "adoption-cancel", category: "notification", typeLabel: "기관", timeLabel: "어제", read: true, title: "기관 채택이 취소되었습니다", description: "해당 안건의 채택 기록을 확인해 주세요." },
  { id: "deleted", category: "notification", typeLabel: "신고", timeLabel: "어제", read: true, title: "게시물이 삭제되었습니다", description: "삭제 결과 안내를 확인해 주세요." },
  { id: "reaction", category: "notification", typeLabel: "반응", timeLabel: "방금 전", read: false, title: "새로운 반응이 달렸어요", description: "내 게시물에 새로운 반응이 추가되었습니다." },
  { id: "participation", category: "activity", typeLabel: "활동", timeLabel: "오늘", read: true, title: "지역 활동에 참여했어요", description: "참여한 지역 활동의 원본을 확인해 주세요." },
  { id: "vote-result", category: "notification", typeLabel: "투표 결과", timeLabel: "오늘", read: false, title: "투표 결과가 나왔어요", description: "참여한 투표의 결과를 확인해 보세요." },
  { id: "adoption", category: "notification", typeLabel: "기관", timeLabel: "오늘", read: false, title: "기관이 안건을 채택했습니다", description: "같은 안건의 현재 채택 정보를 확인해 주세요." },
];
export const notificationScenarios = ["success", "empty", "load-error", "read-error", "target-error", "deleted", "inaccessible", "pending"] as const;
export type NotificationScenario = typeof notificationScenarios[number];
const originalIds: Record<string, string> = { comment: "agenda-photo", reminder: "vote", "adoption-cancel": "agenda-anonymous", reaction: "agenda-photo", participation: "activity", "vote-result": "vote-ended", adoption: "agenda-photo" };
export function createMockNotificationService(scenario: NotificationScenario = "success", delay = 250): NotificationService {
  let items = notificationFixtures.map(item => ({ ...item }));
  let loadFailed = false, readFailed = false, targetFailed = false;
  const wait = (signal: AbortSignal) => new Promise<void>((resolve, reject) => {
    signal.throwIfAborted();
    const aborted = () => { clearTimeout(timer); signal.removeEventListener("abort", aborted); reject(new Error("Aborted")); };
    const timer = setTimeout(() => { signal.removeEventListener("abort", aborted); resolve(); }, delay);
    signal.addEventListener("abort", aborted, { once: true });
  });
  return {
    source: "mock",
    async load(signal) { await wait(signal); if (scenario === "load-error" && !loadFailed) { loadFailed = true; throw new Error("Mock load failed"); } return scenario === "empty" ? [] : items.map(item => ({ ...item })); },
    async markRead(id, signal) {
      await wait(signal);
      if (scenario === "pending") await new Promise<void>((_, reject) => { const abort = () => reject(new Error("Aborted")); signal.addEventListener("abort", abort, { once: true }); if (signal.aborted) abort(); });
      signal.throwIfAborted();
      if (scenario === "read-error" && !readFailed) { readFailed = true; throw new Error("Mock read failed"); }
      items = items.map(item => item.id === id ? { ...item, read: true } : item);
    },
    async resolveTarget(id, signal): Promise<NotificationTargetResult> {
      await wait(signal);
      if (scenario === "target-error" && !targetFailed) { targetFailed = true; throw new Error("Mock target failed"); }
      if (scenario === "deleted" || id === "deleted") return { status: "deleted" };
      if (scenario === "inaccessible") return { status: "inaccessible" };
      const postId = originalIds[id]; if (!postId) return { status: "inaccessible" };
      return { status: "available", target: { kind: id.startsWith("adoption") ? "adoption-record" : "post", destination: { id: "post", params: { postId } } } };
    },
  };
}
