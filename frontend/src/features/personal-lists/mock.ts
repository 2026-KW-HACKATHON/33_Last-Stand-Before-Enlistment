import { postFixtures } from "../post/mock";
import type { Participation, PersonalListsService, PersonalRecord } from "./model";
export const mockSubject = "member-02";
export function createMockPersonalListsService(scenario: "normal" | "empty" | "error" = "normal") {
 let fail = scenario === "error";
 const records: PersonalRecord[] = Object.values(postFixtures).map(post => ({ post: { ...post, author: { ...post.author, id: mockSubject }, images: [] }, visible: true, participation: [{ relationId: post.id + "-participation", subjectKey: mockSubject, action: post.type === "VOTE" ? "VOTE" : "COMMENT", active: true }], ...(post.type === "VOTE" ? { voteSummary: post.vote.status === "OPEN" ? "내 선택: 오후 8시 · 80명 · 남은 2일" : "내 선택: 오후 8시 · 최종 결과 80명" } : {}) }));
 records[1].participation = [...records[1].participation, ...(["EMPATHY", "NECESSARY", "CURIOUS", "EVALUATION"] as const).map(action => ({ relationId: "agenda-anonymous-" + action, subjectKey: mockSubject, action, active: true }))];
 const service: PersonalListsService = { source: "mock", async list(subject, signal) { await new Promise<void>((resolve, reject) => { const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, 150); function abort() { clearTimeout(timer); reject(new Error("aborted")); } signal.addEventListener("abort", abort, { once: true }); if (signal.aborted) abort(); }); signal.throwIfAborted(); if (fail) { fail = false; throw new Error("Mock retry"); } return scenario === "empty" || subject !== mockSubject ? [] : structuredClone(records); } };
 return { service, records, cancelAction(postId: string, action: Participation) { for (const row of records.filter(r => r.post.id === postId)) row.participation = row.participation.filter(p => p.action !== action || p.subjectKey !== mockSubject); }, hide(postId: string) { for (const row of records.filter(r => r.post.id === postId)) row.visible = false; } };
}
