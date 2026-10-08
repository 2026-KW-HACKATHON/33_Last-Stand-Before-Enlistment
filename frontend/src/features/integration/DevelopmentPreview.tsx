"use client";
import { useMemo, useState } from "react";
import { AppProviders } from "../../app/providers";
import { createApiClient } from "../../lib/api";
import type { SessionState } from "../../lib/navigation";
import { createFeatureServices } from "./index";
import { FeatureServicesProvider } from "./provider";
import { featureProviderProps } from "./bindings";
import { ExploreHost } from "./ExploreHost";
import { PostHost } from "./PostHost";
import { EditorHost } from "./EditorHost";
import { Notice } from "../../components/ui/Notice";
const session: SessionState = { status: "member", capabilities: { status: "ready", grants: [{ capability: { kind: "neighbor-region", regionId: "1" }, allowed: true }, { capability: { kind: "institution" }, allowed: true }, { capability: { kind: "responsible-region", regionId: "1" }, allowed: true }, { capability: { kind: "edit-post", postId: "101" }, allowed: true }] } };
/** Local HTTP fixture harness only. The page is unavailable in production. */
export function DevelopmentPreview() {
  const [screen, setScreen] = useState<"home" | "board" | "map" | "post" | "create" | "edit">("post");
  const client = useMemo(() => createApiClient({ baseUrl: "http://127.0.0.1:4199/api/v1" }), []);
  const services = useMemo(() => createFeatureServices(client), [client]);
  return <FeatureServicesProvider services={services} subjectKey="23"><AppProviders session={session} subjectKey="23" apiClient={client} {...featureProviderProps(services)}>
    <Notice>로컬 계약 fixture 검증용 · 실제 계정·배포 API 검증 아님</Notice>
    <nav aria-label="통합 검증 화면">{(["home", "board", "map", "post", "create", "edit"] as const).map(value => <button key={value} type="button" className="m-1 rounded border p-2" onClick={() => setScreen(value)}>{value}</button>)}</nav>
    {screen === "post" ? <PostHost postId="101" /> : screen === "create" ? <EditorHost /> : screen === "edit" ? <EditorHost postId="101" /> : <ExploreHost key={screen} kind={screen} />}
  </AppProviders></FeatureServicesProvider>;
}
