"use client";

import { PostEditorScreen } from "@/features/post-editor/PostEditorScreen";
import { useNavigation } from "@/lib/navigation";

export default function NewPostPage() {
  const navigation = useNavigation();
  return <PostEditorScreen mode="create" onCancel={() => navigation.navigate({ destination: { id: "home" } }, true)} />;
}
