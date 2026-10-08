import type { ReactNode } from "react";
import styles from "./PhoneFrame.module.css";

/** Device shell for desktop demo/product previews; compact viewports keep the mobile canvas direct. */
export function PhoneFrame({ children }: { children: ReactNode }) {
  return (
    <div className={styles.stage}>
      <div className={styles.scaleBox}>
        <section className={styles.phone} aria-label="Discushion 모바일 데모">
          <div aria-hidden="true" className={styles.camera}><span /></div>
          <div className={styles.screen}>
            <div aria-hidden="true" className={styles.statusBar}>
              <time className={styles.time}>9:41</time>
              <div className={styles.statusIndicators}>
                <span className={styles.signal}><i /><i /><i /><i /></span>
                <svg className={styles.wifi} viewBox="0 0 18 14" fill="none"><path d="M1.5 4.5A11.1 11.1 0 0 1 9 1.7c2.8 0 5.5 1 7.5 2.8M4.2 7.2A7 7 0 0 1 9 5.4c1.8 0 3.5.7 4.8 1.8M7 10a3 3 0 0 1 4 0M9 12.2h.01" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" /></svg>
                <span className={styles.battery}><i /></span>
              </div>
            </div>
            <div className={styles.appCanvas}>{children}</div>
          </div>
          <div aria-hidden="true" className={styles.homeIndicator} />
        </section>
      </div>
    </div>
  );
}
