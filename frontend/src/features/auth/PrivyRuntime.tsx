"use client";
import { PrivyProvider, usePrivy, useLoginWithEmail } from "@privy-io/react-auth";
import { useEffect, useState, useSyncExternalStore, type ReactNode } from "react";
import { AppProviders } from "../../app/providers";
import { FeatureServicesProvider } from "../integration/provider";
import { featureProviderProps } from "../integration/bindings";
import { createAuthRuntime, createPrivyPortBridge } from "./api-adapter";
function Bridge({ children, baseUrl }: { children: ReactNode; baseUrl: string }) {
  const privy = usePrivy(), { sendCode, loginWithCode } = useLoginWithEmail();
  const [bridge] = useState(() => createPrivyPortBridge({ ready: false, authenticated: false, principal: null, getAccessToken: privy.getAccessToken, sendCode: value => sendCode({ email: value }), loginWithCode: code => loginWithCode({ code }), logout: privy.logout }));
  useEffect(() => {
    bridge.update({ ready: privy.ready, authenticated: privy.authenticated, principal: privy.user?.id ?? null, getAccessToken: privy.getAccessToken, sendCode: value => sendCode({ email: value }), loginWithCode: async code => { await loginWithCode({ code }); await bridge.waitForAuthentication(); }, logout: privy.logout });
  }, [bridge, privy.ready, privy.authenticated, privy.user?.id, privy.getAccessToken, privy.logout, sendCode, loginWithCode]);
  const [runtime] = useState(() => createAuthRuntime(baseUrl, bridge.read));
  const snapshot = useSyncExternalStore(runtime.subscribe, runtime.getSnapshot, runtime.getSnapshot);
  useEffect(() => { runtime.providerChanged(); }, [runtime, privy.ready, privy.authenticated, privy.user?.id]);
  useEffect(() => () => runtime.dispose(), [runtime]);
  return <AppProviders session={snapshot.session} retrySession={runtime.refresh} subjectKey={snapshot.subjectKey} authScopeKey={snapshot.principal ?? null}
    apiClient={runtime.client} loginService={runtime.loginService} signupService={runtime.signupService} logoutService={runtime.logoutService}
    {...featureProviderProps(runtime.services)}>
    <FeatureServicesProvider services={runtime.services} subjectKey={snapshot.subjectKey}>{children}</FeatureServicesProvider>
  </AppProviders>;
}
export default function PrivyRuntime({ children, appId, baseUrl }: { children: ReactNode; appId: string; baseUrl: string }) {
  return <PrivyProvider appId={appId} config={{ loginMethods: ["email"], embeddedWallets: { ethereum: { createOnLogin: "off" }, solana: { createOnLogin: "off" } } }}><Bridge baseUrl={baseUrl}>{children}</Bridge></PrivyProvider>;
}
