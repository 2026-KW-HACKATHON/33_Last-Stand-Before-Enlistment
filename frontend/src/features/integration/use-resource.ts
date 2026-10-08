"use client";
import { useEffect, useState } from "react";
import { ApiError } from "../../lib/api/error";
export type Resource<T> = { kind: "loading" } | { kind: "success"; data: T } | { kind: "error"; message: string } | { kind: "unavailable"; message: string };
export function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) return "로그인이 필요합니다. 로그인 후 다시 시도해 주세요.";
    if (error.status === 403) return "현재 계정의 지역·기관 권한으로 이용할 수 없습니다.";
    if (error.status === 404) return "삭제되었거나 더 이상 볼 수 없는 대상입니다.";
    if (error.status === 409) return "현재 상태와 충돌했습니다. 다시 조회해 주세요.";
    if (error.status === 429) return "요청이 많습니다. 잠시 후 다시 시도해 주세요.";
  }
  return "불러오지 못했습니다. 다시 시도해 주세요.";
}
export function useResource<T>(load: ((signal: AbortSignal) => Promise<T>) | null) {
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState<{ source: typeof load; value: Resource<T> } | null>(null);
  useEffect(() => {
    if (!load) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setState({ source: load, value: { kind: "loading" } });
      void load(controller.signal).then(data => {
        if (!controller.signal.aborted) setState({ source: load, value: { kind: "success", data } });
      }).catch(error => {
        if (!controller.signal.aborted) setState({ source: load, value: { kind: error instanceof ApiError && error.status === 404 ? "unavailable" : "error", message: errorMessage(error) } });
      });
    }, 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [load, attempt]);
  return { state: !load ? { kind: "error" as const, message: "API 연결 준비 중입니다." } : state?.source === load ? state.value : { kind: "loading" as const }, retry: () => setAttempt(value => value + 1) };
}
