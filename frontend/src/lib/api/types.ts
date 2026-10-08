/** Client-side transport metadata, not a declaration of the server's JSON envelope. */
export interface ApiResponseContext {
  status: number;
  headers: Headers;
}

/** The decoder must validate unknown JSON before exposing a domain DTO. */
export type ResponseDecoder<T> = (
  body: unknown,
  context: ApiResponseContext,
) => T;

export type ApiRequest<T> = Omit<RequestInit, "body" | "redirect"> & {
  decode: ResponseDecoder<T>;
} & (
    | { json: unknown; body?: never }
    | { json?: never; body?: BodyInit | null }
  );

export interface ApiClient {
  request<T>(path: string, options: ApiRequest<T>): Promise<T>;
}

/** Local error metadata. A contract-specific mapper supplies these values. */
export interface ApiErrorInfo {
  code?: string;
  message?: string;
  details?: unknown;
  traceId?: string;
}

export type ErrorMapper = (
  body: unknown,
  context: ApiResponseContext,
) => ApiErrorInfo;

export interface RequestContext {
  url: URL;
  method: string;
  signal: AbortSignal | null;
}

/** TODO(#74, BE #4): inject agreed session delivery; no implicit Bearer or cookie. */
export type PrepareRequest = (
  context: RequestContext,
) =>
  | Pick<RequestInit, "headers" | "credentials">
  | Promise<Pick<RequestInit, "headers" | "credentials">>;

export type ApiTransport = (request: Request) => Promise<Response>;

export interface ApiClientOptions {
  /** Full base URL including any agreed API prefix. No implicit /api/v1. */
  baseUrl?: string;
  prepareRequest?: PrepareRequest;
  mapError?: ErrorMapper;
  transport?: ApiTransport;
}
