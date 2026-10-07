import type { ComponentPropsWithoutRef } from "react";

export type NoticeProps = ComponentPropsWithoutRef<"div"> & {
  tone?: "info" | "warning" | "error";
};

const tones = {
  info: "bg-soft text-primary",
  warning: "bg-warning-soft text-warning",
  error: "bg-error-soft text-error",
};

export function Notice({ tone = "info", className = "", ...props }: NoticeProps) {
  return (
    <div
      {...props}
      className={`rounded-notice px-page py-section text-caption break-words ${tones[tone]} ${className}`}
    />
  );
}
