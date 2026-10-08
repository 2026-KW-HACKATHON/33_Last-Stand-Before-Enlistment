import type { ReactNode } from "react";
import { PhoneFrame } from "../../components/layout/PhoneFrame";

/** All /dev previews share one device shell without affecting product routes. */
export default function DevelopmentLayout({ children }: { children: ReactNode }) {
  return <PhoneFrame>{children}</PhoneFrame>;
}
