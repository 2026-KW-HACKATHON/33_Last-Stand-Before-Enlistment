"use client";

import { useMemo, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ApiClientProvider } from "../lib/api/provider";
import type { ApiClient } from "../lib/api/types";
import { NavigationProvider, SessionProvider, destinationFromPathname, type SessionState } from "../lib/navigation";
import { LoginProvider } from "../features/auth/provider";
import type { OtpLoginService } from "../features/auth/contracts";
import { SignupProvider } from "../features/signup/provider";
import type { SignupEditors, SignupService } from "../features/signup/contracts";

/** Real adapters/client are injected by Integration; no production Mock or guessed base URL. */
export function AppProviders({ children, session, retrySession, apiClient, loginService, signupService, signupEditors }: {
  children: ReactNode; session?: SessionState; retrySession?: () => void | Promise<void>; apiClient?: ApiClient; loginService?: OtpLoginService; signupService?: SignupService; signupEditors?: SignupEditors;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const currentDestination = useMemo(() => destinationFromPathname(pathname), [pathname]);
  return <SessionProvider session={session} retry={retrySession}>
    <NavigationProvider currentDestination={currentDestination} onNavigate={(href, replace) => replace ? router.replace(href) : router.push(href)}>
      <LoginProvider service={loginService}>
        <SignupProvider service={signupService} editors={signupEditors}>
          {apiClient ? <ApiClientProvider client={apiClient}>{children}</ApiClientProvider> : children}
        </SignupProvider>
      </LoginProvider>
    </NavigationProvider>
  </SessionProvider>;
}
