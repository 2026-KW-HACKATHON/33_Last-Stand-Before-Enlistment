import { notFound } from "next/navigation";
import { AccountInfoPreview } from "./preview";
export default function Page() { if (process.env.NODE_ENV !== "development") notFound(); return <AccountInfoPreview/>; }
