"use client";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
export function GuestMemberGate({ onLogin }: { onLogin: () => void }) { return <Notice tone="info"><p>회원 전용 기능입니다. 로그인 또는 가입 후 다시 눌러 주세요.</p><Button className="mt-2" onClick={onLogin}>로그인·가입</Button></Notice>; }
