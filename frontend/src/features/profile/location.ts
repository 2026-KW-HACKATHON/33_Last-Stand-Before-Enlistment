import type { ProfileService } from "./contracts";
import type { SignupRegion } from "../signup/contracts";
/** One user-triggered browser request. Coordinate-to-region adapter is agreed by Integration. No tracking or qualification mutations. */
export function browserRegionLocator(resolve: (point: { latitude: number; longitude: number }, signal: AbortSignal) => Promise<SignupRegion[]>): ProfileService["locate"] {
 return signal => new Promise(resolveResult => {
  if (signal.aborted || !navigator.geolocation) return resolveResult({ kind: "unavailable" });
  let settled = false;
  const finish = (result: Awaited<ReturnType<ProfileService["locate"]>>) => { if (settled) return; settled = true; signal.removeEventListener("abort", abort); resolveResult(result); };
  const abort = () => finish({ kind: "failed" }); signal.addEventListener("abort", abort, { once: true });
  navigator.geolocation.getCurrentPosition(async position => { if (settled || signal.aborted) return; try { const value = await resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }, signal); if (!signal.aborted) finish({ kind: "success", value }); } catch { finish({ kind: "failed" }); } }, error => finish({ kind: error.code === error.PERMISSION_DENIED ? "denied" : "failed" }), { timeout: 10000, maximumAge: 0 });
 });
}
