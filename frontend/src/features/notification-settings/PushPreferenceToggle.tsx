"use client";
import Image from "next/image";

/** L08's 108×44 control and 44×26 track; the single saved preference has no app-record switch. */
export function PushPreferenceToggle({ enabled, disabled, pending, onChange }: { enabled: boolean; disabled: boolean; pending: boolean; onChange: (enabled: boolean) => void }) {
  return <div className="flex h-[52px] shrink-0 items-center gap-1">
    <span id="push-preference-label" className="min-w-0 flex-1 text-body">푸시 알림 수신</span>
    <button type="button" role="switch" aria-labelledby="push-preference-label" aria-checked={enabled} aria-busy={pending} disabled={disabled} onClick={() => onChange(!enabled)} className="flex h-[44px] w-[108px] shrink-0 items-center justify-center gap-internal px-1 disabled:cursor-not-allowed">
      <span className="w-[40px] text-left text-body">{enabled ? "ON" : "OFF"}</span>
      {enabled ? <Image src="/icons/push-toggle-on.svg" alt="" width={44} height={26} unoptimized/> : <span aria-hidden="true" className="relative h-[26px] w-[44px] shrink-0 rounded-[13px] bg-[#94A3B8]"><span className="absolute left-[4px] top-[3px] h-[20px] w-[20px] rounded-full bg-surface"/></span>}
    </button>
  </div>;
}
