import { useState } from "react";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { FilterPills } from "@/shared/components/FilterPills";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { getConsultationStatusLabel } from "@/domain/consultation/display/consultationStatuses";
import { formatSessionNumber, getSessionTag } from "../display/sessionTags";
import styles from "./AccountSessionsTab.module.css";

const ALL = "ALL";

const MESSAGES = {
  loading: "Carregando as sessões...",
  error: "Não foi possível carregar as sessões",
  filtered: "Nenhuma sessão desta técnica",
  empty: "Nenhuma sessão lançada",
  emptyHint: "Os atendimentos do contrato aparecem aqui.",
};

// A sessão: "5ª de 6" e, com etiqueta, o selo ("8ª de 6 · além do contratado")
function SessionNumber({ session }) {
  const tag = getSessionTag(session.tag);
  const number = formatSessionNumber(session);
  if (!tag) return number;
  return <Badge variant={tag.badge}>{session.number == null ? tag.label : `${number} · ${tag.label}`}</Badge>;
}

// A aba Sessões: os atendimentos ativos do contrato, do mais recente para o mais antigo, com a
// numeração da API (ver "Numeração das sessões" em Payment, no domain-model) e o filtro por técnica. Com beneficiário, a coluna "Paciente" diz quem foi
// atendido. data: { hasBeneficiary, sessions } do sessions da API
export function AccountSessionsTab({ data, isLoading = false, isError = false }) {
  const [technique, setTechnique] = useState(ALL);
  const sessions = data?.sessions ?? [];

  if (isLoading || isError || sessions.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={false} messages={MESSAGES} />;
  }

  const techniques = [...new Set(sessions.map((session) => session.techniqueName))];
  const visible = sessions.filter((session) => technique === ALL || session.techniqueName === technique).reverse();

  const columns = [
    { key: "date", header: "Data", cell: (session) => <span className={styles.name}>{formatDate(session.occurredAt)}</span> },
    {
      key: "technique",
      header: "Técnica",
      cell: (session) => (
        <span className={styles.name}>
          {session.techniqueName} · {session.durationMinutes} min
        </span>
      ),
    },
    data.hasBeneficiary && { key: "patient", header: "Paciente", cell: (session) => session.patientName },
    { key: "number", header: "Sessão", cell: (session) => <SessionNumber session={session} /> },
    {
      key: "status",
      header: "Status",
      cell: (session) => (
        <Badge variant={session.status === "ATTENDED" ? "active" : "neutral"}>
          {getConsultationStatusLabel(session.status)}
        </Badge>
      ),
    },
    { key: "provider", header: "Prestador", cell: (session) => session.providerName },
    { key: "value", header: "Valor", align: "right", cell: (session) => formatCurrency(session.baseValue) },
  ].filter(Boolean);

  return (
    <div className={styles.sessions}>
      {techniques.length > 1 && (
        <FilterPills
          options={[{ value: ALL, label: "Todas" }, ...techniques.map((name) => ({ value: name, label: name }))]}
          value={technique}
          onChange={setTechnique}
        />
      )}
      <DataTable
        columns={columns}
        rows={visible}
        getRowKey={(session) => session.consultationId}
        getRowClassName={(session) => (getSessionTag(session.tag)?.highlight ? styles.extra : undefined)}
        compact
        className={styles.table}
      />
    </div>
  );
}
