import { Eye } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatContractLabels } from "../display/accountLabels";
import { formatBalance, getAccountStatus, getBalanceTone } from "../display/accountStatus";
import { formatWarnings } from "../display/accountWarnings";
import styles from "./BillingTable.module.css";

const MESSAGES = {
  loading: "Carregando as contas...",
  error: "Não foi possível carregar as contas",
  filtered: "Nenhum paciente encontrado",
  empty: "Nenhuma conta ainda",
  emptyHint: "As contas aparecem com o primeiro contrato ou atendimento avulso.",
};

// O próximo vencimento: a parcela vencida mais antiga (em vermelho, com quantas são e o total em
// atraso) ou, sem vencida, a próxima e o valor. Tudo vem pronto da API (ver "Lista do Financeiro"
// em Payment, no domain-model)
function NextDue({ account }) {
  if (account.hasOverdue) {
    const count = account.overdueCount > 1 ? `${account.overdueCount} vencidas` : "vencida";
    return (
      <div className={styles.stack}>
        <span className={styles.late}>
          {formatDate(account.oldestOverdueDate, "dd/MM")} · {count}
        </span>
        <span className={styles.detail}>{formatCurrency(account.overdueAmount)} em atraso</span>
      </div>
    );
  }
  if (account.nextDue) {
    return (
      <div className={styles.stack}>
        <span className={styles.name}>{formatDate(account.nextDue.dueDate, "dd/MM")}</span>
        <span className={styles.detail}>{formatCurrency(account.nextDue.amount)}</span>
      </div>
    );
  }
  return (
    <div className={styles.stack}>
      <span>—</span>
      <span className={styles.detail}>{account.contracts.length > 0 ? "parcelas quitadas" : "sem parcelas"}</span>
    </div>
  );
}

// accounts: as linhas da API, já na ordem (a da "Lista do Financeiro"). selectedPatientId: a conta aberta, marcada
export function BillingTable({
  accounts = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  selectedPatientId = null,
  onViewAccount,
  className,
}) {
  if (isLoading || isError || accounts.length === 0) {
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
      header: "Paciente",
      cell: (account) => (
        <div className={styles.stack}>
          <span className={styles.name}>{account.patientName}</span>
          <span className={styles.detail}>
            {formatContractLabels(account.contracts)}
            {!account.patientActive && " · paciente inativo"}
          </span>
        </div>
      ),
    },
    {
      key: "balance",
      header: "Saldo",
      align: "right",
      cell: (account) => (
        <div className={[styles.stack, styles.end].join(" ")}>
          <span className={styles[getBalanceTone(account.balance)]}>{formatBalance(account.balance)}</span>
          <span className={styles.detail}>
            pago {formatCurrency(account.paid)} de {formatCurrency(account.due)}
          </span>
        </div>
      ),
    },
    { key: "nextDue", header: "Próximo vencimento", cell: (account) => <NextDue account={account} /> },
    {
      key: "status",
      header: "Situação",
      cell: (account) => {
        const status = getAccountStatus(account.status);
        return <Badge variant={status.badge}>{status.label}</Badge>;
      },
    },
    {
      key: "warnings",
      header: "Avisos",
      cell: (account) => <span className={styles.warnings}>{formatWarnings(account.warnings)}</span>,
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (account) => (
        <IconButton
          label={`Ver a conta de ${account.patientName}`}
          title="Ver conta"
          onClick={() => onViewAccount?.(account)}
        >
          <Eye size={16} />
        </IconButton>
      ),
    },
  ];

  // A linha inteira abre a conta (o botão do olho continua, para o teclado)
  return (
    <DataTable
      columns={columns}
      rows={accounts}
      getRowKey={(account) => account.patientId}
      onRowClick={(account) => onViewAccount?.(account)}
      selectedRowKey={selectedPatientId}
      className={className}
    />
  );
}
