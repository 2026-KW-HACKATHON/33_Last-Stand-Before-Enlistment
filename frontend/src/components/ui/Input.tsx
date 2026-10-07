"use client";

import { useId, type ComponentPropsWithRef } from "react";

export type InputProps = ComponentPropsWithRef<"input"> & { label: string };

export function Input({ label, id, className = "", disabled, ...props }: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;

  return (
    <div className={`flex w-full flex-col gap-internal rounded-input border border-border bg-surface px-page py-section focus-within:border-primary has-[:disabled]:bg-disabled has-[[aria-invalid=true]]:border-error ${className}`}>
      <label htmlFor={inputId} className="text-caption">{label}</label>
      <input
        {...props}
        id={inputId}
        disabled={disabled}
        className="min-w-0 w-full bg-transparent text-body placeholder:text-secondary disabled:cursor-not-allowed disabled:text-muted"
      />
    </div>
  );
}
