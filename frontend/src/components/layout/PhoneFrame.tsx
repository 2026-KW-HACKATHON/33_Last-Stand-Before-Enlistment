import type { ReactNode } from "react";
import styles from "./PhoneFrame.module.css";

/** Device shell for desktop demo/product previews; compact viewports keep the mobile canvas direct. */
export function PhoneFrame({ children }: { children: ReactNode }) {
  return (
    <div className={styles.stage}>
      <div className={styles.scaleBox}>
        <section className={styles.phone} aria-label="Discushion 모바일 데모">
          <div aria-hidden="true" className={styles.camera}><span /></div>
          <div className={styles.screen}>{children}</div>
          <div aria-hidden="true" className={styles.homeIndicator} />
        </section>
      </div>
    </div>
  );
}
