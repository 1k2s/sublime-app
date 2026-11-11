import { Pencil } from "lucide-react";
import { Card } from "@/shared/components/Card";
import { Badge } from "@/shared/components/Badge";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatPriceAxis } from "@/domain/pricing/priceAxis";
import { formatDate } from "@/shared/utils/date";
import styles from "./ContractTable.module.css";

function getInitials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

function formatValidity(contract) {
  const end = contract.validTo ? formatDate(contract.validTo) : "Sem término";
  return `${formatDate(contract.validFrom)} – ${end}`;
}

const MESSAGES = {
  loading: "Carregando contratos...",
  error: "Não foi possível carregar os contratos",
  filtered: "Nenhum contrato encontrado",
  empty: "Nenhum contrato cadastrado",
  emptyHint: "Clique em Novo contrato para começar.",
};

export function ContractTable({
  contracts = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  onEdit,
  className,
}) {
  const cardClass = (name) => [styles[name], className].filter(Boolean).join(" ");

  if (isLoading || isError || contracts.length === 0) {
    return (
      <ListMessage
        isLoading={isLoading}
        isError={isError}
        hasRecords={hasRecords}
        messages={MESSAGES}
        className={className}
      />
    );
  }

  return (
    <Card className={cardClass("table-card")}>
      <table className={styles.table}>
        <thead>
          <tr className={styles["table-head-row"]}>
            <th className={`${styles["table-th"]} ${styles["table-first"]}`}>PACIENTE</th>
            <th className={styles["table-th"]}>PLANO / TÉCNICA</th>
            <th className={styles["table-th"]}>FREQ. SEMANAL</th>
            <th className={styles["table-th"]}>VIGÊNCIA</th>
            <th className={styles["table-th"]}>STATUS</th>
            <th className={`${styles["table-th"]} ${styles["table-last"]}`}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {contracts.map((contract) => {
            const isActive = contract.status === "ACTIVE";

            return (
              <tr key={contract.id} className={styles["table-row"]}>
                <td className={`${styles["table-td"]} ${styles["table-first"]}`}>
                  <div className={styles["table-patient"]}>
                    <span className={styles["table-avatar"]}>{getInitials(contract.patientName)}</span>
                    <span className={styles["table-patient-name"]}>{contract.patientName}</span>
                  </div>
                </td>
                <td className={styles["table-td"]}>
                  {contract.planName} · {contract.techniqueName}
                </td>
                <td className={styles["table-td"]}>{formatPriceAxis(contract) || "--"}</td>
                <td className={styles["table-td"]}>{formatValidity(contract)}</td>
                <td className={styles["table-td"]}>
                  <Badge variant={isActive ? "active" : "inactive"}>{isActive ? "Ativo" : "Encerrado"}</Badge>
                </td>
                <td className={`${styles["table-td"]} ${styles["table-last"]}`}>
                  <button
                    type="button"
                    className={styles["table-edit"]}
                    disabled={!onEdit}
                    aria-label={`Editar contrato de ${contract.patientName}`}
                    title="Editar"
                    onClick={() => onEdit?.(contract)}
                  >
                    <Pencil size={16} />
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </Card>
  );
}
