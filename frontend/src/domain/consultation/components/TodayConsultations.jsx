// Lançamentos de hoje (só os ativos), na tabela padrão das listagens, com rolagem própria: a barra de
// lançamento fica fixa em cima. O recém-lançado fica destacado (a linha marcada e "agora"), com o
// aviso de saldo da resposta do POST. Desfazer pede confirmação na própria linha (inativar não tem
// volta)
import { useState } from "react";
import { DataTable } from "@/shared/components/DataTable";
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

  const columns = [
    {
      key: "patient",
      header: "Paciente",
      cell: (consultation) => {
        const isLast = consultation.id === lastLaunched?.id;
        const warning = isLast ? getSessionWarning(lastLaunched.remainingSessions) : null;
        return (
          <>
            {consultation.patientName}
            {isLast && <span className={`${styles.tag} ${styles.now}`}>agora</span>}
            {warning && <span className={`${styles.tag} ${styles[`warning-${warning.level}`]}`}>{warning.text}</span>}
          </>
        );
      },
    },
    {
      key: "technique",
      header: "Técnica",
      cell: (consultation) =>
        `${consultation.techniqueName}${consultation.sessionDurationPriceId != null ? ` · ${consultation.durationMinutes} min` : ""}`,
    },
    { key: "status", header: "Status", cell: (consultation) => getConsultationStatusLabel(consultation.status) },
    {
      key: "value",
      header: "Valor",
      align: "right",
      cell: (consultation) => <span className={styles.money}>{formatCurrency(consultation.baseValue)}</span>,
    },
    {
      key: "payout",
      header: "Repasse",
      align: "right",
      cell: (consultation) => <span className={styles.money}>{formatCurrency(consultation.payoutValue)}</span>,
    },
    {
      key: "actions",
      header: "Ações",
      align: "right",
      cell: (consultation) =>
        confirmingId === consultation.id ? (
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
        ),
    },
  ];

  return (
    <section className={[styles.section, className].filter(Boolean).join(" ")} aria-label="Lançamentos de hoje">
      <h2 className={styles.title}>Lançamentos de hoje</h2>
      <DataTable columns={columns} rows={consultations} selectedRowKey={lastLaunched?.id ?? null} />
    </section>
  );
}
