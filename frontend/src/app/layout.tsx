import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { RuntimeProviders } from "./runtime";

export const metadata: Metadata = { title: "Discushion" };

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <body><RuntimeProviders>{children}</RuntimeProviders></body>
    </html>
  );
}
