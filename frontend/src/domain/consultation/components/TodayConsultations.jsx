// Lançamentos de hoje (só os ativos), com rolagem própria: a barra de lançamento fica fixa em
// cima. O recém-lançado fica destacado ("agora"), com o aviso de saldo da resposta do POST.
// Desfazer pede confirmação na própria linha (inativar não tem volta)
import { useState } from "react";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatCurrency } from "@/shared/utils/money";
import { getConsultationStatusLabel } from "../display/consultationStatuses";
import { getSessionWarning } from "../display/sessionWarning";
import styles from "./TodayConsultations.module.css";

const MESSAGES = {
  loading: "Carregando os lançamentos de hoje...",
  error: "Não foi possível carregar os lançamentos de hoje.",
  empty: "Nenhum lançamento hoje.",
  emptyHint: "Os atendimentos lançados aparecem aqui.",
};

// lastLaunched: { id, remainingSessions } do último lançamento desta tela
export function TodayConsultations({
  consultations = [],
  isLoading = false,
  isError = false,
  lastLaunched,
  onUndo,
  undoingId,
  className,
}) {
  const [confirmingId, setConfirmingId] = useState(null);

  if (isLoading || isError || consultations.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} messages={MESSAGES} className={className} />;
  }

  function confirmUndo(id) {
    setConfirmingId(null);
    onUndo(id);
  }

  return (
    <section className={[styles.card, className].filter(Boolean).join(" ")} aria-label="Lançamentos de hoje">
      <h2 className={styles.title}>Lançamentos de hoje</h2>
      <div className={styles.scroll}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Paciente</th>
              <th>Técnica</th>
              <th>Status</th>
              <th className={styles.money}>Valor</th>
              <th className={styles.money}>Repasse</th>
              <th>
                <span className={styles["sr-only"]}>Ações</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {consultations.map((consultation) => {
              const isLast = consultation.id === lastLaunched?.id;
              const warning = isLast ? getSessionWarning(lastLaunched.remainingSessions) : null;
              return (
                <tr key={consultation.id} className={isLast ? styles["row-last"] : undefined}>
                  <td>
                    <span className={styles.patient}>{consultation.patientName}</span>
                    {isLast && <span className={styles.now}>agora</span>}
                    {warning && <span className={styles[`warning-${warning.level}`]}>{warning.text}</span>}
                  </td>
                  <td>
                    {consultation.techniqueName}
                    {consultation.sessionDurationPriceId != null && ` · ${consultation.durationMinutes} min`}
                  </td>
                  <td>{getConsultationStatusLabel(consultation.status)}</td>
                  <td className={styles.money}>{formatCurrency(consultation.baseValue)}</td>
                  <td className={`${styles.money} ${styles.payout}`}>{formatCurrency(consultation.payoutValue)}</td>
                  <td className={styles.actions}>
                    {confirmingId === consultation.id ? (
                      <span className={styles.confirm}>
                        Desfazer?
                        <button type="button" className={styles["confirm-yes"]} onClick={() => confirmUndo(consultation.id)}>
                          Sim
                        </button>
                        <button type="button" className={styles.action} onClick={() => setConfirmingId(null)}>
                          Não
                        </button>
                      </span>
                    ) : (
                      <button
                        type="button"
                        className={styles.action}
                        disabled={undoingId === consultation.id}
                        onClick={() => setConfirmingId(consultation.id)}
                      >
                        {undoingId === consultation.id ? "Desfazendo..." : "Desfazer"}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
