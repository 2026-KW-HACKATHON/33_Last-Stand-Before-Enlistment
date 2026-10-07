"use client";

import { createContext, useContext, type ReactNode } from "react";
import type { ApiClient } from "./types";

const ApiClientContext = createContext<ApiClient | null>(null);

/** Create a stable client at the composition boundary; consumers never choose Mock vs API. */
export function ApiClientProvider({ client, children }: { client: ApiClient; children: ReactNode }) {
  return <ApiClientContext.Provider value={client}>{children}</ApiClientContext.Provider>;
}

export function useApiClient(): ApiClient {
  const client = useContext(ApiClientContext);
  if (!client) throw new Error("useApiClient requires ApiClientProvider.");
  return client;
}
