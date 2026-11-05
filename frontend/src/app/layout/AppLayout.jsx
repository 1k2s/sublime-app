import { Outlet } from 'react-router-dom';
import { AppSidebar } from './AppSidebar';
import styles from './AppLayout.module.css';

export function AppLayout() {
  return (
    <div className={styles.layout}>
      <AppSidebar />
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
