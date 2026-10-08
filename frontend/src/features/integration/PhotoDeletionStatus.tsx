"use client";
import { useState } from "react";
import type { FeatureServices } from "./index";
import { Notice } from "../../components/ui/Notice";
import { Button } from "../../components/ui/Button";
export function PhotoDeletionStatus({ postId, services }: { postId: string; services: FeatureServices }) {
  const ids = services.getPendingPhotoDeletions(postId);
  const [status, setStatus] = useState("사진 삭제가 예약되었습니다. 실제 파일 삭제 완료는 별도 확인이 필요합니다.");
  const [pending, setPending] = useState(false);
  if (!ids.length) return null;
  const check = async () => {
    setPending(true);
    try { const rows = await Promise.all(ids.map(id => services.photoService.get(id))); setStatus(rows.every(row => row.deletionCompleted) ? "실제 사진 파일 삭제가 완료되었습니다." : "사진 파일 삭제 대기 중입니다. 잠시 후 다시 확인해 주세요."); }
    catch { setStatus("사진 삭제 상태를 확인하지 못했습니다. 다시 시도해 주세요."); }
    finally { setPending(false); }
  };
  return <Notice role="status">{status}<Button disabled={pending} onClick={() => void check()}>파일 삭제 상태 확인</Button></Notice>;
}
