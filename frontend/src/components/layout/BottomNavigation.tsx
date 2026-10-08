"use client";

import Link from "next/link";
import Image from "next/image";
import type { ComponentPropsWithoutRef } from "react";

const items = [
  { id: "main", label: "메인", icon: "/icons/home.svg" },
  { id: "map", label: "지도", icon: "/icons/map.svg" },
  { id: "write", label: "글쓰기", icon: "/icons/write.svg" },
  { id: "notification", label: "알림", icon: "/icons/notification.svg" },
  { id: "my", label: "마이", icon: "/icons/my.svg" },
] as const;

export type NavigationItem = (typeof items)[number]["id"];
export type BottomNavigationProps = Omit<ComponentPropsWithoutRef<"nav">, "children"> & {
  activeItem?: NavigationItem;
  // TODO: Product routes are not implemented. Pages supply confirmed destinations.
  destinations?: Partial<Record<NavigationItem, string>>;
  onNavigate?: (item: NavigationItem) => void;
};

export function BottomNavigation({ activeItem, destinations = {}, onNavigate, className = "", ...props }: BottomNavigationProps) {
  return (
    <nav
      aria-label="하단 메뉴"
      {...props}
      className={`relative flex min-h-navigation shrink-0 items-center bg-surface px-page py-internal shadow-navigation before:pointer-events-none before:absolute before:inset-0 before:border before:border-border ${className}`}
    >
      {items.map(({ id, label, icon }) => {
        const selected = activeItem === id;
        const href = destinations[id];
        const itemClass = `flex min-h-[53px] min-w-0 flex-1 flex-col items-center justify-center gap-1 px-0.5 py-1 text-navigation ${selected ? "text-primary" : "text-secondary"}`;
        const content = id === "write" ? (
          <>
            <Image src={icon} alt="" width={40} height={40} unoptimized className={`-translate-y-[2.5px] shrink-0 rounded-chip ${selected ? "ring-2 ring-primary ring-offset-2 ring-offset-surface" : ""}`} />
            <span className="sr-only">{label}</span>
          </>
        ) : (
          <>
            <span
              aria-hidden="true"
              className="size-nav-icon shrink-0 bg-current"
              style={{ mask: `url(${icon}) center / contain no-repeat` }}
            />
            <span>{label}</span>
          </>
        );

        if (href) {
          return <Link key={id} href={href} aria-current={selected ? "page" : undefined} className={itemClass}>{content}</Link>;
        }

        return (
          <button
            key={id}
            type="button"
            disabled={!onNavigate}
            aria-current={selected ? "page" : undefined}
            onClick={() => onNavigate?.(id)}
            className={`${itemClass} disabled:cursor-not-allowed disabled:opacity-50`}
          >
            {content}
          </button>
        );
      })}
    </nav>
  );
}
