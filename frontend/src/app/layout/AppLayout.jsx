import { Outlet, useLocation } from "react-router-dom";
import { AppSidebar } from "./AppSidebar";
import styles from "./AppLayout.module.css";

export function AppLayout() {
  const { pathname } = useLocation();

  return (
    <div className={styles.shell}>
      <div className={styles.rail}>
        <AppSidebar />
      </div>
      <div className={styles.stage}>
        {/* key: cada tela recria o main (animação de entrada e rolagem no topo) */}
        <main key={pathname} className={styles.content}>
          <div className={styles.page}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
