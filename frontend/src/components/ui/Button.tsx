"use client";

import type { ComponentPropsWithRef } from "react";

export type ButtonProps = ComponentPropsWithRef<"button"> & {
  variant?: "primary" | "secondary";
};

export function Button({ variant = "primary", type = "button", className = "", ...props }: ButtonProps) {
  const color = variant === "primary"
    ? "border-primary bg-primary text-surface"
    : "border-border bg-surface text-primary";

  return (
    <button
      {...props}
      type={type}
      className={`inline-flex min-h-button items-center justify-center gap-internal rounded-button border px-page py-section text-button disabled:cursor-not-allowed disabled:border-border disabled:bg-disabled disabled:text-muted ${color} ${className}`}
    />
  );
}
