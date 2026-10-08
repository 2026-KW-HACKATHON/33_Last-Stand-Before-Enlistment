import type { ReactNode } from "react";
import styles from "./PhoneFrame.module.css";

/** Development-demo shell only. Product routes keep rendering their mobile canvas directly. */
export function PhoneFrame({ children }: { children: ReactNode }) {
  return (
    <div className={styles.stage}>
      <section className={styles.phone} aria-label="Discushion 모바일 데모">
        <div aria-hidden="true" className={styles.camera}><span /></div>
        <div className={styles.screen}>{children}</div>
        <div aria-hidden="true" className={styles.homeIndicator} />
      </section>
    </div>
  );
}
