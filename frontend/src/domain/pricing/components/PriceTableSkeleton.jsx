// Esqueleto da tabela de preços enquanto o grupo carrega: a mesma moldura, com blocos no lugar
// dos planos, dos valores e da vigência
import { Skeleton } from "@/shared/components/Skeleton";
import styles from "./PriceTableSkeleton.module.css";

const ROWS = 6;
const VALUE_COLUMNS = 2;

export function PriceTableSkeleton() {
  return (
    <section className={styles.card} aria-busy="true" aria-label="Carregando preços">
      <div className={styles.header}>
        <Skeleton className={styles.headerText} />
        {Array.from({ length: VALUE_COLUMNS }, (_, index) => (
          <Skeleton key={index} className={styles.headerText} />
        ))}
        <Skeleton className={`${styles.headerText} ${styles.validityColumn}`} />
        <span />
      </div>
      {Array.from({ length: ROWS }, (_, row) => (
        <div key={row} className={styles.row}>
          <Skeleton className={styles.plan} />
          {Array.from({ length: VALUE_COLUMNS }, (_, index) => (
            <Skeleton key={index} className={styles.value} />
          ))}
          <Skeleton className={`${styles.validity} ${styles.validityColumn}`} />
          <Skeleton variant="circle" className={styles.icon} />
        </div>
      ))}
    </section>
  );
}
