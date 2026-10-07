"use client";

import Image from "next/image";
import { useEffect, useRef } from "react";
import { MobileLayout } from "../../components/layout/MobileLayout";
import { Button } from "../../components/ui/Button";
import { Notice } from "../../components/ui/Notice";
import { useNavigation, useSession } from "../../lib/navigation";
import { useLogin } from "./provider";
import styles from "./start.module.css";

export function StartScreen() {
  const navigation = useNavigation();
  const { session, retry } = useSession();
  const { state } = useLogin();
  const redirected = useRef(false);
  useEffect(() => {
    if (session.status === "member" && !redirected.current) {
      redirected.current = true;
      navigation.navigate({ destination: { id: "home" } }, true);
    }
  }, [session.status, navigation]);
  return (
    <MobileLayout contentClassName={styles.content}>
      <div className={styles.brand}>
        <Image src="/images/start.png" alt="Discushion" width={336} height={224} priority className={styles.image} />
        <h1 className={styles.slogan}>우리 동네의 내일을 더 편안한 대화로</h1>
      </div>
      <div className={`flex flex-col gap-section ${styles.actions}`}>
        {session.status === "loading" && <Notice role="status">로그인 상태 확인 중입니다.</Notice>}
        {session.status === "error" && <Notice tone="error" role="status">로그인 상태를 확인하지 못했습니다.</Notice>}
        {session.status === "error" && retry && <Button variant="secondary" onClick={() => void retry()}>다시 확인</Button>}
        {state.failure?.reason === "cancelled" && <Notice role="status">로그인을 취소했습니다.</Notice>}
        <Button onClick={() => navigation.beginAuthentication({ destination: { id: "home" } })}>로그인</Button>
        <Button variant="secondary" onClick={() => navigation.beginAuthentication({ destination: { id: "home" } })}>회원가입</Button>
      </div>
    </MobileLayout>
  );
}
