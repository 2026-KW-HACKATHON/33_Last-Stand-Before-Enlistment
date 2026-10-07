import type { ResponseDecoder } from "./types";

/** Confirmed in back/develop HealthController; other endpoint DTOs remain unconfirmed. */
export interface ApiResponse<T> {
  data: T;
}

/** Opt in only for endpoints whose agreed response has a data envelope. */
export function decodeApiResponse<T>(decodeData: ResponseDecoder<T>): ResponseDecoder<ApiResponse<T>> {
  return (body, context) => {
    if (typeof body !== "object" || body === null || !("data" in body)) {
      throw new Error("Expected a data response envelope.");
    }
    return { data: decodeData(body.data, context) };
  };
}

/** No response body is undefined; JSON null remains null and is never converted. */
export const decodeNoContent: ResponseDecoder<void> = (body) => {
  if (body !== undefined) throw new Error("Expected no response body.");
};

// TODO(BE #2): pagination meta/ID/Enum/nullable DTOs need FE/BE agreement.
// TODO(#74, BE #4): local member, signup completion and authority DTOs.
// TODO(#74, BE #30): direct upload/file DTOs and final API base URL.
