"use client";
import { createContext, useContext, type ReactNode } from "react";
import type { FeatureServices } from "./index";
const Context = createContext<FeatureServices | null>(null);
/** Root registration belongs to A. A must change subjectKey on login/logout/account changes. */
export function FeatureServicesProvider({ services, subjectKey, children }: { services: FeatureServices | null; subjectKey: string | null; children: ReactNode }) {
  return <Context.Provider key={subjectKey ?? "guest"} value={services}>{children}</Context.Provider>;
}
export function useFeatureServices() { return useContext(Context); }
