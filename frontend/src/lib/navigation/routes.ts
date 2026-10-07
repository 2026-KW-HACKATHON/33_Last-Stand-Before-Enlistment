/** FE navigation identity only; these are not API endpoints or implemented Pages. */
export const routes = {
  start: { path: "/", access: "public", fallback: "start" },
  login: { path: "/login", access: "public", fallback: "start" },
  signup: { path: "/signup", access: "public", fallback: "login" },
  home: { path: "/home", access: "member", fallback: "start" },
  exploreRegion: { path: "/explore/region", access: "member", fallback: "home" },
  board: { path: "/board", access: "member", fallback: "home" },
  map: { path: "/map", access: "member", fallback: "home" },
  post: { path: "/posts/[postId]", access: "member", fallback: "home" },
  newPost: { path: "/posts/new", access: "member", capability: "neighbor-region", fallback: "home" },
  editPost: { path: "/posts/[postId]/edit", access: "member", capability: "edit-post", fallback: "home" },
  sharedPost: { path: "/shared/posts/[postId]", access: "shared", fallback: "start" },
  me: { path: "/me", access: "member", fallback: "home" },
  profile: { path: "/me/profile", access: "member", fallback: "me" },
  activityRegion: { path: "/me/region", access: "member", fallback: "me" },
  bookmarks: { path: "/me/bookmarks", access: "member", fallback: "me" },
  myPosts: { path: "/me/posts", access: "member", fallback: "me" },
  participations: { path: "/me/participations", access: "member", fallback: "me" },
  myVotes: { path: "/me/votes", access: "member", fallback: "me" },
  officerAgendas: { path: "/officer/agendas", access: "member", capability: "institution", fallback: "me" },
  officerAgenda: { path: "/officer/agendas/[postId]", access: "member", capability: "institution", fallback: "officerAgendas" },
  search: { path: null, access: "member", fallback: "home" },
  recommendations: { path: null, access: "member", fallback: "home" },
  newRecommendation: { path: null, access: "member", fallback: "home" },
  notifications: { path: null, access: "member", fallback: "home" },
  interestRegions: { path: null, access: "member", fallback: "me" },
  interestKeywords: { path: null, access: "member", fallback: "me" },
  settings: { path: null, access: "member", fallback: "me" },
  account: { path: null, access: "member", fallback: "settings" },
  emailChange: { path: null, access: "member", fallback: "account" },
  notificationSettings: { path: null, access: "member", fallback: "settings" },
  withdrawal: { path: null, access: "member", fallback: "account" },
  withdrawalComplete: { path: null, access: "public", fallback: "login" },
} as const;

export type DestinationId = keyof typeof routes;
type PostDestinationId = "post" | "editPost" | "sharedPost" | "officerAgenda";
export type Destination =
  | { id: Exclude<DestinationId, PostDestinationId> }
  | { id: PostDestinationId; params: { postId: string } };

export const bottomNavigationDestinations = {
  main: { id: "home" }, map: { id: "map" }, write: { id: "newPost" },
  notification: { id: "notifications" }, my: { id: "me" },
} as const satisfies Record<string, Destination>;
export const headerDestinations = {
  settings: { id: "settings" }, notification: { id: "notifications" },
} as const satisfies Record<string, Destination>;
export const accountReturnDestinations = {
  logout: { id: "start" }, withdrawal: { id: "login" },
} as const satisfies Record<string, Destination>;

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
function safeSegment(value: unknown): value is string {
  if (typeof value !== "string" || !value.length || value === "." || value === ".." || /[\\/\u0000-\u0020\u007f%?#]/u.test(value)) return false;
  try { encodeURIComponent(value); return true; } catch { return false; }
}
/** Accept only internal identities. Extra fields (including pending actions) are discarded. */
export function parseDestination(value: unknown): Destination | null {
  if (!record(value) || typeof value.id !== "string" || !Object.hasOwn(routes, value.id)) return null;
  const id = value.id as DestinationId;
  if (routes[id].path?.includes("[postId]")) {
    if (!record(value.params) || !safeSegment(value.params.postId)) return null;
    const path = routes[id].path?.replace("[postId]", encodeURIComponent(value.params.postId));
    if (Object.entries(routes).some(([otherId, route]) => otherId !== id && route.path === path)) return null;
    return { id: id as PostDestinationId, params: { postId: value.params.postId } };
  }
  return { id: id as Exclude<DestinationId, PostDestinationId> };
}
export function destinationKey(destination: Destination): string {
  return "params" in destination ? `${destination.id}:${encodeURIComponent(destination.params.postId)}` : destination.id;
}
export function resolveDestination(value: unknown):
  | { status: "ready"; destination: Destination; href: string }
  | { status: "unresolved"; destination: Destination }
  | { status: "invalid" } {
  const destination = parseDestination(value);
  if (!destination) return { status: "invalid" };
  const path = routes[destination.id].path;
  if (path === null) return { status: "unresolved", destination };
  return { status: "ready", destination, href: "params" in destination
    ? path.replace("[postId]", encodeURIComponent(destination.params.postId)) : path };
}
/** Pathname only. No external URL, query serialization, or unregistered redirect targets. */
export function destinationFromPathname(pathname: string): Destination | null {
  if (!pathname.startsWith("/") || /[\\?#\u0000-\u0020]/u.test(pathname)) return null;
  for (const [id, route] of Object.entries(routes)) {
    if (route.path === pathname) return parseDestination({ id });
  }
  for (const [id, route] of Object.entries(routes)) {
    if (!route.path?.includes("[postId]")) continue;
    const [prefix, suffix] = route.path.split("[postId]");
    if (!pathname.startsWith(prefix) || !pathname.endsWith(suffix)) continue;
    const segment = pathname.slice(prefix.length, suffix ? -suffix.length : undefined);
    try {
      const destination = parseDestination({ id, params: { postId: decodeURIComponent(segment) } });
      const resolved = resolveDestination(destination);
      if (resolved.status === "ready" && resolved.href === pathname) return destination;
    } catch { /* Malformed percent encoding is not a destination. */ }
  }
  return null;
}
