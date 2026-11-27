import { useState } from "react";
import { ChevronDown, ChevronRight, Wallet } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { FilterPills } from "@/shared/components/FilterPills";
import { Modal } from "@/shared/components/Modal";
import { Tabs } from "@/shared/components/Tabs";
import { formatContractLabelWithStatus } from "../display/accountLabels";
import { getAccountStatus } from "../display/accountStatus";
import { isClosedContract } from "../utils/closedContracts";
import { AccountInstallmentsTab } from "./AccountInstallmentsTab";
import { AccountPaymentsTab } from "./AccountPaymentsTab";
import { AccountSessionsTab } from "./AccountSessionsTab";
import { AccountSummaryTab } from "./AccountSummaryTab";
import styles from "./PatientAccountModal.module.css";

const TABS = { SUMMARY: "summary", INSTALLMENTS: "installments", SESSIONS: "sessions", PAYMENTS: "payments" };

const TAB_OPTIONS = [
  { value: TABS.SUMMARY, label: "Resumo" },
  { value: TABS.INSTALLMENTS, label: "Parcelas" },
  { value: TABS.SESSIONS, label: "Sessões" },
  { value: TABS.PAYMENTS, label: "Pagamentos" },
];

// A conta do paciente, em quatro abas: Resumo (um cartão por contrato e o avulso), Parcelas (as de
// cada versão do contrato), Sessões (os atendimentos numerados) e Pagamentos (com Inativar). Tudo
// vem calculado da API (ver "Payment" no domain-model). Parcelas e Sessões são de um contrato: com
// mais de um, pílulas no topo escolhem qual (contractId, controlado pela página, que busca os dados).
// patientName e status: da linha da lista. account, installments, sessions, payments, transfers:
// as consultas ({ data, isLoading, isError })
export function PatientAccountModal({
  patientName,
  status,
  account,
  installments,
  sessions,
  payments,
  transfers,
  contractId,
  onContractChange,
  isDeactivatingPayment = false,
  onDeactivatePayment,
  onRegisterPayment,
  onClose,
}) {
  const [tab, setTab] = useState(TABS.SUMMARY);
  const [isClosedOpen, setIsClosedOpen] = useState(false);
  const contracts = account.data?.contracts ?? [];
  const accountStatus = status ? getAccountStatus(status) : null;

  function viewInstallments(selectedContractId) {
    onContractChange(selectedContractId);
    setTab(TABS.INSTALLMENTS);
  }

  // Parcelas e Sessões: as pílulas dos contratos, só com mais de um. Os encerrados ficam atrás da
  // pílula "Encerrados (N)", que abre as deles; já vêm abertos quando o escolhido é um deles ou
  // quando não há outro
  function renderContractPills() {
    if (contracts.length < 2) return null;
    const toOption = (contract) => ({
      value: contract.contractId,
      label: formatContractLabelWithStatus(contract, contract.contractStatus),
    });
    const openContracts = contracts.filter((contract) => !isClosedContract(contract));
    const closedContracts = contracts.filter(isClosedContract);
    const isChosenClosed = closedContracts.some((contract) => contract.contractId === contractId);
    const showClosed = isClosedOpen || isChosenClosed || openContracts.length === 0;
    return (
      <div className={styles.contracts}>
        {openContracts.length > 0 && (
          <FilterPills options={openContracts.map(toOption)} value={contractId} onChange={onContractChange} />
        )}
        {closedContracts.length > 0 && (
          <button
            type="button"
            className={styles.closedToggle}
            aria-expanded={showClosed}
            onClick={() => setIsClosedOpen(!showClosed)}
          >
            {showClosed ? <ChevronDown size={14} aria-hidden /> : <ChevronRight size={14} aria-hidden />}
            Encerrados ({closedContracts.length})
          </button>
        )}
        {closedContracts.length > 0 && showClosed && (
          <FilterPills options={closedContracts.map(toOption)} value={contractId} onChange={onContractChange} />
        )}
      </div>
    );
  }

  function renderTab() {
    if (tab === TABS.SUMMARY) {
      return (
        <AccountSummaryTab
          account={account.data}
          isLoading={account.isLoading}
          isError={account.isError}
          onViewInstallments={viewInstallments}
        />
      );
    }
    if (tab === TABS.PAYMENTS) {
      return (
        <AccountPaymentsTab
          payments={payments.data}
          transfers={transfers.data}
          isLoading={payments.isLoading || transfers.isLoading}
          isError={payments.isError || transfers.isError}
          isDeactivating={isDeactivatingPayment}
          onDeactivate={onDeactivatePayment}
        />
      );
    }
    // Parcelas e Sessões precisam de um contrato
    if (!account.isLoading && contracts.length === 0) {
      return <p className={styles.empty}>Sem contrato: o paciente só tem atendimentos avulsos.</p>;
    }
    return (
      <>
        {renderContractPills()}
        {tab === TABS.INSTALLMENTS ? (
          <AccountInstallmentsTab
            versions={installments.data}
            isLoading={account.isLoading || installments.isLoading}
            isError={installments.isError}
          />
        ) : (
          <AccountSessionsTab
            data={sessions.data}
            isLoading={account.isLoading || sessions.isLoading}
            isError={sessions.isError}
          />
        )}
      </>
    );
  }

  const footer = (
    <div className={styles.footer}>
      <Button variant="outline" onClick={onClose}>
        Fechar
      </Button>
      <Button onClick={onRegisterPayment} disabled={!account.data}>
        Registrar pagamento
      </Button>
    </div>
  );

  return (
    <Modal
      size="medium"
      icon={Wallet}
      title={patientName ?? "Conta do paciente"}
      titleExtra={accountStatus && <Badge variant={accountStatus.badge}>{accountStatus.label}</Badge>}
      description="Conta do paciente"
      footer={footer}
      onClose={onClose}
    >
      <div className={styles.content}>
        <Tabs label="Conta do paciente" tabs={TAB_OPTIONS} value={tab} onChange={setTab} />
        {/* A mesma altura nas quatro abas (o modal não muda de tamanho ao trocar): as listas rolam
            por dentro */}
        <div role="tabpanel" className={styles.panel}>
          {renderTab()}
        </div>
      </div>
    </Modal>
  );
}
