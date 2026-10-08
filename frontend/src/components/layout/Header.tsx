"use client";

import type { ComponentPropsWithoutRef, ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";

export type HeaderProps = Omit<ComponentPropsWithoutRef<"header">, "title" | "children"> & {
  rightAction?: ReactNode;
} & (
  | { variant?: "default"; title: string; onBack?: () => void }
  | { variant: "brand"; title?: never; onBack?: never }
);

export type HeaderActionProps = {
  action: "settings" | "notification";
  variant?: "default" | "main";
  destination?: string;
  onAction?: () => void;
  disabled?: boolean;
};

// L01/H01 URLs and navigation state belong to #40 and the consuming Page.
export function HeaderAction({ action, variant = "default", destination, onAction, disabled = false }: HeaderActionProps) {
  const label = action === "settings" ? "설정" : "알림";
  const icon = action === "settings" ? "/icons/settings.svg" : "/icons/header-notification.svg";
  const className = `flex h-header shrink-0 items-center justify-end ${variant === "main" ? "w-8" : "w-14"} ${action === "settings" ? "pb-[3.117px]" : "pb-[4.918px]"}`;
  const content = <Image src={icon} alt="" width={32} height={32} unoptimized />;

  if (destination && !disabled) {
    return <Link href={destination} aria-label={label} className={className}>{content}</Link>;
  }

  return (
    <button type="button" aria-label={label} disabled={disabled || !onAction} onClick={onAction} className={`${className} disabled:cursor-not-allowed disabled:opacity-50`}>
      {content}
    </button>
  );
}

export function Header({ title, onBack, rightAction, variant = "default", className = "", ...props }: HeaderProps) {
  const brand = variant === "brand";
  return (
    <header {...props} className={`flex h-header shrink-0 items-center bg-surface ${brand ? "gap-section pl-6 pr-[14px]" : "gap-internal px-page"} ${className}`}>
      {onBack && (
        <button type="button" onClick={onBack} aria-label="뒤로가기" className="flex h-back-height w-back-width shrink-0 items-center justify-center text-back">
          <span aria-hidden="true" className="w-full -translate-y-[2.01px] text-left">‹</span>
        </button>
      )}
      {brand ? (
        <div className="flex h-header min-w-0 flex-1 items-center gap-1 pl-px">
          <Image src="/icons/brand.png" alt="" width={30} height={28} unoptimized className="shrink-0 -translate-y-px" />
          <h1 className="flex h-7 min-w-0 -translate-y-px items-center text-[20px] leading-none font-bold text-black">
            Dis<span className="text-primary">cushion</span>
          </h1>
        </div>
      ) : (
        <h1 className="min-w-0 flex-1 truncate text-header" title={title}>{title}</h1>
      )}
      {rightAction && <div className={`flex h-full shrink-0 items-center justify-end ${brand ? "gap-section" : "mr-internal"}`}>{rightAction}</div>}
    </header>
  );
}
