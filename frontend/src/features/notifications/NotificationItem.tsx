"use client";
import type { NotificationItemModel } from "./model";

/** Reusable H01 display row; original content is never duplicated here. */
export function NotificationItem({ item, disabled = false, pending = false, onSelect }: { item: NotificationItemModel; disabled?: boolean; pending?: boolean; onSelect: () => void }) {
  return <button type="button" onClick={onSelect} disabled={disabled} aria-busy={pending} aria-label={`${item.read ? "읽음" : "미확인"}: ${item.title}`} className="flex w-full shrink-0 flex-col gap-section rounded-card border border-border bg-surface p-section text-left shadow-card disabled:cursor-not-allowed">
    <span className="text-caption text-secondary">{!item.read && "● 미확인 · "}□ {item.typeLabel} · {item.timeLabel}</span>
    <span className="text-body text-text">{item.title}</span>
    <span className="text-body text-secondary">{item.description}</span>
  </button>;
}
