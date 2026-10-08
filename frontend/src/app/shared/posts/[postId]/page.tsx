import { SharedHost } from "@/features/integration/SharedHost";
export default async function SharedPostPage({ params, searchParams }: { params: Promise<{ postId: string }>; searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const { postId } = await params, query = await searchParams;
  return <SharedHost postId={postId} token={typeof query.token === "string" ? query.token : null} />;
}
