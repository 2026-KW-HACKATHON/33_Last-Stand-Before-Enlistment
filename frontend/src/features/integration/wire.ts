import type { ApiClient, ApiRequest } from "../../lib/api/types";
import { ApiError } from "../../lib/api/error";

export type JsonObject = Record<string, unknown>;
export function object(value: unknown): JsonObject {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Expected object");
  return value as JsonObject;
}
export function array(value: unknown): unknown[] {
  if (!Array.isArray(value)) throw new Error("Expected array");
  return value;
}
export function text(value: unknown): string {
  if (typeof value !== "string") throw new Error("Expected string");
  return value;
}
export function bool(value: unknown): boolean {
  if (typeof value !== "boolean") throw new Error("Expected boolean");
  return value;
}
export function count(value: unknown): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 0) throw new Error("Expected nonnegative safe integer");
  return value;
}
export function id(value: unknown): string {
  const n = count(value);
  if (n < 1) throw new Error("Expected positive safe ID");
  return String(n);
}
export function inputId(value: string): number {
  if (!/^[1-9][0-9]*$/.test(value)) throw new ApiError("validation", "올바른 ID가 필요합니다.");
  const n = Number(value);
  if (!Number.isSafeInteger(n)) throw new ApiError("validation", "ID 범위를 벗어났습니다.");
  return n;
}
export function timestamp(value: unknown): string {
  const s = text(value);
  if (!/^\d{4}-\d{2}-\d{2}T.*(?:Z|[+-]\d{2}:\d{2})$/.test(s) || !Number.isFinite(Date.parse(s))) throw new Error("Expected offset timestamp");
  return s;
}
export function enumValue<const T extends readonly string[]>(value: unknown, values: T): T[number] {
  const s = text(value);
  if (!values.includes(s)) throw new Error(`Unknown enum: ${s}`);
  return s as T[number];
}
export function nullableText(value: unknown): string | undefined {
  return value === null ? undefined : text(value);
}
export function region(value: unknown) {
  const row = object(value);
  return { id: id(row.id), name: text(row.name) };
}
export function url(value: unknown): string {
  const s = text(value), parsed = new URL(s);
  if (!["http:", "https:"].includes(parsed.protocol) || parsed.username || parsed.password) throw new Error("Unsafe URL");
  return s;
}
export function query(path: string, params: Record<string, string | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) if (value !== undefined) search.set(key, value);
  return search.size ? `${path}?${search}` : path;
}
/** Feature boundary only: uses A's client and never supplies authentication. */
export async function data<T>(client: ApiClient, path: string, decode: (value: unknown) => T, init: Omit<ApiRequest<T>, "decode"> = {}): Promise<T> {
  try {
    return await client.request(path, { ...init, decode: body => decode(object(body).data) } as ApiRequest<T>);
  } catch (error) {
    if (!(error instanceof ApiError) || !error.body || typeof error.body !== "object") throw error;
    const envelope = object(error.body), wire = envelope.error ?? envelope;
    if (!wire || typeof wire !== "object" || Array.isArray(wire)) throw error;
    const info = object(wire);
    throw new ApiError(error.kind, typeof info.message === "string" ? info.message : error.message, {
      status: error.status, headers: error.headers, body: error.body,
      code: typeof info.code === "string" ? info.code : error.code,
      details: info.details, traceId: typeof envelope.traceId === "string" ? envelope.traceId : error.traceId, cause: error,
    });
  }
}
export function page(body: unknown) {
  const envelope = object(body), meta = object(envelope.meta);
  const hasNext = bool(meta.hasNext), cursor = nullableText(meta.nextCursor);
  if (hasNext !== Boolean(cursor)) throw new Error("Inconsistent cursor metadata");
  return { rows: array(envelope.data), nextCursor: cursor };
}
export async function allPages(client: ApiClient, path: string, signal?: AbortSignal): Promise<unknown[]> {
  const rows: unknown[] = [], seen = new Set<string>();
  let cursor: string | undefined;
  do {
    const result = await client.request(query(path, { cursor }), { signal, decode: page });
    rows.push(...result.rows);
    cursor = result.nextCursor;
    if (cursor && seen.has(cursor)) throw new ApiError("invalid-response", "반복된 페이지 커서를 받았습니다.");
    if (cursor) seen.add(cursor);
  } while (cursor);
  return rows;
}
