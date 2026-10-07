import { notFound } from "next/navigation";
import { PostPreview } from "./preview";

export default function PostPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <PostPreview />;
}
