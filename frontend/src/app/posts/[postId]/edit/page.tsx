import { EditorHost } from "@/features/integration/EditorHost";
export default async function EditPostPage({ params }: { params: Promise<{ postId: string }> }) {
  const { postId } = await params;
  return <EditorHost key={postId} postId={postId} />;
}
