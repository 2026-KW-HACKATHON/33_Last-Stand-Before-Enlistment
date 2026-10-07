import { type Destination, routes } from "./routes";

/** Adapter assertions, scoped to the resource; not server DTOs or role-derived permissions. */
export type Capability =
  | { kind: "neighbor-region"; regionId: string }
  | { kind: "institution" }
  | { kind: "responsible-region"; regionId: string }
  | { kind: "edit-post"; postId: string };
type States<S extends string> = { [K in S]: { status: K } }[S];
export type CapabilitiesState =
  | States<"loading" | "error">
  | { status: "ready"; grants: readonly { capability: Capability; allowed: boolean }[] };
export type SessionState =
  | States<"loading" | "guest" | "signup-incomplete" | "error">
  | { status: "member"; capabilities: CapabilitiesState };
export type SharedAccess =
  | States<"loading" | "error" | "unavailable">
  | { status: "ready"; postId: string };
export type GuardContext = {
  regionId?: string;
  requirements?: readonly Capability[];
  sharedAccess?: SharedAccess;
};
export type GuardResult =
  | States<"allowed" | "loading" | "forbidden" | "unavailable">
  | { status: "login-required"; destination: { id: "login" } }
  | { status: "signup-required"; destination: { id: "signup" } }
  | { status: "error"; retryable: true };

function sameCapability(left: Capability, right: Capability): boolean {
  if (left.kind !== right.kind) return false;
  if ("regionId" in left) return "regionId" in right && left.regionId === right.regionId;
  if ("postId" in left) return "postId" in right && left.postId === right.postId;
  return true;
}
export function evaluateGuard(destination: Destination, session: SessionState, context: GuardContext = {}): GuardResult {
  const access = routes[destination.id].access;
  if (access === "public") return { status: "allowed" };
  if (session.status === "loading") return { status: "loading" };
  if (session.status === "error") return { status: "error", retryable: true };
  if (access === "shared") {
    const shared = context.sharedAccess;
    if (!shared || shared.status === "loading") return { status: "loading" };
    if (shared.status === "error") return { status: "error", retryable: true };
    if (shared.status === "unavailable") return { status: "unavailable" };
    return { status: "params" in destination && shared.postId === destination.params.postId ? "allowed" : "forbidden" };
  }
  if (session.status === "guest") return { status: "login-required", destination: { id: "login" } };
  if (session.status === "signup-incomplete") return { status: "signup-required", destination: { id: "signup" } };
  const requirements: Capability[] = [...(context.requirements ?? [])];
  const route = routes[destination.id];
  const policy = "capability" in route ? route.capability : null;
  if (policy === "neighbor-region") {
    if (!context.regionId) return { status: "error", retryable: true };
    requirements.push({ kind: "neighbor-region", regionId: context.regionId });
  }
  if (policy === "edit-post" && "params" in destination) requirements.push({ kind: "edit-post", postId: destination.params.postId });
  if (policy === "institution") requirements.push({ kind: "institution" });
  if (!requirements.length) return { status: "allowed" };
  if (session.status !== "member") return { status: "error", retryable: true };
  const capabilities = session.capabilities;
  if (capabilities.status === "loading") return { status: "loading" };
  if (capabilities.status === "error") return { status: "error", retryable: true };
  const grants = requirements.map(requirement => capabilities.grants.filter(grant => sameCapability(grant.capability, requirement)));
  // Missing or conflicting assertions require a requery, never an inferred permission.
  if (grants.some(matches => matches.length !== 1)) return { status: "error", retryable: true };
  return { status: grants.every(matches => matches[0].allowed) ? "allowed" : "forbidden" };
}
