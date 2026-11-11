import { useState } from "react";
import { Plus } from "lucide-react";
import { PageHeader } from "@/shared/components/PageHeader";
import { Button } from "@/shared/components/Button";
import { showError } from "@/shared/utils/alert";
import { useContracts, useCreateContract } from "../api/useContracts";
import { ContractFilters } from "../components/ContractFilters";
import { ContractForm } from "../components/ContractForm";
import { ContractTable } from "../components/ContractTable";
import { filterContracts } from "../filterContracts";
import styles from "./ContractPage.module.css";

export function ContractPage() {
  const [view, setView] = useState("list");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");

  const { data: contracts = [], isLoading, isError } = useContracts();
  const createContract = useCreateContract();
  const visibleContracts = filterContracts(contracts, { search, status });

  function handleCreate(contract) {
    createContract.mutate(contract, {
      onSuccess: () => setView("list"),
      onError: (error) => showError(error.message),
    });
  }

  if (view === "create") {
    return (
      <div className={styles["page-create"]}>
        <PageHeader title="Novo contrato" description="Vincule paciente, plano e técnica para emitir um novo contrato." />
        <ContractForm
          onSubmit={handleCreate}
          onCancel={() => setView("list")}
          isSubmitting={createContract.isPending}
        />
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Contratos"
        description="Contratos vigentes e histórico de vigência dos pacientes."
        action={
          <Button onClick={() => setView("create")}>
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
      />
    </div>
  );
}
