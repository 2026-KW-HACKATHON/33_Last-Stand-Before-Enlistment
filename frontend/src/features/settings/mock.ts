import type { NavigationEntry } from "../../lib/navigation";
import type { SettingsMenuHandler, SettingsMenuId } from "./model";
export const settingsScenarios = ["success", "error-retry", "loading"] as const;
export type SettingsScenario = typeof settingsScenarios[number];
/** Development-only Owner handoff. No account mutation or copied list Service. */
export function createMockSettingsHandler(scenario: SettingsScenario, onOpen: (id: SettingsMenuId, entry: NavigationEntry | null, onReturn: () => void) => void): SettingsMenuHandler {
 let attempts = 0;
 return async (id, entry, onReturn, signal) => {
  attempts++;
  if (scenario === "loading") return new Promise<void>(() => {});
  await new Promise<void>((resolve, reject) => { const timer = setTimeout(() => { signal.removeEventListener("abort", abort); resolve(); }, 80); const abort = () => { clearTimeout(timer); reject(new Error("Aborted")); }; if (signal.aborted) abort(); else signal.addEventListener("abort", abort, { once: true }); });
  if (scenario === "error-retry" && attempts === 1) throw new Error("Mock handoff failure");
  if (!signal.aborted) onOpen(id, entry, onReturn);
 };
}
