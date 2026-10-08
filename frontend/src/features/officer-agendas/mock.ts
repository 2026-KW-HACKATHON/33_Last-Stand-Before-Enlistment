import { mockQualification, mockInstitutionSession, mockSubjectId, mockResponsibleRegion, type InstitutionScenario } from "../institution/mock";
import { postFixtures } from "../post/mock";
import { canChange, canRead, publicPost, type AdoptionRelation, type AdoptionService, type AgendaReview, type OfficerAuthority } from "./model";

export type OfficerScenario = "normal" | "empty" | "loading" | "error" | "save-error" | "detail-error" | "deleted";
export function mockOfficerAuthority(scenario: InstitutionScenario = "valid"): OfficerAuthority {
  return { subjectId: mockSubjectId, session: mockInstitutionSession(scenario), institution: { status: "ready", qualification: mockQualification(scenario), checkedAt: Date.parse("2026-10-08T00:00:00Z") } };
}
/** One original and independent relationship collection per mock service instance. No production registration. */
export function createMockAdoptionService(getAuthority: () => OfficerAuthority = mockOfficerAuthority, initial: OfficerScenario = "normal", delay = 150) {
  let scenario = initial, counter = 0;
  const canceled: AdoptionRelation[] = [];
  const originals = [postFixtures["agenda-photo"], postFixtures["agenda-anonymous"]].map((post, index): AgendaReview => {
    if (post.type !== "LOCAL_AGENDA") throw new Error("Agenda fixture required");
    return {
      post: { ...post, images: [], metadata: { ...post.metadata, regionName: index ? "하계2동" : mockResponsibleRegion.name }, reactionCount: index ? 21 : 32, commentCount: 2, adoptions: [] },
      regionId: index ? "mock-hagye2" : mockResponsibleRegion.id,
      relations: [{ id: `other-${index}`, institutionId: "mock-other-office", institutionName: "다른 기관", adoptedAtLabel: "2026.10.05 13:00" }],
      reactions: index ? { empathy: 12, need: 7, curious: 2 } : { empathy: 20, need: 10, curious: 2 },
      opinions: [{ id: `opinion-${index}`, authorLabel: "동네이웃", createdAtLabel: "10월 5일", body: "출퇴근 시간에 보행자가 몰려 위험합니다. 보행 환경 개선이 필요합니다.", replies: ["조명도 함께 개선되면 좋겠습니다."] }],
      summary: "주민이 보행 환경 개선을 제안했습니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다.",
    };
  });
  const clone = (item: AgendaReview): AgendaReview => structuredClone({ ...item, post: publicPost(item) });
  async function wait(signal: AbortSignal) {
    await new Promise<void>((resolve, reject) => {
      if (signal.aborted) { reject(new Error("Aborted")); return; }
      const abort = () => { clearTimeout(timer); reject(new Error("Aborted")); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, scenario === "loading" ? 60000 : delay);
      signal.addEventListener("abort", abort, { once: true });
    });
    if (!canRead(getAuthority())) throw new Error("Forbidden");
  }
  const service: AdoptionService = {
    async list(signal) { await wait(signal); if (scenario === "error") throw new Error("Mock query failure"); return scenario === "empty" || scenario === "deleted" ? [] : originals.map(clone); },
    async get(postId, signal) { await wait(signal); if (scenario === "detail-error") throw new Error("Mock detail failure"); const item = originals.find(row => row.post.id === postId); return item && scenario !== "deleted" ? clone(item) : null; },
    async setAdopted(postId, adopted, signal) {
      await wait(signal); const item = originals.find(row => row.post.id === postId); const authority = getAuthority();
      if (!item || scenario === "deleted" || !canChange(item, authority)) throw new Error("Forbidden");
      if (scenario === "save-error") throw new Error("Mock save failure");
      const qualification = authority.institution.status === "ready" ? authority.institution.qualification : null;
      if (!qualification || !("institution" in qualification)) throw new Error("No institution");
      const institution = qualification.institution;
      const own = item.relations.find(relation => relation.institutionId === institution.id);
      if (adopted && !own) item.relations = [...item.relations, { id: `mock-adoption-${++counter}`, institutionId: institution.id, institutionName: institution.name, adoptedAtLabel: "2026.10.08 09:00" }];
      if (!adopted && own) { canceled.push(own); item.relations = item.relations.filter(relation => relation.id !== own.id); }
      return clone(item);
    },
  };
  return { service, scenario(next: OfficerScenario) { scenario = next; }, canceled: () => structuredClone(canceled) };
}
