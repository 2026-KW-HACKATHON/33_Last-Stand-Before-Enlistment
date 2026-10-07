"use client";

import { Input } from "../../components/ui/Input";
import { TextArea } from "../../components/ui/TextArea";
import { Notice } from "../../components/ui/Notice";
import type { ProfileStepProps, RegionStepProps, SignupEditors } from "./contracts";
import { signupAttributeFixtures, signupRegionFixtures } from "./mock";

/** DEV ONLY: #42 stage/props exercise. #43 owns real photo/edit/search/location UI and Service. */
function ProfileStub({ value, onChange, disabled }: ProfileStepProps) {
  return <>
    <Notice>#43 연결용 개발 stub · 프로필 사진/중복 조회는 실행하지 않습니다.</Notice>
    <Input label="닉네임" value={value.nickname} disabled={disabled} onChange={event => onChange({ ...value, nickname: event.currentTarget.value })} placeholder="닉네임" aria-invalid={Array.from(value.nickname.trim()).length > 10} />
    <Notice>중복 없이 최대 10자</Notice>
    <TextArea label="한 줄 소개 (선택)" value={value.bio} disabled={disabled} onChange={event => onChange({ ...value, bio: event.currentTarget.value })} aria-invalid={Array.from(value.bio).length > 50} />
    <fieldset disabled={disabled} className="flex flex-wrap gap-internal">
      <legend className="mb-internal text-caption">이웃 속성 · 복수 선택</legend>
      {signupAttributeFixtures.map(label => <label key={label} className={`flex items-center gap-internal rounded-chip px-page py-internal text-caption ${value.attributes.includes(label) ? "bg-selected text-surface" : "bg-disabled text-secondary"}`}>
        <input type="checkbox" checked={value.attributes.includes(label)} onChange={event => onChange({ ...value, attributes: event.currentTarget.checked ? [...value.attributes, label] : value.attributes.filter(item => item !== label) })} />{label}
      </label>)}
    </fieldset>
  </>;
}
function RegionStub({ value, onChange, disabled }: RegionStepProps) {
  return <>
    <Notice>#43 연결용 개발 stub · 실제 지역 조회/현재 위치는 실행하지 않습니다.</Notice>
    <fieldset disabled={disabled} className="flex flex-col gap-section">
      <legend className="mb-internal text-caption">활동 지역 후보 (Mock)</legend>
      {signupRegionFixtures.map(region => <label key={region.reference} className="flex items-center gap-internal rounded-card border border-border bg-surface px-page py-section">
        <input name="signup-region" type="radio" checked={value?.reference === region.reference} onChange={() => onChange(region)} />{region.label}
      </label>)}
      <label className="flex items-center gap-internal text-caption"><input name="signup-region" type="radio" checked={!value} onChange={() => onChange(null)} />선택 안 함</label>
    </fieldset>
  </>;
}
export const signupEditorStubs: SignupEditors = { Profile: ProfileStub, Region: RegionStub };
