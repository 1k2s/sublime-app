// Resumo do dia no cabeçalho: a data, a quantidade de atendimentos e o repasse, como o backend
// devolve (o front não soma)
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import styles from "./DaySummary.module.css";

export function DaySummary({ summary, isLoading = false }) {
  if (isLoading || !summary) {
    return <p className={styles.loading}>Carregando o resumo do dia...</p>;
  }

  const countLabel = summary.consultationCount === 1 ? "1 atendimento" : `${summary.consultationCount} atendimentos`;

  return (
    <div className={styles.summary} aria-live="polite">
      <div className={styles.block}>
        <span className={styles.caption}>Hoje, {formatDate(summary.date, "EEEE, d 'de' MMMM")}</span>
        <span className={styles.count}>{countLabel}</span>
      </div>
      <div className={styles.block}>
        <span className={styles.caption}>Repasse do dia</span>
        <span className={styles.total}>{formatCurrency(summary.payoutTotal)}</span>
      </div>
    </div>
  );
}
