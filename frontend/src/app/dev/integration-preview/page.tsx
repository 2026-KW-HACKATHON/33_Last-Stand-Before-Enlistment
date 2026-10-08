import { notFound } from "next/navigation";
import { DevelopmentPreview } from "@/features/integration/DevelopmentPreview";
export default function Page() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <DevelopmentPreview />;
}
