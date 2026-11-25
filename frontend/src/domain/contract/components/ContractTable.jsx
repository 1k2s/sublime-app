import { Eye } from "lucide-react";
import { AvatarName } from "@/shared/components/AvatarName";
import { ContractBadge } from "./ContractBadge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { getContractStatus } from "../display/contractStatus";
import { getVersionLabel, hasAmendment } from "../display/contractVersions";
import styles from "./ContractTable.module.css";

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

// contracts: já com patientName e beneficiaryName (withPatientNames). selectedContractId: o
// contrato aberto (detalhe ou aditivo), marcado na lista
export function ContractTable({
  contracts = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  selectedContractId = null,
  onViewDetails,
  className,
}) {
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

  const columns = [
    {
      key: "patient",
      header: "Titular",
      cell: (contract) => (
        <AvatarName
          name={contract.patientName}
          detail={contract.beneficiaryName && `Benef.: ${contract.beneficiaryName}`}
        />
      ),
    },
    { key: "plan", header: "Plano", cell: (contract) => contract.planName },
    { key: "items", header: "Itens", cell: formatItems },
    {
      key: "term",
      header: "Vigência",
      cell: (contract) => `${formatDate(contract.startDate)} – ${formatDate(contract.endDate)}`,
    },
    {
      key: "value",
      header: "Valor",
      align: "right",
      cell: (contract) => (
        <div className={styles.value}>
          <span className={styles.total}>{formatCurrency(contract.totalValue)}</span>
          <span className={styles.installments}>em {contract.installments}x</span>
        </div>
      ),
    },
    {
      key: "status",
      header: "Status",
      // O estado e, se houve aditivo, o selo "Aditivo N" (a versão original fica só com o estado)
      cell: (contract) => {
        const status = getContractStatus(contract);
        return (
          <div className={styles.badges}>
            <ContractBadge variant={status.badge}>{status.label}</ContractBadge>
            {hasAmendment(contract) && <ContractBadge>{getVersionLabel(contract)}</ContractBadge>}
          </div>
        );
      },
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (contract) => (
        <IconButton
          variant="soft"
          label={`Ver contrato de ${contract.patientName ?? "paciente"}`}
          title="Ver detalhes"
          onClick={() => onViewDetails?.(contract)}
        >
          <Eye size={16} />
        </IconButton>
      ),
    },
  ];

  // Variante clara, nos tons dos modais do contrato. A linha inteira abre o detalhe (o botão do olho
  // continua, para o teclado; o clique nele também chega à linha, e abre o mesmo detalhe)
  return (
    <DataTable
      columns={columns}
      rows={contracts}
      variant="light"
      onRowClick={(contract) => onViewDetails?.(contract)}
      selectedRowKey={selectedContractId}
      className={className}
    />
  );
}
