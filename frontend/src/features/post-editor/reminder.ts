/** FE-only boundary for the optional vote reminder. It is not a backend DTO. */
export type VoteReminderInput = {
  enabled: boolean;
  remindAt: string;
  hadExistingReminder: boolean;
  endsAtChanged: boolean;
};
export type VoteReminderResult = { saved: true };
export type VoteReminderService = { save(input: VoteReminderInput): Promise<VoteReminderResult> };
export type ReminderMockMode = "success" | "error";
export function createVoteReminderMockService(mode: ReminderMockMode = "success"): VoteReminderService {
  return {
    async save() {
      await new Promise((resolve) => setTimeout(resolve, 350));
      if (mode === "error") throw new Error("Mock reminder save failed.");
      return { saved: true };
    },
  };
}
