import { ApiError, httpErrorKind, transportError } from "./error";
import type { ApiClient, ApiClientOptions, ApiErrorInfo, ApiRequest, ApiResponseContext } from "./types";

function resolveBaseUrl(value: string | undefined): URL {
  try {
    if (!value) throw new Error("Missing API base URL.");
    const url = new URL(value);
    if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
      throw new Error("Expected an HTTP(S) base URL without credentials, query or fragment.");
    }
    url.pathname = `${url.pathname.replace(/\/+$/, "")}/`;
    return url;
  } catch (cause) {
    throw new ApiError("configuration", "Configure NEXT_PUBLIC_API_BASE_URL or pass baseUrl.", { cause });
  }
}

function resolveEndpoint(baseUrl: URL, path: string): URL {
  const url = new URL(path.replace(/^\/(?!\/)/, ""), baseUrl);
  if (url.origin !== baseUrl.origin || !url.pathname.startsWith(baseUrl.pathname) || url.username || url.password || url.hash) {
    throw new Error("Endpoint must stay within the configured API base URL.");
  }
  return url;
}

export function createApiClient(options: ApiClientOptions = {}): ApiClient {
  const baseUrl = resolveBaseUrl(options.baseUrl ?? process.env.NEXT_PUBLIC_API_BASE_URL);
  const transport = options.transport ?? ((request: Request) => fetch(request));

  return {
    async request<T>(path: string, input: ApiRequest<T>): Promise<T> {
      const { decode, json, body, ...init } = input;
      const signal = init.signal ?? null;
      let request: Request;

      try {
        signal?.throwIfAborted();
        const url = resolveEndpoint(baseUrl, path);
        const headers = new Headers(init.headers);
        if (!headers.has("Accept")) headers.set("Accept", "application/json");
        let requestBody = body;
        if ("json" in input) {
          if (body !== undefined) throw new Error("Use either json or body.");
          requestBody = JSON.stringify(json);
          if (requestBody === undefined) throw new Error("JSON body is not serializable.");
          if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json");
        }
        const prepared = await options.prepareRequest?.({
          url: new URL(url),
          method: (init.method ?? "GET").toUpperCase(),
          signal,
        });
        new Headers(prepared?.headers).forEach((value, key) => headers.set(key, value));
        signal?.throwIfAborted();
        request = new Request(url, {
          cache: "no-store",
          ...init,
          redirect: "error",
          credentials: prepared?.credentials ?? init.credentials ?? "omit",
          headers,
          body: requestBody,
        });
      } catch (cause) {
        if (signal?.aborted) throw transportError(cause, signal);
        if (cause instanceof ApiError) throw cause;
        throw new ApiError("configuration", "Could not prepare the API request.", { cause });
      }

      let response: Response;
      let text: string;
      try {
        response = await transport(request);
        text = response.status === 204 || response.status === 205 || request.method === "HEAD"
          ? ""
          : await response.text();
        signal?.throwIfAborted();
      } catch (cause) {
        throw transportError(cause, signal);
      }

      const context: ApiResponseContext = { status: response.status, headers: response.headers };
      let parsed: unknown = undefined;
      let parseError: unknown;
      if (text !== "") {
        try {
          parsed = JSON.parse(text) as unknown;
        } catch (cause) {
          parsed = text;
          parseError = cause;
        }
      }

      if (!response.ok) {
        // Malformed/HTML error bodies and a broken mapper must not hide 401/403/etc.
        let info: ApiErrorInfo = {};
        let mapperError: unknown;
        try {
          info = options.mapError?.(parsed, context) ?? {};
        } catch (cause) {
          mapperError = cause;
        }
        throw new ApiError(httpErrorKind(response.status), info.message ?? `API request failed (${response.status}).`, {
          ...info,
          ...context,
          body: parsed,
          cause: mapperError ?? parseError,
        });
      }

      try {
        if (parseError) throw parseError;
        return decode(parsed, context);
      } catch (cause) {
        throw new ApiError("invalid-response", "API response does not match its contract.", {
          ...context,
          body: parsed,
          cause,
        });
      }
    },
  };
}
