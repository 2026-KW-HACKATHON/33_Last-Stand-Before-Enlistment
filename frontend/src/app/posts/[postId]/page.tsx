import { PostHost } from "@/features/integration/PostHost";
export default async function PostPage({ params }: { params: Promise<{ postId: string }> }) {
  const { postId } = await params;
  return <PostHost key={postId} postId={postId} />;
}
