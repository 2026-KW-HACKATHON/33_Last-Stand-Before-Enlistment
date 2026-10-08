import { notFound } from "next/navigation";
import { UiPreview } from "./preview";

export default function UiPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <UiPreview />;
}
