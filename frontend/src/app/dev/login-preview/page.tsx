import { notFound } from "next/navigation";
import { LoginPreview } from "./preview";

export default function LoginPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <LoginPreview />;
}
