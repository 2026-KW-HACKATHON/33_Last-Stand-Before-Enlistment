import { notFound } from "next/navigation";
import { ExplorePreview } from "./preview";

export default function ExplorePreviewPage() { if (process.env.NODE_ENV !== "development") notFound(); return <ExplorePreview />; }
