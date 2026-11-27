import { useState } from "react";
import { ChevronDown, ChevronRight } from "lucide-react";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatCurrency } from "@/shared/utils/money";
import { formatBalance, getBalanceNote, getBalanceTone } from "../display/accountStatus";
import { isClosedContract } from "../utils/closedContracts";
import { AccountCells } from "./AccountCells";
import { ContractAccountCard } from "./ContractAccountCard";
import styles from "./AccountSummaryTab.module.css";

const MESSAGES = {
  loading: "Carregando a conta...",
  error: "Não foi possível carregar a conta",
  filtered: "Sem conta",
  empty: "Sem contrato nem atendimento avulso",
  emptyHint: "A conta aparece com o primeiro contrato ou atendimento avulso.",
};

// O Resumo da conta: um cartão por contrato (o vigente primeiro) e um do avulso; os contratos
// encerrados (terminados e zerados) numa seção recolhida no fim, para consulta. Rola por dentro do
// painel. onViewInstallments(contractId): o "Ver parcelas" de um contrato
export function AccountSummaryTab({ account, isLoading = false, isError = false, onViewInstallments }) {
  const [isClosedOpen, setIsClosedOpen] = useState(false);
  const hasAccount = Boolean(account && (account.contracts.length > 0 || account.singleSession));
  if (isLoading || isError || !hasAccount) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={false} messages={MESSAGES} />;
  }

  const openContracts = account.contracts.filter((contract) => !isClosedContract(contract));
  const closedContracts = account.contracts.filter(isClosedContract);
  const single = account.singleSession;
  // Só com encerrados, a seção já vem aberta
  const showClosed = isClosedOpen || (openContracts.length === 0 && !single);

  function renderCard(contract) {
    return (
      <ContractAccountCard
        key={contract.contractId}
        account={contract}
        onViewInstallments={() => onViewInstallments?.(contract.contractId)}
      />
    );
  }

  return (
    <div className={styles.summary}>
      {openContracts.map(renderCard)}

      {single && (
        <AccountCells
          title="Avulso"
          subtitle="atendimentos e pagamentos sem contrato"
          cells={[
            { label: "Consumido", value: formatCurrency(single.due), note: "sem contrato" },
            { label: "Pago", value: formatCurrency(single.paid), note: "sem contrato" },
            {
              label: "Saldo",
              value: formatBalance(single.balance),
              note: getBalanceNote(single.balance),
              tone: getBalanceTone(single.balance),
            },
          ]}
        />
      )}

      {closedContracts.length > 0 && (
        <section className={styles.closed}>
          <button
            type="button"
            className={styles.closedToggle}
            aria-expanded={showClosed}
            onClick={() => setIsClosedOpen(!showClosed)}
          >
            {showClosed ? <ChevronDown size={16} aria-hidden /> : <ChevronRight size={16} aria-hidden />}
            Contratos encerrados ({closedContracts.length})
          </button>
          {showClosed && <div className={styles.closedCards}>{closedContracts.map(renderCard)}</div>}
        </section>
      )}
    </div>
  );
}
