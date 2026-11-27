// Resumo do dia no cabeçalho: a data, a quantidade de atendimentos realizados, o rendimento do dia
// e as faltas cobradas à parte, como o backend devolve (o front não soma). É a mesma regra do
// rendimento do dashboard: só os realizados, pela data do lançamento (ver "Rendimento" em
// Consultation, no domain-model)
import { formatDate } from "@/shared/utils/date";
import { PageLoader } from "@/shared/components/PageLoader";
import { formatCurrency } from "@/shared/utils/money";
import { formatChargedAbsenceCount, formatConsultationCount } from "../display/earningsLabels";
import styles from "./DaySummary.module.css";

export function DaySummary({ summary, isLoading = false }) {
  if (isLoading || !summary) {
    return <PageLoader variant="compact" label="Carregando o resumo do dia..." />;
  }

  return (
    <div className={styles.summary} aria-live="polite">
      <div className={styles.block}>
        <span className={styles.caption}>Hoje, {formatDate(summary.date, "EEEE, d 'de' MMMM")}</span>
        <span className={styles.count}>{formatConsultationCount(summary.consultationCount)}</span>
      </div>
      <div className={styles.block}>
        <span className={styles.caption}>Faltas cobradas</span>
        <span className={styles.absence}>
          {formatCurrency(summary.chargedAbsencePayoutTotal)}
          <span className={styles.absenceCount}>{formatChargedAbsenceCount(summary.chargedAbsenceCount)}</span>
        </span>
      </div>
      <div className={styles.block}>
        <span className={styles.caption}>Rendimento do dia</span>
        <span className={styles.total}>{formatCurrency(summary.payoutTotal)}</span>
      </div>
    </div>
  );
}
