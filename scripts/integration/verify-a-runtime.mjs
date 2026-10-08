// Read-only deployment checks; no OTP, accounts, credentials, qualifications or migrations.
const api = process.env.NEXT_PUBLIC_API_BASE_URL;
const origin = process.env.DISCUSHION_FE_ORIGIN;
if (!api || !origin) throw new Error("Set NEXT_PUBLIC_API_BASE_URL and the exact DISCUSHION_FE_ORIGIN");
const base = new URL(api.endsWith("/") ? api : api + "/");
const fe = new URL(origin);
if (!["http:", "https:"].includes(base.protocol) || base.username || base.password || base.search || base.hash
    || fe.origin !== origin || fe.username || fe.password) throw new Error("Use credential-free API URL and an exact FE origin");
const checks = [];
async function request(label, url, init, check) {
  try {
    const response = await fetch(url, { ...init, redirect: "error", signal: AbortSignal.timeout(60000) });
    checks.push({ check: label, status: response.status, passed: await check(response) });
  } catch { checks.push({ check: label, passed: false, error: "network-or-invalid-response" }); }
}
await request("health", new URL("/health", base), {}, async response => response.ok && (await response.json()).data?.status === "UP");
await request("registered-regions", new URL("regions", base), {}, async response => {
  const body = await response.json();
  return response.ok && Array.isArray(body.data) && body.data.length > 0
    && body.data.every(row => Number.isSafeInteger(row.id) && row.id > 0 && typeof row.name === "string" && (row.mapFeatureKey === null || /^\d{10}$/.test(row.mapFeatureKey)));
});
await request("unauthenticated-login-denied", new URL("auth/login", base), { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" }, response => response.status === 401);
await request("unauthenticated-profile-denied", new URL("users/me", base), {}, response => response.status === 401);
await request("exact-origin-preflight", new URL("auth/login", base), { method: "OPTIONS", headers: { Origin: origin, "Access-Control-Request-Method": "POST", "Access-Control-Request-Headers": "authorization,content-type,x-post-share-token" } }, response => {
  const headers = (response.headers.get("Access-Control-Allow-Headers") ?? "").toLowerCase();
  return response.ok && response.headers.get("Access-Control-Allow-Origin") === origin
    && ["authorization", "content-type", "x-post-share-token"].every(header => headers.includes(header));
});
console.log(JSON.stringify({ checks, authenticatedJourney: "Requires real Privy app, OTP mailbox and separately provisioned demo accounts" }, null, 2));
if (checks.some(check => !check.passed)) process.exitCode = 1;
