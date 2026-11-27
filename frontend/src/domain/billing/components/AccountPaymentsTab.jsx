import { useState } from "react";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { DataTable } from "@/shared/components/DataTable";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency, formatPercentage } from "@/shared/utils/money";
import { getPaymentMethodLabel } from "@/domain/contract/display/paymentMethods";
import { formatContractLabel } from "../display/accountLabels";
import styles from "./AccountPaymentsTab.module.css";

const MESSAGES = {
  loading: "Carregando os pagamentos...",
  error: "Não foi possível carregar os pagamentos",
  filtered: "Nenhum pagamento",
  empty: "Nenhum pagamento registrado",
  emptyHint: "Clique em Registrar pagamento para começar.",
};

// O contrato embaixo da data: "Evolução · Aditivo 1" ou "Avulso"
function contractOf(payment) {
  return payment.contractId
    ? formatContractLabel({ planName: payment.contractPlanName, amendmentNumber: payment.contractAmendmentNumber })
    : "Avulso";
}

// O "parcelado em" do crédito: "à vista (1x)" ou "3x"
function cardOf(payment) {
  if (payment.paymentMethod !== "CREDIT_CARD") return null;
  return payment.cardInstallments === 1 ? "à vista (1x)" : `${payment.cardInstallments}x`;
}

// "de Evolução para Evolução · Aditivo 1": os dois contratos da transferência de saldo
function transferContractsOf(transfer) {
  const from = formatContractLabel({ planName: transfer.fromPlanName, amendmentNumber: transfer.fromAmendmentNumber });
  const to = formatContractLabel({ planName: transfer.toPlanName, amendmentNumber: transfer.toAmendmentNumber });
  return `de ${from} para ${to}`;
}

// Os pagamentos e as transferências de saldo numa lista só, do mais recente para o mais antigo
// (pela data; no mesmo dia, o pagamento antes)
function toRows(payments, transfers) {
  const rows = [
    ...payments.map((payment) => ({ kind: "payment", key: `payment-${payment.id}`, date: payment.paidAt, payment })),
    ...transfers.map((transfer) => ({
      kind: "transfer",
      key: `transfer-${transfer.id}`,
      date: transfer.transferredAt,
      transfer,
    })),
  ];
  return rows.sort((first, second) => second.date.localeCompare(first.date));
}

// A aba Pagamentos: os pagamentos do paciente, ativos e inativos, e o crédito de contratos
// anteriores aplicado num contrato novo (a transferência de saldo, que não é dinheiro: sem taxa e
// sem "Inativar"). O pagamento inativado fica apagado, com a data e o bruto riscados, sem o botão.
// "Inativar" pede confirmação (corrige um registro errado: não é estorno). payments e transfers: os
// da API
export function AccountPaymentsTab({
  payments = [],
  transfers = [],
  isLoading = false,
  isError = false,
  isDeactivating = false,
  onDeactivate,
}) {
  const [confirming, setConfirming] = useState(null);

  if (isLoading || isError || payments.length + transfers.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={false} messages={MESSAGES} />;
  }

  function confirmDeactivate() {
    onDeactivate?.(confirming);
    setConfirming(null);
  }

  const columns = [
    {
      key: "date",
      header: "Data",
      cell: (row) => {
        const isOff = row.kind === "payment" && !row.payment.active;
        return (
          <div className={styles.stack}>
            <span className={[styles.name, isOff && styles.strike].filter(Boolean).join(" ")}>{formatDate(row.date)}</span>
            <span className={styles.detail}>
              {row.kind === "payment" ? contractOf(row.payment) : transferContractsOf(row.transfer)}
            </span>
          </div>
        );
      },
    },
    {
      key: "method",
      header: "Forma",
      cell: (row) =>
        row.kind === "payment" ? (
          <div className={styles.stack}>
            <span className={styles.name}>{getPaymentMethodLabel(row.payment.paymentMethod)}</span>
            {cardOf(row.payment) && <span className={styles.detail}>{cardOf(row.payment)}</span>}
          </div>
        ) : (
          <span className={styles.name}>Crédito aplicado</span>
        ),
    },
    {
      key: "amount",
      header: "Bruto",
      align: "right",
      cell: (row) => {
        const amount = row.kind === "payment" ? row.payment.amount : row.transfer.amount;
        const isOff = row.kind === "payment" && !row.payment.active;
        return <span className={[styles.name, isOff && styles.strike].filter(Boolean).join(" ")}>{formatCurrency(amount)}</span>;
      },
    },
    {
      key: "fee",
      header: "Taxa",
      align: "right",
      cell: (row) =>
        row.kind === "payment" && Number(row.payment.fee) > 0
          ? `${formatCurrency(row.payment.fee)} · ${formatPercentage(row.payment.feePercentage)}`
          : "—",
    },
    {
      key: "net",
      header: "Líquido",
      align: "right",
      cell: (row) => (row.kind === "payment" ? formatCurrency(row.payment.netAmount) : "—"),
    },
    {
      key: "status",
      header: "Status",
      cell: (row) => {
        if (row.kind === "transfer") return <Badge variant="suggestion">Transferência</Badge>;
        return (
          <Badge variant={row.payment.active ? "active" : "inactive"}>{row.payment.active ? "Ativo" : "Inativado"}</Badge>
        );
      },
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (row) =>
        row.kind === "payment" &&
        row.payment.active && (
          <button
            type="button"
            className={styles.action}
            onClick={() => setConfirming(row.payment)}
            disabled={isDeactivating}
          >
            Inativar
          </button>
        ),
    },
  ];

  return (
    <div className={styles.payments}>
      {confirming && (
        <div className={styles.confirmBox}>
          <p className={styles.confirmText}>
            Inativar o pagamento de {formatCurrency(confirming.amount)} de {formatDate(confirming.paidAt)}? Ele deixa de
            contar na conta. Use só para corrigir um registro errado (o certo é registrado de novo).
          </p>
          <div className={styles.actions}>
            <Button variant="outline" onClick={() => setConfirming(null)}>
              Voltar
            </Button>
            <Button variant="danger" onClick={confirmDeactivate} disabled={isDeactivating}>
              Confirmar inativação
            </Button>
          </div>
        </div>
      )}
      <DataTable
        columns={columns}
        rows={toRows(payments, transfers)}
        getRowKey={(row) => row.key}
        getRowClassName={(row) => (row.kind === "payment" && !row.payment.active ? styles.off : undefined)}
        compact
        className={styles.table}
      />
    </div>
  );
}
