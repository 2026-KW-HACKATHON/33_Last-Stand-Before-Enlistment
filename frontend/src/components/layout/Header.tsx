"use client";

import type { ComponentPropsWithoutRef, ReactNode } from "react";

export type HeaderProps = Omit<ComponentPropsWithoutRef<"header">, "title" | "children"> & {
  title: string;
  onBack?: () => void;
  rightAction?: ReactNode;
};

export function Header({ title, onBack, rightAction, className = "", ...props }: HeaderProps) {
  return (
    <header {...props} className={`flex h-header shrink-0 items-center gap-internal bg-surface px-page ${className}`}>
      {onBack && (
        <button type="button" onClick={onBack} aria-label="뒤로가기" className="flex h-back-height w-back-width shrink-0 items-center justify-center text-back">
          <span aria-hidden="true">‹</span>
        </button>
      )}
      <h1 className="min-w-0 flex-1 truncate text-header" title={title}>{title}</h1>
      {rightAction && <div className="flex h-full shrink-0 items-center justify-end">{rightAction}</div>}
    </header>
  );
}
