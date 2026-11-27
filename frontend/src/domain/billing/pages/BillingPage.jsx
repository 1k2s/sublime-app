import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { PageHeader } from "@/shared/components/PageHeader";
import { showError, showSuccess } from "@/shared/utils/alert";
import {
  useAccounts,
  useContractInstallments,
  useContractSessions,
  usePatientAccount,
  usePatientTransfers,
} from "../api/useBilling";
import { useDeactivatePayment, usePayments, useRegisterPayment } from "../api/usePayments";
import { BillingFilters } from "../components/BillingFilters";
import { BillingTable } from "../components/BillingTable";
import { PatientAccountModal } from "../components/PatientAccountModal";
import { RegisterPaymentModal } from "../components/RegisterPaymentModal";
import { defaultContractId } from "../utils/closedContracts";
import { countAccounts, filterAccounts } from "../utils/filterAccounts";
import styles from "./BillingPage.module.css";

// O Financeiro: a lista das contas (uma linha por paciente, na ordem da API) e a conta do
// paciente num modal. O paciente aberto fica na URL (?patientId=): o "Ver conta" do detalhe do
// contrato abre o mesmo modal. Tudo é calculado pela API (ver "Payment" no domain-model)
export function BillingPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  // O contrato das abas Parcelas e Sessões (null = o primeiro não encerrado, o vigente)
  const [chosenContractId, setChosenContractId] = useState(null);
  const [isRegistering, setIsRegistering] = useState(false);

  const selectedPatientId = Number(searchParams.get("patientId")) || null;
  const accountsQuery = useAccounts();
  const accountQuery = usePatientAccount(selectedPatientId);
  const paymentsQuery = usePayments(selectedPatientId);
  const transfersQuery = usePatientTransfers(selectedPatientId);
  const contractId = defaultContractId(accountQuery.data?.contracts ?? [], chosenContractId);
  const installmentsQuery = useContractInstallments(contractId);
  const sessionsQuery = useContractSessions(contractId);
  const registerPayment = useRegisterPayment();
  const deactivatePayment = useDeactivatePayment();

  const accounts = accountsQuery.data ?? [];
  const visibleAccounts = filterAccounts(accounts, { search, filter });
  // A linha da lista dá o nome e a situação do modal (a conta traz os contratos, não a situação geral)
  const selectedRow = accounts.find((account) => account.patientId === selectedPatientId) ?? null;

  function openAccount(account) {
    setChosenContractId(null);
    setSearchParams({ patientId: String(account.patientId) });
  }

  function closeAccount() {
    setIsRegistering(false);
    setSearchParams({});
  }

  function handleRegister(request, { applyApiErrors }) {
    registerPayment.mutate(request, {
      onSuccess: () => {
        setIsRegistering(false);
        showSuccess("Pagamento registrado.");
      },
      onError: (error) => {
        applyApiErrors?.(error);
        showError(error.message);
      },
    });
  }

  function handleDeactivatePayment(payment) {
    deactivatePayment.mutate(payment.id, { onError: (error) => showError(error.message) });
  }

  return (
    <div className={styles.page}>
      <PageHeader title="Financeiro" description="O que cada paciente deve e o que pagou." />

      <BillingFilters
        search={search}
        onSearchChange={setSearch}
        filter={filter}
        onFilterChange={setFilter}
        counts={countAccounts(accounts)}
      />

      <BillingTable
        accounts={visibleAccounts}
        isLoading={accountsQuery.isLoading}
        isError={accountsQuery.isError}
        hasRecords={accounts.length > 0}
        selectedPatientId={selectedPatientId}
        onViewAccount={openAccount}
      />

      {selectedPatientId && (
        <PatientAccountModal
          patientName={accountQuery.data?.patientName ?? selectedRow?.patientName}
          status={selectedRow?.status}
          account={accountQuery}
          installments={installmentsQuery}
          sessions={sessionsQuery}
          payments={paymentsQuery}
          transfers={transfersQuery}
          contractId={contractId}
          onContractChange={setChosenContractId}
          isDeactivatingPayment={deactivatePayment.isPending}
          onDeactivatePayment={handleDeactivatePayment}
          onRegisterPayment={() => setIsRegistering(true)}
          // Com o registrar pagamento aberto, o Esc fecha só ele (os dois modais ouvem a tecla)
          onClose={isRegistering ? () => {} : closeAccount}
        />
      )}

      {selectedPatientId && isRegistering && accountQuery.data && (
        <RegisterPaymentModal
          patientId={selectedPatientId}
          patientName={accountQuery.data.patientName}
          account={accountQuery.data}
          isSubmitting={registerPayment.isPending}
          onSubmit={handleRegister}
          onCancel={() => setIsRegistering(false)}
        />
      )}
    </div>
  );
}
