"use client";
import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { Button } from "../../components/ui/Button";
import { TextArea } from "../../components/ui/TextArea";
import { Input } from "../../components/ui/Input";
import { Notice } from "../../components/ui/Notice";
import type { ProfileStepProps, RegionStepProps, SignupEditors } from "../signup/contracts";
import { useProfile } from "./provider";
const attributes = ["거주자", "학생", "직장인", "상인"];
export function ProfileFields({ value, onChange, disabled, photoUrl, signup = false }: ProfileStepProps & { photoUrl?: string; signup?: boolean }) {
 const { service } = useProfile(); const [photoError, setPhotoError] = useState<string | null>(null);
 const file = value.photo?.kind === "replace" ? value.photo.file : null;
 const preview = useMemo(() => file ? URL.createObjectURL(file) : null, [file]);
 useEffect(() => () => { if (preview) URL.revokeObjectURL(preview); }, [preview]);
 const src = file ? preview : value.photo?.kind === "remove" ? null : photoUrl;
 return <>
 {service?.photoPolicy && <><div className="relative h-[80px] overflow-hidden rounded-card border border-border bg-surface">
 {src ? <Image unoptimized src={src} alt="프로필 사진" width={364} height={80} className="h-[80px] w-full object-cover" /> : <p className="px-page py-section text-caption text-secondary">프로필 사진 (선택)</p>}
 <label className="absolute bottom-0 right-0 rounded-input bg-surface px-internal text-caption">사진 선택<input aria-label="프로필 사진 선택" className="sr-only" type="file" accept={service?.photoPolicy?.accept} disabled={disabled || !service?.photoPolicy} onChange={event => { const selected = event.currentTarget.files?.[0]; event.currentTarget.value = ""; if (!selected || !service?.photoPolicy) return; const error = service.photoPolicy.validate(selected); setPhotoError(error); if (!error) onChange({ ...value, photo: { kind: "replace", file: selected } }); }} /></label>
 </div>
 {(src || value.photo?.kind === "replace") && <button className="text-caption text-primary" disabled={disabled} onClick={() => { onChange({ ...value, photo: { kind: "remove" } }); setPhotoError(null); }}>사진 제거</button>}
 {photoError && <Notice tone="error" role="status">{photoError}</Notice>}
 </>}
 <Input label="닉네임" value={value.nickname} disabled={disabled} aria-invalid={!value.nickname.trim() || Array.from(value.nickname.trim()).length > 10} onChange={e => onChange({ ...value, nickname: e.currentTarget.value })} />
 <Notice>{signup ? "중복 없이 최대 10자" : "최대 10자 · 중복 불가"}</Notice>
 {!signup && <><TextArea label="한 줄 소개" value={value.bio} disabled={disabled} aria-invalid={Array.from(value.bio).length > 50} onChange={e => onChange({ ...value, bio: e.currentTarget.value })} /><Notice>최대 50자</Notice></>}
 <fieldset disabled={disabled} className="flex flex-col gap-section"><legend className="mb-section text-caption">이웃 속성 · 복수 선택</legend><div className="grid grid-cols-3 gap-internal">{attributes.map(label => <button type="button" key={label} aria-pressed={value.attributes.includes(label)} className={"rounded-chip px-page py-internal text-caption disabled:opacity-50 " + (value.attributes.includes(label) ? "bg-selected text-surface" : "bg-[#F3F4F6] text-[#4B5563]")} onClick={() => onChange({ ...value, attributes: value.attributes.includes(label) ? value.attributes.filter(v => v !== label) : [...value.attributes, label] })}>{value.attributes.includes(label) ? "☑" : "□"} {label}</button>)}</div></fieldset>
 </>;
}
export function RegionCandidates({ value, onChange, disabled, compact = false }: RegionStepProps & { compact?: boolean }) {
 const { store, state } = useProfile();
 return <>
 <form onSubmit={e => { e.preventDefault(); void store.search(); }} className="flex flex-col gap-section"><Input label="지역명 검색" value={state.query} disabled={disabled} onChange={e => store.setQuery(e.currentTarget.value)} /><Button variant="secondary" disabled={disabled || state.candidatePhase === "loading"} type="submit">지역 검색</Button></form>
 {store && <LocationButton disabled={disabled || state.candidatePhase === "loading"} onLocate={() => void store.locate()} />}
 {state.candidatePhase === "loading" && <Notice role="status">지역 후보를 찾고 있습니다.</Notice>}
 {state.candidatePhase === "empty" && <Notice role="status">지역 후보가 없습니다. 지역명을 직접 검색해 주세요.</Notice>}
 {["error", "denied", "unavailable"].includes(state.candidatePhase) && <><Notice role="status" tone="warning">{state.candidatePhase === "denied" ? "위치 권한이 거부되었습니다." : state.candidatePhase === "unavailable" ? "지역 연결 준비 중입니다." : "지역 후보를 찾지 못했습니다."} 지역명을 직접 검색해 주세요.</Notice><Button variant="secondary" disabled={disabled} onClick={() => void store.search()}>검색 다시 시도</Button></>}
 {state.candidates.map(region => <button key={region.reference} disabled={disabled} aria-pressed={value?.reference === region.reference} className={"flex flex-col gap-internal rounded-card border border-border bg-surface px-page py-section text-left " + (compact ? "min-h-[48px]" : "min-h-[80px]")} onClick={() => { store.choose(region); onChange(region); }}><span>{region.label}</span>{(!compact || value?.reference === region.reference) && <span className="text-secondary">{value?.reference === region.reference ? "선택됨 ›" : "›"}</span>}</button>)}

 {value && <Notice>선택된 지역 · {value.label}</Notice>}
 </>;
}
function SignupProfileEditor(props: ProfileStepProps) { return <ProfileFields {...props} signup />; }
function LocationButton({ disabled, onLocate }: { disabled: boolean; onLocate: () => void }) {
 const { service } = useProfile();
 return service?.source === "mock" ? <Button variant="secondary" disabled={disabled} onClick={onLocate}>현재 위치로 찾기</Button> : null;
}
function SignupRegionEditor(props: RegionStepProps) { return <RegionCandidates {...props} compact />; }
export const profileSignupEditors: SignupEditors = { Profile: SignupProfileEditor, Region: SignupRegionEditor };
