import { useState } from "react";
import { FileText, Pencil, Plus } from "lucide-react";
import { PageHeader } from "@/shared/components/PageHeader";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import { PageLoader } from "@/shared/components/PageLoader";
import { showError, showSuccess } from "@/shared/utils/alert";
import { usePatients } from "@/domain/patient/api/usePatients";
import { usePlans, usePricingGroups, useTechniques } from "@/domain/pricing/api/usePricing";
import {
  useAmendContract,
  useContractSessionBalance,
  useContractVersions,
  useContracts,
  useCreateContract,
  useDeactivateContract,
} from "../api/useContracts";
import { ContractAmendmentReview } from "../components/ContractAmendmentReview";
import { ContractDetailsModal } from "../components/ContractDetailsModal";
import { ContractFilters } from "../components/ContractFilters";
import { ContractForm } from "../components/ContractForm";
import { ContractTable } from "../components/ContractTable";
import { getContractStatus } from "../display/contractStatus";
import { getVersionLabel, hasAmendment } from "../display/contractVersions";
import { withPatientNames } from "../utils/contractPatients";
import { filterContracts } from "../utils/filterContracts";
import styles from "./ContractPage.module.css";

export function ContractPage() {
  const [isCreating, setIsCreating] = useState(false);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("CURRENT");
  const [selectedContractId, setSelectedContractId] = useState(null);
  // Aditivo: a versão sendo alterada (passo 1) e, na revisão (passo 2), o pedido montado e as
  // "Alterações". O formulário continua montado na revisão: o "Voltar" o encontra preenchido
  const [amendingContractId, setAmendingContractId] = useState(null);
  const [review, setReview] = useState(null);

  const contractsQuery = useContracts();
  const patientsQuery = usePatients();
  const createContract = useCreateContract();
  const deactivateContract = useDeactivateContract();
  const amendContract = useAmendContract();
  // Catálogo do formulário (planos, técnicas e grupos de preço), do cache do módulo pricing
  const plansQuery = usePlans();
  const techniquesQuery = useTechniques();
  const pricingGroupsQuery = usePricingGroups();
  const isCatalogLoading = plansQuery.isLoading || techniquesQuery.isLoading || pricingGroupsQuery.isLoading;
  const isCatalogError = plansQuery.isError || techniquesQuery.isError || pricingGroupsQuery.isError;
  // Histórico do contrato aberto (detalhe ou aditivo) e o saldo de sessões, lido ao abrir o aditivo
  const versionsQuery = useContractVersions(selectedContractId ?? amendingContractId);
  const sessionBalanceQuery = useContractSessionBalance(amendingContractId);

  // Nome do titular e do beneficiário vêm da lista de pacientes (o mesmo cache da tela de Pacientes)
  const patients = patientsQuery.data ?? [];
  const contracts = withPatientNames(contractsQuery.data ?? [], patients);
  const versions = withPatientNames(versionsQuery.data ?? [], patients);
  const visibleContracts = filterContracts(contracts, { search, status });
  // Lidos da lista inteira: depois de inativar, o modal mostra o estado novo
  const selectedContract = contracts.find((contract) => contract.id === selectedContractId) ?? null;
  const amendingContract = contracts.find((contract) => contract.id === amendingContractId) ?? null;
  // O número do contrato é o da versão original (a última do histórico)
  const contractNumber = versions.at(-1)?.id ?? amendingContract?.id;
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

  // "Editar" no detalhe: o detalhe fecha e abre o passo 1
  function startAmendment(contract) {
    setSelectedContractId(null);
    setAmendingContractId(contract.id);
  }

  function closeAmendment() {
    setReview(null);
    setAmendingContractId(null);
  }

  // Cancelar (ou Esc) no passo 1: volta ao detalhe do contrato, de onde o "Editar" veio
  function cancelAmendment() {
    setSelectedContractId(amendingContractId);
    closeAmendment();
  }

  // Passo 1 → passo 2
  function handleReview(request, { applyApiErrors, changes }) {
    setReview({ request, changes, applyApiErrors });
  }

  // Confirmar: tudo fecha e a lista mostra a versão nova (invalidação). Erro da API: volta ao
  // formulário, com a mensagem e o erro no campo
  function confirmAmendment() {
    amendContract.mutate(
      { id: amendingContractId, payload: review.request },
      {
        onSuccess: () => {
          closeAmendment();
          showSuccess("Aditivo salvo.");
        },
        onError: (error) => {
          review.applyApiErrors?.(error);
          setReview(null);
          showError(error.message);
        },
      },
    );
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Contratos"
        description="Contratos dos pacientes: vigentes, vencidos, encerrados e cancelados."
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
        selectedContractId={selectedContractId ?? amendingContractId}
        onViewDetails={(contract) => setSelectedContractId(contract.id)}
      />

      <ContractDetailsModal
        contract={selectedContract}
        versions={versions}
        isVersionsLoading={versionsQuery.isLoading}
        isVersionsError={versionsQuery.isError}
        isDeactivating={deactivateContract.isPending}
        onDeactivate={handleDeactivate}
        onEdit={startAmendment}
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
            isCatalogLoading={isCatalogLoading}
            isCatalogError={isCatalogError}
            onSubmit={handleCreate}
            onCancel={() => setIsCreating(false)}
            isSubmitting={createContract.isPending}
          />
        </Modal>
      )}

      {/* Aditivo, passo 1: o mesmo modal grande do cadastro, preenchido com a versão atual. Na
          revisão, o Esc fecha só a revisão (os dois modais ouvem a tecla) */}
      {amendingContract && (
        <Modal
          size="large"
          closeOnBackdrop={false}
          icon={Pencil}
          title={`Aditivo · ${amendingContract.patientName ?? "Contrato"}`}
          titleExtra={
            <span className={styles.badges}>
              <Badge variant={getContractStatus(amendingContract).badge}>{getContractStatus(amendingContract).label}</Badge>
              {hasAmendment(amendingContract) && <Badge>{getVersionLabel(amendingContract)}</Badge>}
            </span>
          }
          description={`Contrato nº ${contractNumber} · hoje ${hasAmendment(amendingContract) ? `no ${getVersionLabel(amendingContract)}` : "na versão original"}`}
          onClose={review ? () => {} : cancelAmendment}
        >
          {/* O formulário abre preenchido: só com o catálogo carregado (o tipo do plano vem dele) */}
          {isCatalogLoading ? (
            <PageLoader variant="section" label="Carregando o contrato..." />
          ) : (
            <ContractForm
              contract={amendingContract}
              sessionBalance={sessionBalanceQuery.data ?? []}
              plans={plansQuery.data}
              techniques={techniquesQuery.data}
              pricingGroups={pricingGroupsQuery.data}
              isCatalogError={isCatalogError}
              onSubmit={handleReview}
              onCancel={cancelAmendment}
              isSubmitting={amendContract.isPending}
            />
          )}
        </Modal>
      )}

      {amendingContract && review && (
        <ContractAmendmentReview
          patientName={amendingContract.patientName}
          changes={review.changes}
          isSubmitting={amendContract.isPending}
          onBack={() => setReview(null)}
          onConfirm={confirmAmendment}
        />
      )}
    </div>
  );
}
