import { destinationFromPathname, resolveDestination, type Destination } from "./routes";
/** Only canonical, registered internal pages; never arbitrary URLs or query/token storage. */
export function safeReturnDestination(value: string | null): Destination | null {
  if (!value || value.startsWith("//") || /[\\?#\u0000-\u0020]/u.test(value)) return null;
  const target = destinationFromPathname(value);
  if (!target || ["start", "login", "signup", "sharedPost"].includes(target.id)) return null;
  if ("params" in target && (!/^[1-9]\d*$/.test(target.params.postId) || !Number.isSafeInteger(Number(target.params.postId)))) return null;
  return resolveDestination(target).status === "ready" ? target : null;
}
/** Kept only in mounted navigation memory so cancelling login can restore the original share. */
export function safeSharedHref(href: string, postId: string): string | null {
  if (!/^[1-9]\d*$/.test(postId) || !Number.isSafeInteger(Number(postId))) return null;
  try {
    const parsed = new URL(href, "https://internal.invalid");
    if (parsed.origin !== "https://internal.invalid" || parsed.pathname !== `/shared/posts/${postId}` || parsed.hash) return null;
    const keys = Array.from(parsed.searchParams.keys());
    if (keys.length !== 1 || keys[0] !== "token" || !parsed.searchParams.get("token") || /[\r\n]/.test(parsed.searchParams.get("token")!)) return null;
    return parsed.pathname + parsed.search;
  } catch { return null; }
}
