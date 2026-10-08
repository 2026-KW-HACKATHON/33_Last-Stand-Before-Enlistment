import { notFound } from "next/navigation";
import { SignupPreview } from "./preview";

export default function SignupPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <SignupPreview />;
}
