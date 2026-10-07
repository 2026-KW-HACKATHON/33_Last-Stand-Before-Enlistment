import { PostDetailScreen } from "@/features/post/PostDetail";

/** #53 owns the Page shell. #67/#15 wire an agreed service; production never falls back to Mock data. */
export default async function PostPage({ params }: { params: Promise<{ postId: string }> }) {
  await params;
  return <PostDetailScreen state={{ kind: "error", message: "게시물 조회 연결이 아직 준비되지 않았습니다." }} />;
}
