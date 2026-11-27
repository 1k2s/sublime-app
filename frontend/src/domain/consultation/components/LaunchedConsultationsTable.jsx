// Atendimentos do período, pela data do lançamento (a do rendimento), com o filtro por status.
// A altura é do pai: o título e o filtro ficam parados, e a tabela rola por dentro.
// Mostra as duas datas: o retroativo aparece pela diferença entre elas. O desfeito (inativo)
// continua na lista, marcado, e não entra no total (o total vem do backend)
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { ListMessage } from "@/shared/components/ListMessage";
import { Select } from "@/shared/components/Select";
import { formatDate, formatDateTime } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { CONSULTATION_STATUS_OPTIONS, getConsultationStatusLabel } from "../display/consultationStatuses";
import { ALL_STATUSES } from "../utils/filterConsultations";
import styles from "./LaunchedConsultationsTable.module.css";

const STATUS_FILTER_OPTIONS = [{ value: ALL_STATUSES, label: "Todos" }, ...CONSULTATION_STATUS_OPTIONS];

const MESSAGES = {
  loading: "Carregando os atendimentos do período...",
  error: "Não foi possível carregar os atendimentos do período.",
  filtered: "Nenhum atendimento com esse status no período.",
  empty: "Nenhum atendimento lançado no período.",
  emptyHint: "Os atendimentos aparecem aqui pela data do lançamento.",
};

function buildColumns(showProvider) {
  return [
    { key: "launchedAt", header: "Lançado em", cell: (row) => formatDateTime(row.launchedAt) },
    { key: "occurredAt", header: "Atendido em", cell: (row) => formatDate(row.occurredAt) },
    {
      key: "patient",
      header: "Paciente",
      cell: (row) => (
        <span className={styles.patient}>
          {row.patientName}
          {!row.active && <Badge variant="inactive">Desfeito</Badge>}
        </span>
      ),
    },
    showProvider && { key: "provider", header: "Prestador", cell: (row) => row.providerName },
    {
      key: "technique",
      header: "Técnica",
      cell: (row) =>
        row.sessionDurationPriceId != null ? `${row.techniqueName} · ${row.durationMinutes} min` : row.techniqueName,
    },
    { key: "status", header: "Status", cell: (row) => getConsultationStatusLabel(row.status) },
    {
      key: "payout",
      header: "Repasse",
      align: "right",
      cell: (row) => (
        <span className={row.active ? styles.payout : styles.undone}>{formatCurrency(row.payoutValue)}</span>
      ),
    },
  ].filter(Boolean);
}

// consultations: já filtradas e ordenadas pela page; hasRecords: o período tem lançamentos
// (o filtro de status escondeu todos). showProvider: o ADMIN vendo todos os prestadores
export function LaunchedConsultationsTable({
  consultations = [],
  hasRecords = false,
  isLoading = false,
  isError = false,
  status,
  onStatusChange,
  showProvider = false,
  className,
}) {
  const isEmpty = isLoading || isError || consultations.length === 0;

  return (
    <section className={[styles.section, className].filter(Boolean).join(" ")} aria-label="Atendimentos do período">
      <div className={styles.header}>
        <h2 className={styles.title}>Atendimentos do período</h2>
        <label className={styles.statusFilter}>
          <span className={styles.statusLabel}>Status</span>
          <span className={styles.statusSelect}>
            <Select value={status} onChange={(event) => onStatusChange(event.target.value)}>
              {STATUS_FILTER_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </span>
        </label>
      </div>
      {isEmpty ? (
        <ListMessage
          isLoading={isLoading}
          isError={isError}
          hasRecords={hasRecords}
          messages={MESSAGES}
          className={styles.body}
        />
      ) : (
        <DataTable columns={buildColumns(showProvider)} rows={consultations} className={styles.body} />
      )}
    </section>
  );
}
