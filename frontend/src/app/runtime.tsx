"use client";
import dynamic from "next/dynamic";
import { useMemo, type ReactNode } from "react";
import { createApiClient } from "../lib/api/client";
import { createFeatureServices } from "../features/integration";
import { FeatureServicesProvider } from "../features/integration/provider";
import { featureProviderProps } from "../features/integration/bindings";
import { AppProviders } from "./providers";
import { Notice } from "../components/ui/Notice";
const PrivyRuntime = dynamic(() => import("../features/auth/PrivyRuntime"), { ssr: false });
export const confirmedApiBase = "https://discushion-api.onrender.com/api/v1";
function PublicRuntime({ children, baseUrl }: { children: ReactNode; baseUrl: string }) {
  const { client, services } = useMemo(() => { const client = createApiClient({ baseUrl }); return { client, services: createFeatureServices(client) }; }, [baseUrl]);
  return <AppProviders apiClient={client} session={{ status: "guest" }} {...featureProviderProps(services)}>
    <Notice tone="warning">이 환경의 로그인 설정을 준비 중입니다. 공유 링크는 계속 확인할 수 있습니다.</Notice>
    <FeatureServicesProvider services={services} subjectKey={null}>{children}</FeatureServicesProvider>
  </AppProviders>;
}
export function RuntimeProviders({ children }: { children: ReactNode }) {
  const appId = process.env.NEXT_PUBLIC_PRIVY_APP_ID;
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || confirmedApiBase;
  if (!appId) return <PublicRuntime baseUrl={baseUrl}>{children}</PublicRuntime>;
  return <PrivyRuntime appId={appId} baseUrl={baseUrl}>{children}</PrivyRuntime>;
}
