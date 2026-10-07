import type { ComponentPropsWithoutRef, ReactNode } from "react";

export type MobileLayoutProps = ComponentPropsWithoutRef<"div"> & {
  header?: ReactNode;
  bottomNavigation?: ReactNode;
  contentClassName?: string;
};

export function MobileLayout({ header, bottomNavigation, children, className = "", contentClassName = "", ...props }: MobileLayoutProps) {
  return (
    <div {...props} className={`mx-auto flex h-dvh w-full max-w-mobile flex-col overflow-hidden bg-background ${className}`}>
      {header && <div className="shrink-0 pt-[env(safe-area-inset-top)] bg-surface">{header}</div>}
      <main className={`flex min-h-0 flex-1 flex-col gap-section overflow-y-auto px-page pt-content-top pb-content-bottom ${!header ? "[padding-top:calc(var(--spacing-content-top)+env(safe-area-inset-top))]" : ""} ${!bottomNavigation ? "[padding-bottom:calc(var(--spacing-content-bottom)+env(safe-area-inset-bottom))]" : ""} ${contentClassName}`}>
        {children}
      </main>
      {bottomNavigation && <div className="shrink-0 pb-[env(safe-area-inset-bottom)] bg-surface">{bottomNavigation}</div>}
    </div>
  );
}
