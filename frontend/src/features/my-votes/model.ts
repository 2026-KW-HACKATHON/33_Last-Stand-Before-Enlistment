import type { VotePostDisplay } from "../post/model";
import type { VoteSnapshot } from "../vote/model";

export type VoteFilter = "ALL" | "OPEN" | "CLOSED";
export const voteFilters = { ALL: "전체", OPEN: "진행 중", CLOSED: "종료" } as const;
/** FE read port, not a wire DTO. Unavailable records deliberately contain no content. */
export type MyVoteRecord = { postId: string; subjectKey: string; participatedAt: string } & (
  | { availability: "available"; post: VotePostDisplay; snapshot: VoteSnapshot | null; timingLabel: string; reminderLabel?: string }
  | { availability: "unavailable"; status: "OPEN" | "ENDED" | null }
);
export interface MyVotesService {
  readonly source: "mock" | "api";
  list(subjectKey: string, signal: AbortSignal): Promise<readonly MyVoteRecord[]>;
}

export function selectVotes(records: readonly MyVoteRecord[], subject: string, filter: VoteFilter) {
  const latest = new Map<string, MyVoteRecord>();
  for (const row of records) if (row.subjectKey === subject) latest.set(row.postId, row);
  return [...latest.values()].filter(row => {
    const status = row.availability === "available" ? row.post.vote.status : row.status;
    return filter === "ALL" || status === (filter === "OPEN" ? "OPEN" : "ENDED");
  });
}

export function voteSummary(snapshot: VoteSnapshot | null, ended: boolean) {
  if (!snapshot) return "본인 선택과 결과 조회 대기";
  const total = snapshot.options.reduce((n, option) => n + option.count, 0);
  const percent = (count: number) => total ? Math.round(count / total * 100) : 0;
  const own = snapshot.options.find(option => option.id === snapshot.submittedOptionId);
  const max = Math.max(0, ...snapshot.options.map(option => option.count));
  const leaders = max ? snapshot.options.filter(option => option.count === max) : [];
  const result = leaders.length ? leaders.map(option => `${option.label} ${percent(option.count)}%`).join(" / ") : "집계 없음";
  return `${own ? `내 선택: ${own.label} ${percent(own.count)}%` : "본인 선택 조회 대기"} | ${ended ? "최종 결과" : "최다"}: ${result}`;
}

export function createMyVotesStore(service: MyVotesService | null, subject: string | null) {
  let state: { phase: "idle" | "loading" | "ready" | "error" | "unavailable"; records: readonly MyVoteRecord[]; filter: VoteFilter; scroll: number; visible: boolean } = { phase: "idle", records: [], filter: "ALL", scroll: 0, visible: false };
  let generation = 0;
  let controller: AbortController | null = null;
  const listeners = new Set<() => void>();
  const publish = (patch: Partial<typeof state>) => { state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  return {
    getState: () => state,
    subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    open() { publish({ visible: true }); },
    close() { generation++; controller?.abort(); publish({ visible: false, phase: state.phase === "loading" ? "idle" : state.phase }); },
    filter(filter: VoteFilter) { publish({ filter, scroll: 0 }); },
    scroll(scroll: number) { if (Number.isFinite(scroll) && scroll >= 0 && state.scroll !== scroll) publish({ scroll }); },
    async load() {
      const attempt = ++generation;
      controller?.abort(); controller = new AbortController();
      const signal = controller.signal;
      if (!service || !subject) { publish({ phase: "unavailable", records: [] }); return; }
      publish({ phase: "loading" });
      try {
        const records = await service.list(subject, signal);
        if (attempt === generation && !signal.aborted) publish({ phase: "ready", records });
      } catch { if (attempt === generation && !signal.aborted) publish({ phase: "error" }); }
    },
    dispose() { generation++; controller?.abort(); listeners.clear(); },
  };
}
export type MyVotesStore = ReturnType<typeof createMyVotesStore>;
