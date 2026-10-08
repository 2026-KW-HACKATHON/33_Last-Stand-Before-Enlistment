import { postFixtures } from "../post/mock";
import { createVoteMockService } from "../vote/mock";
import type { VoteService } from "../vote/service";
import type { VotePostDisplay } from "../post/model";
import type { MyVoteRecord, MyVotesService } from "./model";

export type MyVotesScenario = "normal" | "empty" | "error" | "unknown";
export const mockVoteSubject = "member-02";
/** One original and FE2 vote service per post: list and detail read the same choices/counts. */
export function createMyVotesMock(scenario: MyVotesScenario = "normal") {
  const votes = new Map<string, VoteService>(["vote", "vote-ended"].map(id => [id, createVoteMockService("submitted")]));
  const hidden = new Set<string>(["deleted-vote"]);
  let failOnce = scenario === "error";
  async function records(subject: string): Promise<MyVoteRecord[]> {
    if (subject !== mockVoteSubject || scenario === "empty") return [];
    const rows: MyVoteRecord[] = [];
    for (const id of ["vote", "vote-ended", "deleted-vote"]) {
      const history = { postId: id, subjectKey: subject, participatedAt: "2026. 10. 7. 10:00" };
      if (hidden.has(id)) { rows.push({ ...history, availability: "unavailable", status: id === "vote-ended" ? "ENDED" : id === "vote" ? "OPEN" : null }); continue; }
      const post = postFixtures[id] as VotePostDisplay;
      rows.push({ ...history, availability: "available", post, snapshot: scenario === "unknown" ? null : await votes.get(id)!.get(id), timingLabel: post.vote.status === "OPEN" ? "남은 2일 · 종료 2026. 10. 10. 10:00 (Mock)" : "종료 2026. 10. 7. 10:00 (Mock)" });
    }
    return rows;
  }
  const service: MyVotesService = { source: "mock", async list(subject, signal) {
    await new Promise<void>((resolve, reject) => {
      signal.throwIfAborted();
      const abort = () => { clearTimeout(timer); reject(signal.reason); };
      const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, 150);
      signal.addEventListener("abort", abort, { once: true });
    });
    signal.throwIfAborted();
    if (failOnce) { failOnce = false; throw new Error("Mock list failure"); }
    return records(subject);
  } };
  return { service, records, voteService: (postId: string) => votes.get(postId), hide: (postId: string) => { hidden.add(postId); } };
}
