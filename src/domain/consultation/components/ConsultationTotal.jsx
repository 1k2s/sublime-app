// Barra com o total de repasse do dia: soma os lançamentos já registrados hoje, não o carrinho
import { formatCurrency, sumMoney } from "@/shared/utils/money";
import styles from "./ConsultationTotal.module.css";

export function ConsultationTotal({ className, consultations = [], isLoading = false }) {
  const total = sumMoney(consultations.map((consultation) => consultation.providerPayoutValue));
  const countLabel = consultations.length === 1 ? "1 atendimento" : `${consultations.length} atendimentos`;

  return (
    <section className={[styles.bar, className].filter(Boolean).join(" ")} aria-live="polite" aria-busy={isLoading}>
      <h2 className={styles.label}>Total repasse do dia:</h2>
      <span className={styles.count}>{isLoading ? "Carregando..." : countLabel}</span>
      <span className={styles.value}>{isLoading ? "" : formatCurrency(total)}</span>
    </section>
  );
}
