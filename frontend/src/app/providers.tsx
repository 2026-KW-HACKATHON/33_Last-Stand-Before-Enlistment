"use client";

import { useMemo, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ApiClientProvider } from "../lib/api/provider";
import type { ApiClient } from "../lib/api/types";
import { NavigationProvider, SessionProvider, destinationFromPathname, type SessionState } from "../lib/navigation";

/** Real adapters/client are injected by Integration; no production Mock or guessed base URL. */
export function AppProviders({ children, session, retrySession, apiClient }: {
  children: ReactNode; session?: SessionState; retrySession?: () => void | Promise<void>; apiClient?: ApiClient;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const currentDestination = useMemo(() => destinationFromPathname(pathname), [pathname]);
  return <SessionProvider session={session} retry={retrySession}>
    <NavigationProvider currentDestination={currentDestination} onNavigate={(href, replace) => replace ? router.replace(href) : router.push(href)}>
      {apiClient ? <ApiClientProvider client={apiClient}>{children}</ApiClientProvider> : children}
    </NavigationProvider>
  </SessionProvider>;
}
