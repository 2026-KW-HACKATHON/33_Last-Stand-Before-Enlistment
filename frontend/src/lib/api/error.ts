import type { ApiErrorInfo } from "./types";

export type ApiErrorKind =
  | "configuration"
  | "network"
  | "cancelled"
  | "invalid-response"
  | "validation"
  | "unauthorized"
  | "forbidden"
  | "unavailable"
  | "conflict"
  | "payload-too-large"
  | "unsupported-media-type"
  | "domain-validation"
  | "rate-limit"
  | "server"
  | "http";

export function httpErrorKind(status: number): ApiErrorKind {
  switch (status) {
    case 400: return "validation";
    case 401: return "unauthorized";
    case 403: return "forbidden";
    case 404: return "unavailable";
    case 409: return "conflict";
    case 413: return "payload-too-large";
    case 415: return "unsupported-media-type";
    case 422: return "domain-validation";
    case 429: return "rate-limit";
    default: return status >= 500 ? "server" : "http";
  }
}

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number | undefined;
  readonly headers: Headers | undefined;
  readonly body: unknown;
  readonly code: string | undefined;
  readonly details: unknown;
  readonly traceId: string | undefined;

  constructor(
    kind: ApiErrorKind,
    message: string,
    options: ApiErrorInfo & {
      status?: number;
      headers?: Headers;
      body?: unknown;
      cause?: unknown;
    } = {},
  ) {
    super(message, { cause: options.cause });
    this.name = "ApiError";
    this.kind = kind;
    this.status = options.status;
    this.headers = options.headers;
    this.body = options.body;
    this.code = options.code;
    this.details = options.details;
    this.traceId = options.traceId;
  }
}

export function transportError(cause: unknown, signal: AbortSignal | null): ApiError {
  if (signal?.aborted || (cause instanceof Error && cause.name === "AbortError")) {
    return new ApiError("cancelled", "Request cancelled.", { cause });
  }
  if (cause instanceof ApiError) return cause;
  return new ApiError("network", "Could not reach the API.", { cause });
}
