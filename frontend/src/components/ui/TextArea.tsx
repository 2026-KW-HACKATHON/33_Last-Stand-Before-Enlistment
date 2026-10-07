"use client";

import { useId, type ComponentPropsWithRef } from "react";

export type TextAreaProps = ComponentPropsWithRef<"textarea"> & { label: string };

export function TextArea({ label, id, className = "", disabled, rows = 3, ...props }: TextAreaProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;

  return (
    <div className={`flex min-h-textarea w-full flex-col gap-internal rounded-input border border-border bg-surface px-page py-section focus-within:border-primary has-[:disabled]:bg-disabled has-[[aria-invalid=true]]:border-error ${className}`}>
      <label htmlFor={inputId} className="text-caption">{label}</label>
      <textarea
        {...props}
        id={inputId}
        rows={rows}
        disabled={disabled}
        className="min-w-0 w-full flex-1 resize-y bg-transparent text-body placeholder:text-secondary disabled:cursor-not-allowed disabled:text-muted"
      />
    </div>
  );
}
