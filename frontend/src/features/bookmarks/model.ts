import type { PostDisplayModel, PostType } from "../post/model";

export const bookmarkTypes = { ALL: "전체", LOCAL_AGENDA: "지역 안건", LOCAL_ACTIVITY: "지역 활동 정보", VOTE: "투표" } as const;
export const bookmarkTopics = ["전체", "교통", "주거", "안전", "복지", "생활정보", "환경", "기타"] as const;
export type BookmarkType = "ALL" | PostType;
export type BookmarkTopic = typeof bookmarkTopics[number];
export type BookmarkOrigin = "me" | "settings";
export type BookmarkState = { postId: string; available: boolean; isBookmarked: boolean };
/** FE1 port using FE2's existing display model, not an API DTO or an endpoint agreement. */
export interface BookmarkService {
  readonly source: "mock" | "api";
  list(subjectKey: string, signal: AbortSignal): Promise<readonly PostDisplayModel[]>;
  get(subjectKey: string, postId: string, signal: AbortSignal): Promise<BookmarkState>;
  set(subjectKey: string, postId: string, isBookmarked: boolean, signal: AbortSignal): Promise<BookmarkState>;
}
export type BookmarkDetailState = {
  phase: "loading" | "ready" | "error" | "unavailable";
  value?: BookmarkState; pending: boolean; feedback: string; saved: boolean;
};
export type BookmarksState = {
  phase: "idle" | "loading" | "ready" | "error" | "unavailable";
  records: readonly PostDisplayModel[]; type: BookmarkType; topic: BookmarkTopic;
  origin: BookmarkOrigin; scroll: number; visible: boolean;
  details: Readonly<Record<string, BookmarkDetailState>>;
};
export function selectBookmarks(state: Pick<BookmarksState, "records" | "type" | "topic">) {
  return state.records.filter(post => (state.type === "ALL" || post.type === state.type) &&
    (state.topic === "전체" || (post.metadata.topic === "생활" ? "생활정보" : post.metadata.topic) === state.topic));
}
export function createBookmarksStore(service: BookmarkService | null, subjectKey: string | null) {
  let state: BookmarksState = { phase: "idle", records: [], type: "ALL", topic: "전체", origin: "me", scroll: 0, visible: false, details: {} };
  let disposed = false;
  let listRequest: AbortController | null = null;
  const requests = new Map<string, AbortController>();
  const listeners = new Set<() => void>();
  const update = (patch: Partial<BookmarksState>) => { if (disposed) return; state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  const detail = (id: string, patch: BookmarkDetailState) => update({ details: { ...state.details, [id]: patch } });
  async function load() {
    if (disposed) return;
    listRequest?.abort();
    if (!service || !subjectKey) { update({ phase: "unavailable", records: [] }); return; }
    const request = new AbortController(); listRequest = request;
    update({ phase: "loading" });
    try {
      const records = await service.list(subjectKey, request.signal);
      if (disposed || request.signal.aborted) return;
      update({ phase: "ready", records: [...new Map(records.map(post => [post.id, post])).values()] });
    } catch { if (!disposed && !request.signal.aborted) update({ phase: "error", records: [] }); }
  }
  async function read(postId: string) {
    if (disposed) return;
    if (state.details[postId]?.pending) return;
    requests.get(postId)?.abort();
    if (!service || !subjectKey) { detail(postId, { phase: "unavailable", pending: false, feedback: "Bookmark Service 연결 대기 · 실 API 연동 대기", saved: false }); return; }
    const request = new AbortController(); requests.set(postId, request);
    detail(postId, { phase: "loading", pending: false, feedback: "북마크 상태 확인 중", saved: false });
    try {
      const value = await service.get(subjectKey, postId, request.signal);
      if (disposed || request.signal.aborted) return;
      detail(postId, { phase: value.available ? "ready" : "unavailable", value, pending: false, feedback: value.available ? "" : "삭제되었거나 접근할 수 없는 게시물입니다.", saved: false });
      if (!value.available) update({ records: state.records.filter(post => post.id !== postId) });
    } catch { if (!disposed && !request.signal.aborted) detail(postId, { phase: "error", pending: false, feedback: "북마크 상태를 확인하지 못했습니다.", saved: false }); }
  }
  async function toggle(postId: string) {
    if (disposed) return;
    const old = state.details[postId];
    if (!service || !subjectKey || old?.phase !== "ready" || old.pending || !old.value?.available) return;
    const request = new AbortController(); requests.get(postId)?.abort(); requests.set(postId, request);
    detail(postId, { ...old, pending: true, saved: false, feedback: "처리 중" });
    try {
      const value = await service.set(subjectKey, postId, !old.value.isBookmarked, request.signal);
      if (disposed || request.signal.aborted) return;
      detail(postId, { phase: value.available ? "ready" : "unavailable", value, pending: false, saved: value.available && value.isBookmarked, feedback: !value.available ? "삭제되었거나 접근할 수 없는 게시물입니다." : value.isBookmarked ? "저장되었습니다" : "북마크를 해제했습니다." });
      if (!value.available || !value.isBookmarked) update({ records: state.records.filter(post => post.id !== postId) });
      await load();
    } catch { if (!disposed && !request.signal.aborted) detail(postId, { ...old, pending: false, saved: false, feedback: "저장/해제에 실패했습니다. 기존 상태를 유지했습니다. 다시 시도해 주세요." }); }
  }
  return {
    // Match the existing logout Provider lifecycle: React's development effect replay may reattach.
    activate: () => { disposed = false; },
    getState: () => state,
    subscribe: (fn: () => void) => { listeners.add(fn); return () => { listeners.delete(fn); }; },
    load, read, toggle,
    open: (origin?: BookmarkOrigin) => update({ visible: true, ...(origin ? { origin } : {}) }),
    close: () => update({ visible: false }),
    filter: (type: BookmarkType, topic: BookmarkTopic) => update({ type, topic, scroll: 0 }),
    scroll: (scroll: number) => update({ scroll }),
    dismissSaved: (postId: string) => { const old = state.details[postId]; if (old) detail(postId, { ...old, saved: false }); },
    dispose: () => { disposed = true; listRequest?.abort(); requests.forEach(request => request.abort()); listeners.clear(); },
  };
}
export type BookmarksStore = ReturnType<typeof createBookmarksStore>;
