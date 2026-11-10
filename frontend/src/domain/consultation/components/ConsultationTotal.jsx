// Barra com o total de repasse do dia: soma o payoutValue (calculado pela API) dos lançamentos ativos de hoje,
// não o carrinho. Inativos não contam no repasse
import { formatCurrency, sumMoney } from "@/shared/utils/money";
import styles from "./ConsultationTotal.module.css";

export function ConsultationTotal({ className, consultations = [], isLoading = false }) {
  const active = consultations.filter((consultation) => consultation.active);
  const total = sumMoney(active.map((consultation) => consultation.payoutValue));
  const countLabel = active.length === 1 ? "1 atendimento" : `${active.length} atendimentos`;

  return (
    <section className={[styles["total-bar"], className].filter(Boolean).join(" ")} aria-live="polite" aria-busy={isLoading}>
      <h2 className={styles["total-label"]}>Total repasse do dia:</h2>
      <span className={styles["total-count"]}>{isLoading ? "Carregando..." : countLabel}</span>
      <span className={styles["total-value"]}>{isLoading ? "" : formatCurrency(total)}</span>
    </section>
  );
}
