import { useState } from "react";
import { FileText, Plus } from "lucide-react";
import { PageHeader } from "@/shared/components/PageHeader";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import { showError } from "@/shared/utils/alert";
import { usePatients } from "@/domain/patient/api/usePatients";
import { usePlans, usePricingGroups, useTechniques } from "@/domain/pricing/api/usePricing";
import { useContracts, useCreateContract, useDeactivateContract } from "../api/useContracts";
import { ContractDetailsModal } from "../components/ContractDetailsModal";
import { ContractFilters } from "../components/ContractFilters";
import { ContractForm } from "../components/ContractForm";
import { ContractTable } from "../components/ContractTable";
import { withPatientNames } from "../utils/contractPatients";
import { filterContracts } from "../utils/filterContracts";
import styles from "./ContractPage.module.css";

export function ContractPage() {
  const [isCreating, setIsCreating] = useState(false);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("CURRENT");
  const [selectedContractId, setSelectedContractId] = useState(null);

  const contractsQuery = useContracts();
  const patientsQuery = usePatients();
  const createContract = useCreateContract();
  const deactivateContract = useDeactivateContract();
  // Catálogo do formulário (planos, técnicas e grupos de preço), do cache do módulo pricing
  const plansQuery = usePlans();
  const techniquesQuery = useTechniques();
  const pricingGroupsQuery = usePricingGroups();

  // Nome do titular e do beneficiário vêm da lista de pacientes (o mesmo cache da tela de Pacientes)
  const contracts = withPatientNames(contractsQuery.data ?? [], patientsQuery.data ?? []);
  const visibleContracts = filterContracts(contracts, { search, status });
  // Lido da lista inteira: depois de inativar, o modal mostra o estado novo
  const selectedContract = contracts.find((contract) => contract.id === selectedContractId) ?? null;
  const isLoading = contractsQuery.isLoading || patientsQuery.isLoading;
  const isError = contractsQuery.isError || patientsQuery.isError;

  function handleCreate(payload, { applyApiErrors }) {
    createContract.mutate(payload, {
      onSuccess: () => setIsCreating(false),
      onError: (error) => {
        applyApiErrors?.(error);
        showError(error.message);
      },
    });
  }

  function handleDeactivate(contract) {
    deactivateContract.mutate(contract.id, { onError: (error) => showError(error.message) });
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Contratos"
        description="Contratos dos pacientes: vigentes, vencidos e inativos."
        action={
          <Button onClick={() => setIsCreating(true)}>
            <Plus size={18} /> Novo contrato
          </Button>
        }
      />

      <ContractFilters search={search} onSearchChange={setSearch} status={status} onStatusChange={setStatus} />

      <ContractTable
        contracts={visibleContracts}
        isLoading={isLoading}
        isError={isError}
        hasRecords={contracts.length > 0}
        onViewDetails={(contract) => setSelectedContractId(contract.id)}
      />

      <ContractDetailsModal
        contract={selectedContract}
        isDeactivating={deactivateContract.isPending}
        onDeactivate={handleDeactivate}
        onClose={() => setSelectedContractId(null)}
      />

      {/* Cadastro num modal grande sobre a lista (fundo desfocado), com o resultado e os botões
          num rodapé fixo. O clique no fundo não fecha: um clique sem querer perderia o formulário */}
      {isCreating && (
        <Modal
          size="large"
          closeOnBackdrop={false}
          icon={FileText}
          title="Novo contrato"
          description="Escolha o paciente, o plano e os itens. O fim e o valor são calculados."
          onClose={() => setIsCreating(false)}
        >
          <ContractForm
            plans={plansQuery.data}
            techniques={techniquesQuery.data}
            pricingGroups={pricingGroupsQuery.data}
            isCatalogLoading={plansQuery.isLoading || techniquesQuery.isLoading || pricingGroupsQuery.isLoading}
            isCatalogError={plansQuery.isError || techniquesQuery.isError || pricingGroupsQuery.isError}
            onSubmit={handleCreate}
            onCancel={() => setIsCreating(false)}
            isSubmitting={createContract.isPending}
          />
        </Modal>
      )}
    </div>
  );
}
