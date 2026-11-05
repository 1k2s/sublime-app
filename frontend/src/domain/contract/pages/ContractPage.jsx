import { useState } from "react";
import { Plus } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Button } from "@/shared/ui/Button";
import { showError } from "@/shared/utils/alert";
import { useContracts, useCreateContract } from "../api/useContracts";
import { ContractFilters } from "../components/ContractFilters";
import { ContractForm } from "../components/ContractForm";
import { ContractTable } from "../components/ContractTable";
import styles from "./ContractPage.module.css";

export function ContractPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;

  const [view, setView] = useState("list");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");

  const { data: contracts = [], isLoading, isError } = useContracts({ search, status });
  const createContract = useCreateContract();
  const isFiltered = search.trim() !== "" || status !== "ALL";

  function handleCreate(contract) {
    createContract.mutate(contract, {
      onSuccess: () => setView("list"),
      onError: (error) => showError(error.message),
    });
  }

  if (view === "create" && isAdmin) {
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
          isAdmin && (
            <Button onClick={() => setView("create")}>
              <Plus size={18} /> Novo contrato
            </Button>
          )
        }
      />

      <ContractFilters search={search} onSearchChange={setSearch} status={status} onStatusChange={setStatus} />

      <ContractTable contracts={contracts} isLoading={isLoading} isError={isError} isFiltered={isFiltered} />
    </div>
  );
}
