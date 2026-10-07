"use client";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { createDraftMockService, type DraftMockMode } from "@/features/post-editor/draft";
import { emptyPostForm, type PostFormState } from "@/features/post-editor/model";
import { PostEditorScreen } from "@/features/post-editor/PostEditorScreen";

type Scenario = "LOCAL_AGENDA" | "LOCAL_ACTIVITY" | "VOTE";
function initialForm(type: Scenario): PostFormState { const form = emptyPostForm(type); return { ...form, title: `${type} draft`, content: "Draft form state remains visible after saving.", referenceLink: "https://example.invalid/reference", anonymous: type === "LOCAL_AGENDA", ...(type === "LOCAL_ACTIVITY" ? { activity: { source: "지역 문화센터", schedule: "2026-10-20 10:00", location: "공릉2동", status: "UPCOMING", link: "" } } : {}), ...(type === "VOTE" ? { vote: { question: "공원 운영 시간을 늘릴까요?", options: ["찬성", "반대"], endsAt: "2026-10-30T18:00", status: "OPEN" } } : {}) }; }
export function PostEditorPreview(){const [scenario,setScenario]=useState<Scenario>("LOCAL_AGENDA");const [mode,setMode]=useState<DraftMockMode>("success");return <div className="min-h-dvh bg-background p-4"><div className="mx-auto mb-3 flex max-w-mobile flex-wrap gap-2 rounded-card border p-3"><Button variant={scenario==="LOCAL_AGENDA"?"primary":"secondary"} onClick={()=>setScenario("LOCAL_AGENDA")}>지역 안건</Button><Button variant={scenario==="LOCAL_ACTIVITY"?"primary":"secondary"} onClick={()=>setScenario("LOCAL_ACTIVITY")}>활동 정보</Button><Button variant={scenario==="VOTE"?"primary":"secondary"} onClick={()=>setScenario("VOTE")}>투표</Button><Button variant={mode==="success"?"primary":"secondary"} onClick={()=>setMode("success")}>임시저장 성공</Button><Button variant={mode==="error"?"primary":"secondary"} onClick={()=>setMode("error")}>임시저장 실패</Button></div><PostEditorScreen key={`${scenario}-${mode}`} mode="create" initial={initialForm(scenario)} draftService={createDraftMockService(mode)}/></div>}
