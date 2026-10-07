import type { ComponentPropsWithoutRef, ReactNode } from "react";
export type SettingRowProps = Omit<ComponentPropsWithoutRef<"button">, "children"> & { label: string; value?: ReactNode; pending?: boolean };
/** Discushion/Menu: label/value, 14px radius, 16/12px padding, 8px gap. */
export function SettingRow({ label, value = "  ›", pending = false, disabled, className = "", ...props }: SettingRowProps) {
 return <button {...props} type="button" disabled={disabled || pending} aria-busy={pending || undefined} className={"flex w-full shrink-0 flex-col gap-internal rounded-card border border-border bg-surface px-page py-section text-left text-body disabled:cursor-not-allowed disabled:opacity-50 " + className}><span>{label}</span><span className="whitespace-pre-wrap text-secondary">{pending ? "연결 중…" : value}</span></button>;
}
