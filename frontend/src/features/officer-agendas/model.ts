import type { SessionState } from "../../lib/navigation";
import { institutionAccess, type InstitutionState } from "../institution/model";
import type { AdoptionDisplay, AgendaPostDisplay } from "../post/model";

/** FE ports, not wire DTOs. Private relation identity is never projected publicly. */
export type AdoptionRelation = { id: string; institutionId: string; institutionName: string; adoptedAtLabel: string };
export type OfficerAgenda = { post: AgendaPostDisplay; regionId: string; relations: readonly AdoptionRelation[] };
export type AgendaReview = OfficerAgenda & {
  reactions: { empathy: number; need: number; curious: number };
  opinions: readonly { id: string; authorLabel: string; createdAtLabel: string; body: string; replies: readonly string[] }[];
  summary?: string;
};
export interface AdoptionService {
  list(signal: AbortSignal): Promise<readonly OfficerAgenda[]>;
  get(postId: string, signal: AbortSignal): Promise<AgendaReview | null>;
  /** Desired state prevents duplicate adoption on retry; cancellation is scoped to the institution. */
  setAdopted(postId: string, adopted: boolean, signal: AbortSignal): Promise<AgendaReview>;
}
export type OfficerAuthority = { subjectId: string; institution: InstitutionState; session: SessionState };
export function canRead(authority: OfficerAuthority) { return institutionAccess(authority.institution, authority.subjectId, authority.session).status === "allowed"; }
export function ownRelation(item: OfficerAgenda, authority: OfficerAuthority) {
  const state = authority.institution;
  if (state.status !== "ready" || !("institution" in state.qualification)) return undefined;
  const institutionId = state.qualification.institution.id;
  return item.relations.find(relation => relation.institutionId === institutionId);
}
export function canChange(item: OfficerAgenda, authority: OfficerAuthority) {
  return item.post.type === "LOCAL_AGENDA" && institutionAccess(authority.institution, authority.subjectId, authority.session, item.regionId).status === "allowed";
}
export function publicAdoptions(item: OfficerAgenda): readonly AdoptionDisplay[] {
  return item.relations.map(({ institutionName, adoptedAtLabel }) => ({ institutionName, adoptedAtLabel }));
}
export function publicPost(item: OfficerAgenda): AgendaPostDisplay { return { ...item.post, adoptions: item.post.adoptions.length ? item.post.adoptions : publicAdoptions(item) }; }
export type OfficerState = {
  phase: "idle" | "loading" | "ready" | "error" | "unavailable";
  items: readonly OfficerAgenda[]; scope: "all" | "adopted"; regionId: string; scroll: number;
  detailPhase: "idle" | "loading" | "ready" | "error" | "unavailable";
  detail: AgendaReview | null; pending: boolean; feedback: string; record: boolean;
};
export function visibleAgendas(state: OfficerState, authority: OfficerAuthority) {
  return state.items.filter(item => (!state.regionId || item.regionId === state.regionId) && (state.scope === "all" || !!ownRelation(item, authority)))
    .slice().sort((a, b) => b.post.reactionCount - a.post.reactionCount);
}
function validate(item: OfficerAgenda, postId?: string) {
  if (item.post.type !== "LOCAL_AGENDA" || !item.regionId || (postId && item.post.id !== postId) || new Set(item.relations.map(r => r.institutionId)).size !== item.relations.length) throw new Error("Invalid agenda display port");
  return item;
}
export function createOfficerStore(service: AdoptionService | null, authorityReader: () => OfficerAuthority = () => ({ subjectId: "", session: { status: "loading" }, institution: { status: "idle" } })) {
  let suppliedAuthority: OfficerAuthority | null = null;
  const getAuthority = () => suppliedAuthority ?? authorityReader();
  let state: OfficerState = { phase: "idle", items: [], scope: "all", regionId: "", scroll: 0, detailPhase: "idle", detail: null, pending: false, feedback: "", record: false };
  let disposed = false, listGeneration = 0, detailGeneration = 0;
  let listController: AbortController | null = null, detailController: AbortController | null = null;
  const listeners = new Set<() => void>();
  const publish = (patch: Partial<OfficerState>) => { state = { ...state, ...patch }; listeners.forEach(fn => fn()); };
  const live = (signal: AbortSignal) => !disposed && !signal.aborted && canRead(getAuthority());
  const store = {
    getState: () => state,
    subscribe(fn: () => void) { listeners.add(fn); return () => { listeners.delete(fn); }; },
    activate() { disposed = false; },
    setAuthority(authority: OfficerAuthority) { suppliedAuthority = authority; },
    filter(scope: OfficerState["scope"], regionId: string) { publish({ scope, regionId, scroll: 0 }); },
    saveScroll(scroll: number) { publish({ scroll }); },
    showRecord(record: boolean) { publish({ record }); },
    clear() { listGeneration++; detailGeneration++; listController?.abort(); detailController?.abort(); publish({ phase: "idle", items: [], detailPhase: "idle", detail: null, pending: false, feedback: "", record: false }); },
    dispose() { disposed = true; store.clear(); listeners.clear(); },
    async load() {
      if (disposed || !canRead(getAuthority())) return;
      const generation = ++listGeneration; listController?.abort(); listController = new AbortController(); const signal = listController.signal;
      publish({ phase: service ? "loading" : "unavailable" }); if (!service) return;
      try { const items = await service.list(signal); items.forEach(item => validate(item)); if (generation === listGeneration && live(signal)) publish({ phase: "ready", items }); }
      catch { if (generation === listGeneration && live(signal)) publish({ phase: "error" }); }
    },
    async read(postId: string) {
      if (disposed || !canRead(getAuthority())) return;
      const generation = ++detailGeneration; detailController?.abort(); detailController = new AbortController(); const signal = detailController.signal;
      publish({ detailPhase: service ? "loading" : "unavailable", detail: null, pending: false, feedback: "", record: false }); if (!service) return;
      try { const detail = await service.get(postId, signal); if (detail) validate(detail, postId); if (generation === detailGeneration && live(signal)) publish({ detailPhase: detail ? "ready" : "unavailable", detail }); }
      catch { if (generation === detailGeneration && live(signal)) publish({ detailPhase: "error" }); }
    },
    async change(adopted: boolean) {
      const detail = state.detail;
      if (disposed || !service || state.pending || state.detailPhase !== "ready" || !detail || !canChange(detail, getAuthority())) return;
      if (!!ownRelation(detail, getAuthority()) === adopted) return;
      const generation = ++detailGeneration; detailController?.abort(); detailController = new AbortController(); const signal = detailController.signal;
      publish({ pending: true, feedback: "" });
      try {
        const next = await service.setAdopted(detail.post.id, adopted, signal); validate(next, detail.post.id);
        if (generation !== detailGeneration || !live(signal) || !canChange(next, getAuthority())) return;
        // A pending list response must not restore a stale relationship.
        listGeneration++; listController?.abort();
        publish({ phase: state.phase === "loading" ? "idle" : state.phase, detail: next, items: state.items.map(item => item.post.id === next.post.id ? next : item), pending: false, record: adopted, feedback: adopted ? "채택이 기록되었습니다." : "본 기관 채택을 취소했습니다." });
      } catch { if (generation === detailGeneration && live(signal)) publish({ pending: false, feedback: "저장하지 못했습니다. 기존 관계는 유지됩니다. 다시 시도해 주세요." }); }
    },
  };
  return store;
}
export type OfficerStore = ReturnType<typeof createOfficerStore>;
