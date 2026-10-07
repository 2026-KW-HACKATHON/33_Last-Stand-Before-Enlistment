import type { Destination, NavigationEntry } from "../../lib/navigation";
export const settingsMenus = [
 { id: "account", label: "계정 관리", section: "계정", issue: 92, destination: { id: "account" } },
 { id: "myPosts", label: "내가 쓴 글", section: "커뮤니티", issue: 47, destination: { id: "myPosts" } },
 { id: "participations", label: "댓글 남긴 글", section: "커뮤니티", issue: 47, destination: { id: "participations" } },
 { id: "bookmarks", label: "스크랩한 글", section: "커뮤니티", issue: 49, destination: { id: "bookmarks" } },
 { id: "interestKeywords", label: "관심 키워드", section: "커뮤니티", issue: 88, destination: { id: "interestKeywords" } },
 { id: "notificationSettings", label: "알림 설정", section: "앱 설정", issue: 90, destination: { id: "notificationSettings" } },
 { id: "logout", label: "로그아웃", section: "기타", issue: 94 },
 { id: "withdrawal", label: "회원 탈퇴", section: "기타", issue: 95, destination: { id: "withdrawal" } },
] as const satisfies readonly { id: string; label: string; section: string; issue: number; destination?: Destination }[];
export type SettingsMenuId = typeof settingsMenus[number]["id"];
/** Owner port. Logout is an action intent, never a guessed Route or logout implementation. */
export type SettingsMenuHandler = (id: SettingsMenuId, entry: NavigationEntry | null, onReturn: () => void, signal: AbortSignal) => void | Promise<void>;
export function settingsMenuEntry(id: SettingsMenuId, sharedContextRef?: string): NavigationEntry | null {
 const menu = settingsMenus.find(item => item.id === id);
 return menu && "destination" in menu ? { destination: menu.destination, origin: { id: "settings" }, ...(sharedContextRef ? { sharedContextRef } : {}) } : null;
}
/** Retain L01's original entry when a child returns with origin: settings. */
export function settingsEntry(previous: NavigationEntry | null, incoming: NavigationEntry): NavigationEntry {
 const origin = incoming.origin?.id === "settings" ? previous?.origin : incoming.origin;
 return { destination: { id: "settings" }, origin: origin ?? { id: "me" } };
}
export function createSettingsMenuStore(handler?: SettingsMenuHandler) {
 let state: { selected: SettingsMenuId | null; phase: "idle" | "pending" | "success" | "error" | "unavailable" } = { selected: null, phase: "idle" };
 let generation = 0; let controller: AbortController | null = null; const listeners = new Set<() => void>();
 const publish = (next: typeof state) => { state = next; listeners.forEach(fn => fn()); };
 return { getState: () => state, subscribe: (fn: () => void) => { listeners.add(fn); return () => { listeners.delete(fn); }; },
 cancel() { generation++; controller?.abort(); publish({ selected: null, phase: "idle" }); },
 async open(id: SettingsMenuId, entry: NavigationEntry | null, onReturn: () => void) {
  if (state.phase === "pending") return;
  const version = ++generation; controller?.abort(); controller = new AbortController(); const signal = controller.signal; publish({ selected: id, phase: handler ? "pending" : "unavailable" }); if (!handler) return;
  try { await handler(id, entry, () => { if (version === generation) onReturn(); }, signal); if (version === generation) publish({ selected: id, phase: "success" }); }
  catch { if (version === generation) publish({ selected: id, phase: "error" }); }
 }, dispose() { generation++; controller?.abort(); listeners.clear(); } };
}
