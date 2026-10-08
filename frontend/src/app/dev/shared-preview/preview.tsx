"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Notice } from "@/components/ui/Notice";
import { postFixtures } from "@/features/post/mock";
import { LoginScreen } from "@/features/auth/LoginScreen";
import { createMockLoginService, loginScenarios, type LoginScenario } from "@/features/auth/mock";
import { LoginProvider } from "@/features/auth/provider";
import { createShareMockService, type ShareMockMode } from "@/features/share/mock";
import { SharedPostScreen } from "@/features/share/SharedPostScreen";
import { NavigationProvider, SessionProvider, destinationFromPathname, type SessionState } from "@/lib/navigation";

const member: SessionState = { status: "member", capabilities: { status: "ready", grants: [] } };
const sharedPath = `/shared/posts/${postFixtures["agenda-photo"].id}`;

function PreviewScreen({ target, shareService, mode }: {
  target: string;
  shareService: ReturnType<typeof createShareMockService>;
  mode: "ready" | "loading" | "error" | "unavailable";
}) {
  const destination = destinationFromPathname(target);
  if (destination?.id === "login") return <LoginScreen />;
  if (destination?.id === "post") {
    return <Notice role="status">로그인 후 원 게시물 상세로 복귀했습니다. 회원 기능은 사용자가 다시 눌러야 실행됩니다.</Notice>;
  }
  return <SharedPostScreen postId={postFixtures["agenda-photo"].id} shareService={shareService} mode={mode} />;
}

/** Uses the real Navigation/Login contracts instead of preview-only returnTo state. */
export function SharedPreview() {
  const [mode, setMode] = useState<"ready" | "loading" | "error" | "unavailable">("ready");
  const [copyMode, setCopyMode] = useState<ShareMockMode>("success");
  const [scenario, setScenario] = useState<LoginScenario>("member");
  const [target, setTarget] = useState(sharedPath);
  const [session, setSession] = useState<SessionState>({ status: "guest" });
  const shareService = useMemo(() => createShareMockService(copyMode), [copyMode]);
  const loginService = useMemo(() => createMockLoginService(scenario), [scenario]);
  const destination = destinationFromPathname(target);

  return <>
    <div className="mx-auto flex max-w-mobile flex-col gap-2 p-page">
      <div className="flex flex-wrap gap-2">
        <Button onClick={() => { setSession({ status: "guest" }); setTarget(sharedPath); setMode("ready"); }}>공유 상세</Button>
        <Button variant="secondary" onClick={() => setMode("loading")}>Loading</Button>
        <Button variant="secondary" onClick={() => setMode("error")}>Error</Button>
        <Button variant="secondary" onClick={() => setMode("unavailable")}>Unavailable</Button>
        <Button variant={copyMode === "success" ? "primary" : "secondary"} onClick={() => setCopyMode(copyMode === "success" ? "error" : "success")}>복사 {copyMode}</Button>
      </div>
      <label className="text-caption">로그인 Mock
        <select className="ml-2 rounded-input border border-border p-internal" value={scenario} onChange={event => setScenario(event.target.value as LoginScenario)}>
          {Object.entries(loginScenarios).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
    </div>
    <SessionProvider session={session}>
      <NavigationProvider currentDestination={destination} onNavigate={href => {
        if (href.startsWith("/posts/")) setSession(member);
        setTarget(href);
      }}>
        <LoginProvider service={loginService}>
          <PreviewScreen target={target} shareService={shareService} mode={mode} />
        </LoginProvider>
      </NavigationProvider>
    </SessionProvider>
  </>;
}
