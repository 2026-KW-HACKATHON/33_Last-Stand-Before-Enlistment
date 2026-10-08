import test from "node:test";
import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { VoteValuePill, VoteValueSummary } from "./VoteValue";

test("vote summary labels stay plain while complete option values use white pills", () => {
  const html = renderToStaticMarkup(<VoteValueSummary summary="내 선택: 토요일 오전 32% | 최다: 평일 저녁 48% | 현재 결과: A 50% / B 50% | 최종 결과: 평일 저녁 52%"/>);
  for (const label of ["내 선택", "최다", "현재 결과", "최종 결과"]) assert.ok(html.includes(`>${label}</span>`));
  for (const value of ["토요일 오전 32%", "평일 저녁 48%", "A 50% / B 50%", "평일 저녁 52%"]) assert.match(html, new RegExp(`bg-white[^>]*>${value}</span>`));
  assert.ok(!html.includes(":")); assert.ok(!html.includes(" | ")); assert.ok(!html.includes("<button"));
});

test("legacy participation metadata remains outside the option pill", () => {
  const html = renderToStaticMarkup(<VoteValueSummary summary="내 선택: 오후 8시 · 80명 · 남은 2일 · 종료 2026. 10. 10"/>);
  assert.match(html, /bg-white[^>]*>오후 8시<\/span>/);
  for (const text of ["80명", "남은 2일", "종료 2026. 10. 10"]) assert.ok(html.includes(text));
  assert.equal((html.match(/bg-white/g) ?? []).length, 1);
});

test("submitted option count and percentage remain together; waiting gets no value pill", () => {
  const html = renderToStaticMarkup(<VoteValueSummary summary="내가 제출한 선택: 평일 저녁 · 12표 · 48%"/>);
  assert.match(html, /bg-white[^>]*>평일 저녁 · 12표 · 48%<\/span>/);
  const waiting = renderToStaticMarkup(<VoteValueSummary summary="본인 선택과 결과 조회 대기"/>);
  assert.ok(waiting.includes("본인 선택과 결과 조회 대기")); assert.ok(!waiting.includes("bg-white"));
});

test("value pills allow long options to wrap and carry no button, border or shadow", () => {
  const value = "길고 긴 실제 선택지 이름 100%";
  const html = renderToStaticMarkup(<VoteValuePill>{value}</VoteValuePill>);
  assert.ok(html.includes(value));
  for (const style of ["whitespace-normal", "break-words", "px-[10px]", "py-1", "rounded-chip"]) assert.ok(html.includes(style));
  for (const style of ["truncate", "ellipsis", "shadow", "border", "<button"]) assert.ok(!html.includes(style));
});
