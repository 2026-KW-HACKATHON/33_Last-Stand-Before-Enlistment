import type { ReactNode } from "react";

/** Display-only vote value; never a selection control or status badge. */
export function VoteValuePill({ children }: { children: ReactNode }) {
  return <span className="min-w-0 whitespace-normal break-words rounded-chip bg-white px-[10px] py-1 text-[11px] leading-snug text-primary">{children}</span>;
}

/** Consume existing display strings without changing options, counts or metadata. */
export function VoteValueSummary({ summary }: { summary: string }) {
  return <div className="flex flex-wrap items-center gap-x-2 gap-y-1">{summary.split(" | ").map((part, index) => {
    const labeled = /^(내 선택|내가 제출한 선택|최다|현재 결과|최종 결과): ([\s\S]*)$/.exec(part);
    if (!labeled) return <span key={index}>{part}</span>;
    const [value, ...metadata] = labeled[2].split(" · ");
    // Counts that follow an option belong inside its pill; participant totals and dates do not.
    const optionCounts = metadata.filter(text => /^\d+표$|^\d+(?:\.\d+)?%$/.test(text));
    const generalInfo = metadata.filter(text => !optionCounts.includes(text));
    return <span key={index} className="inline-flex max-w-full min-w-0 flex-wrap items-center gap-1"><span className="shrink-0 whitespace-nowrap">{labeled[1]}</span><VoteValuePill>{[value, ...optionCounts].join(" · ")}</VoteValuePill>{generalInfo.map((text, metaIndex) => <span key={metaIndex}>· {text}</span>)}</span>;
  })}</div>;
}
