import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { getInstallmentStatus, getInstallmentStatusText } from "../display/installmentStatus";
import { toInstallmentRows } from "../utils/installmentRows";
import styles from "./AccountInstallmentsTab.module.css";

const MESSAGES = {
  loading: "Carregando as parcelas...",
  error: "Não foi possível carregar as parcelas",
  filtered: "Sem parcelas",
  empty: "Sem parcelas",
  emptyHint: "",
};

function versionName(version) {
  return version.amendmentNumber > 0 ? `Aditivo ${version.amendmentNumber}` : "Contrato original";
}

// O resumo em texto, da versão atual: "6 de 12 pagas · 1 atrasada · próxima 15/12 · R$ 723,66"
function summaryOf(current) {
  const paid = current.installments.filter((installment) => installment.status === "PAID").length;
  const overdue = current.installments.filter((installment) => installment.status === "OVERDUE").length;
  const next = current.installments.find((installment) => installment.status === "NEXT");
  return [
    `${paid} de ${current.installments.length} pagas`,
    overdue > 0 && `${overdue} ${overdue === 1 ? "atrasada" : "atrasadas"}`,
    next && `próxima ${formatDate(next.dueDate, "dd/MM")} · ${formatCurrency(next.remaining)}`,
  ].filter(Boolean);
}

// "Parcelado: R$ 2.338,00 em 3x (total R$ 3.006,00 − R$ 668,00 já pagos antes do aditivo)"
function SplitLine({ version }) {
  const hasPaidBefore = Number(version.paidBeforeAmendment) > 0;
  return (
    <span className={styles.split}>
      Parcelado: <strong>{formatCurrency(version.installmentBase)}</strong> em {version.installments.length}x
      {hasPaidBefore &&
        ` (total ${formatCurrency(version.totalValue)} − ${formatCurrency(version.paidBeforeAmendment)} já pagos antes do aditivo)`}
    </span>
  );
}

const columns = [
  {
    key: "number",
    header: "Parcela",
    cell: (row) =>
      row.kind === "replaced"
        ? range(row.installments, (installment) => `${installment.number}ª`)
        : `${row.installment.number}ª`,
  },
  {
    key: "dueDate",
    header: "Vencimento",
    cell: (row) =>
      row.kind === "replaced"
        ? range(row.installments, (installment) => formatDate(installment.dueDate))
        : formatDate(row.installment.dueDate),
  },
  {
    key: "value",
    header: "Valor",
    align: "right",
    cell: (row) => (row.kind === "replaced" ? replacedValue(row.installments) : formatCurrency(row.installment.value)),
  },
  {
    key: "paid",
    header: "Pago",
    align: "right",
    cell: (row) =>
      row.kind === "replaced" || Number(row.installment.paid) === 0 ? "—" : formatCurrency(row.installment.paid),
  },
  {
    key: "status",
    header: "Situação",
    cell: (row) => {
      if (row.kind === "replaced") return row.installments.length > 1 ? "substituídas pelo aditivo" : "substituída pelo aditivo";
      const status = getInstallmentStatus(row.installment.status);
      return <Badge variant={status.badge}>{getInstallmentStatusText(row.installment)}</Badge>;
    },
  },
];

// "7ª a 12ª" ou "7ª" (uma só)
function range(installments, format) {
  const first = format(installments[0]);
  return installments.length > 1 ? `${first} a ${format(installments.at(-1))}` : first;
}

// "R$ 723,66 cada" quando todas valem o mesmo; senão, os valores do primeiro e do último
function replacedValue(installments) {
  const values = installments.map((installment) => formatCurrency(installment.value));
  if (values.length === 1) return values[0];
  return values.every((value) => value === values.at(-1)) ? `${values.at(-1)} cada` : `${values[0]} a ${values.at(-1)}`;
}

// A aba Parcelas: o resumo em texto da versão atual e um bloco por versão, da mais nova para a
// mais antiga (as anteriores tracejadas). Cada tabela rola por dentro, com o cabeçalho parado; os
// blocos dividem o painel na proporção do conteúdo. versions: as do installments da API
export function AccountInstallmentsTab({ versions = [], isLoading = false, isError = false }) {
  if (isLoading || isError || versions.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={false} messages={MESSAGES} />;
  }

  const [paidPart, ...otherParts] = summaryOf(versions[0]);
  return (
    <div className={styles.installments}>
      <span className={styles.summary}>
        <strong>{paidPart}</strong>
        {otherParts.map((part) => ` · ${part}`).join("")}
      </span>

      {versions.map((version, index) => {
        const newer = versions[index - 1];
        const rows = toInstallmentRows(version.installments);
        return (
          <section
            key={version.contractId}
            className={[styles.block, !version.current && styles.blockDashed].filter(Boolean).join(" ")}
            style={{ flexGrow: rows.length + 2 }}
          >
            <div className={styles.blockHeader}>
              <h3 className={styles.blockTitle}>
                {versionName(version)}
                {version.current && " · versão atual"}
              </h3>
              <span className={styles.meta}>
                {version.current
                  ? `criado em ${formatDate(version.createdAt)} · vence todo dia ${version.dueDay}`
                  : `substituído pelo ${versionName(newer)} em ${formatDate(newer.createdAt)}`}
              </span>
            </div>
            <SplitLine version={version} />
            <DataTable
              columns={columns}
              rows={rows}
              getRowKey={(row) => row.key}
              getRowClassName={(row) => (row.kind === "replaced" ? styles.replaced : undefined)}
              compact
              className={styles.table}
            />
          </section>
        );
      })}
    </div>
  );
}
