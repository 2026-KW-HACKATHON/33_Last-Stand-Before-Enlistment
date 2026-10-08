import type { PostDisplayModel, PostType } from "../post/model";
export type ListKind = "myPosts" | "participations";
export type TypeFilter = "ALL" | PostType;
export const participationLabels = { EMPATHY: "공감해요", NECESSARY: "필요해요", CURIOUS: "궁금해요", COMMENT: "댓글 작성", EVALUATION: "댓글 좋아요·싫어요", VOTE: "투표 참여" } as const;
export type Participation = keyof typeof participationLabels;
/** FE display relation, not a server DTO. Adapter must supply current, subject-scoped relations. */
export type PersonalRecord = { post: PostDisplayModel; visible: boolean; participation: readonly { relationId: string; subjectKey: string; action: Participation; active: boolean }[]; voteSummary?: string };
export type PersonalItem = { post: PostDisplayModel; actions: readonly Participation[]; voteSummary?: string };
export interface PersonalListsService { readonly source: "mock" | "api"; list(subjectKey: string, signal: AbortSignal): Promise<readonly PersonalRecord[]>; }
export function selectItems(records: readonly PersonalRecord[], subject: string, kind: ListKind, filter: TypeFilter): PersonalItem[] {
 const groups = new Map<string, PersonalRecord[]>();
 for (const record of records) groups.set(record.post.id, [...(groups.get(record.post.id) ?? []), record]);
 return [...groups.values()].flatMap(group => {
  const last = group[group.length - 1];
  if (!last.visible || (filter !== "ALL" && last.post.type !== filter)) return [];
  const relations = new Map<string, PersonalRecord["participation"][number]>();
  for (const row of group) for (const relation of row.participation) if (relation.subjectKey === subject) relations.set(relation.relationId, relation);
  const actions = [...new Set([...relations.values()].filter(p => p.active).map(p => p.action))];
  if (kind === "myPosts" ? last.post.author.id !== subject : !actions.length) return [];
  return [{ post: last.post, actions, ...(actions.includes("VOTE") && last.voteSummary ? { voteSummary: last.voteSummary } : {}) }];
 });
}
export function createPersonalListsStore(service: PersonalListsService | null, subjectKey: string | null) {
 type View = { filter: TypeFilter; scroll: number };
 let state: { phase: "idle" | "loading" | "ready" | "error" | "unavailable"; records: readonly PersonalRecord[]; kind: ListKind; origin: "me" | "settings"; visible: boolean; views: Record<ListKind, View> } = { phase: "idle", records: [], kind: "myPosts", origin: "me", visible: false, views: { myPosts: { filter: "ALL", scroll: 0 }, participations: { filter: "ALL", scroll: 0 } } };
 const listeners = new Set<() => void>(); let version = 0; let controller: AbortController | null = null;
 const publish = (next: typeof state) => { state = next; listeners.forEach(fn => fn()); };
 return { getState: () => state, subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
 open(kind: ListKind, origin: "me" | "settings") { publish({ ...state, kind, origin, visible: true }); },
 close() { version++; controller?.abort(); publish({ ...state, visible: false, phase: state.phase === "loading" ? "idle" : state.phase }); },
 filter(filter: TypeFilter) { publish({ ...state, views: { ...state.views, [state.kind]: { filter, scroll: 0 } } }); },
 scroll(scroll: number) { if (state.views[state.kind].scroll !== scroll) publish({ ...state, views: { ...state.views, [state.kind]: { ...state.views[state.kind], scroll } } }); },
 async load() { const attempt = ++version; controller?.abort(); controller = new AbortController(); if (!service || !subjectKey) { publish({ ...state, phase: "unavailable", records: [] }); return; }
 publish({ ...state, phase: "loading" }); try { const records = await service.list(subjectKey, controller.signal); if (attempt === version) publish({ ...state, records, phase: "ready" }); } catch { if (attempt === version) publish({ ...state, phase: "error" }); } },
 dispose() { version++; controller?.abort(); listeners.clear(); }
 };
}
export type PersonalListsStore = ReturnType<typeof createPersonalListsStore>;
