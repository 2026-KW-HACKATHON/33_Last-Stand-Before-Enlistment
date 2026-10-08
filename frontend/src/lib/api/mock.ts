import { createApiClient } from "./client";
import { ApiError } from "./error";
import type { ApiClient, ApiClientOptions } from "./types";

export interface MockRoute {
  method: string;
  path: string;
  respond: (request: Request) => Response | Promise<Response>;
}

export interface MockClientOptions extends Pick<ApiClientOptions, "mapError"> {
  routes: readonly MockRoute[];
  delayMs?: number;
}

function waitForDelay(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const onAbort = () => {
      clearTimeout(timer);
      reject(signal.reason);
    };
    const timer = setTimeout(() => {
      signal.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    signal.addEventListener("abort", onAbort, { once: true });
  });
}

function respondWithSignal(route: MockRoute, request: Request): Promise<Response> {
  return new Promise((resolve, reject) => {
    const onAbort = () => reject(request.signal.reason);
    request.signal.addEventListener("abort", onAbort, { once: true });
    Promise.resolve().then(() => {
      request.signal.throwIfAborted();
      return route.respond(request);
    }).then(resolve, reject).finally(() => request.signal.removeEventListener("abort", onAbort));
  });
}

/** Development/test only. No default fixtures, environment toggle or live fetch fallback. */
export function createMockApiClient(options: MockClientOptions): ApiClient {
  if (process.env.NODE_ENV === "production") {
    throw new ApiError("configuration", "Mock API clients are disabled in production.");
  }
  const delayMs = options.delayMs ?? 0;
  if (!Number.isFinite(delayMs) || delayMs < 0) {
    throw new ApiError("configuration", "Mock delay must be a finite non-negative number.");
  }

  return createApiClient({
    // Request construction only; .invalid is never contacted or used as a backend URL.
    baseUrl: "https://discushion-mock.invalid/",
    mapError: options.mapError,
    async transport(request) {
      request.signal.throwIfAborted();
      const route = options.routes.find((candidate) =>
        candidate.method.toUpperCase() === request.method &&
        candidate.path === new URL(request.url).pathname,
      );
      if (!route) throw new ApiError("configuration", "No Mock route matches this request.");
      await waitForDelay(delayMs, request.signal);
      request.signal.throwIfAborted();
      return respondWithSignal(route, request);
    },
  });
}
