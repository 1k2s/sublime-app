import { Eye } from "lucide-react";
import { Card } from "@/shared/components/Card";
import { Badge } from "@/shared/components/Badge";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { getContractStatus } from "../contractStatus";
import styles from "./ContractTable.module.css";

function getInitials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

// As técnicas dos itens (um item por grupo de preço)
function formatItems(contract) {
  return contract.items.map((item) => item.techniqueName).join(" + ");
}

const MESSAGES = {
  loading: "Carregando contratos...",
  error: "Não foi possível carregar os contratos",
  filtered: "Nenhum contrato encontrado",
  empty: "Nenhum contrato cadastrado",
  emptyHint: "Clique em Novo contrato para começar.",
};

// contracts: já com patientName e beneficiaryName (withPatientNames)
export function ContractTable({
  contracts = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  onViewDetails,
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
            <th className={`${styles["table-th"]} ${styles["table-first"]}`}>TITULAR</th>
            <th className={styles["table-th"]}>PLANO</th>
            <th className={styles["table-th"]}>ITENS</th>
            <th className={styles["table-th"]}>VIGÊNCIA</th>
            <th className={styles["table-th"]}>VALOR</th>
            <th className={styles["table-th"]}>STATUS</th>
            <th className={`${styles["table-th"]} ${styles["table-last"]}`}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {contracts.map((contract) => {
            const status = getContractStatus(contract);

            return (
              <tr key={contract.id} className={styles["table-row"]}>
                <td className={`${styles["table-td"]} ${styles["table-first"]}`}>
                  <div className={styles["table-patient"]}>
                    <span className={styles["table-avatar"]}>{getInitials(contract.patientName)}</span>
                    <div className={styles["table-patient-text"]}>
                      <span className={styles["table-patient-name"]}>{contract.patientName ?? "--"}</span>
                      {contract.beneficiaryName && (
                        <span className={styles["table-beneficiary"]}>Benef.: {contract.beneficiaryName}</span>
                      )}
                    </div>
                  </div>
                </td>
                <td className={styles["table-td"]}>{contract.planName}</td>
                <td className={styles["table-td"]}>{formatItems(contract)}</td>
                <td className={styles["table-td"]}>
                  {formatDate(contract.startDate)} – {formatDate(contract.endDate)}
                </td>
                <td className={styles["table-td"]}>
                  <div className={styles["table-value"]}>
                    <span className={styles["table-value-total"]}>{formatCurrency(contract.totalValue)}</span>
                    <span className={styles["table-value-installments"]}>em {contract.installments}x</span>
                  </div>
                </td>
                <td className={styles["table-td"]}>
                  <Badge variant={status.badge}>{status.label}</Badge>
                </td>
                <td className={`${styles["table-td"]} ${styles["table-last"]}`}>
                  <button
                    type="button"
                    className={styles["table-edit"]}
                    aria-label={`Ver contrato de ${contract.patientName ?? "paciente"}`}
                    title="Ver detalhes"
                    onClick={() => onViewDetails?.(contract)}
                  >
                    <Eye size={16} />
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
