import { useState } from "react";
import { Plus } from "lucide-react";
import { PageHeader } from "@/shared/components/PageHeader";
import { Button } from "@/shared/components/Button";
import { showError } from "@/shared/utils/alert";
import { useActivateProvider, useDeactivateProvider, useProviders } from "../api/useProviders";
import { ProviderFilters } from "../components/ProviderFilters";
import { ProviderTable } from "../components/ProviderTable";
import { ProviderDetailsModal } from "../components/ProviderDetailsModal";
import { ProviderWizard } from "../components/wizard/ProviderWizard";
import { filterProviders } from "../filterProviders";
import styles from "./ProviderPage.module.css";

export function ProviderPage() {
  const [view, setView] = useState("list");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedProviderId, setSelectedProviderId] = useState(null);
  const [editingProvider, setEditingProvider] = useState(null);

  const { data: providers = [], isLoading, isError } = useProviders();
  const deactivateProviderMutation = useDeactivateProvider();
  const activateProviderMutation = useActivateProvider();

  const visibleProviders = filterProviders(providers, { search, status });
  // Lido da lista inteira: depois de inativar ou reativar, o modal mostra o estado novo
  const selectedProvider = providers.find((provider) => provider.id === selectedProviderId) ?? null;
  const isFormMode = view === "create" || view === "edit";
  const isEditing = view === "edit";
  const isTogglingStatus = deactivateProviderMutation.isPending || activateProviderMutation.isPending;

  function handleToggleStatus(provider) {
    const mutation = provider.active ? deactivateProviderMutation : activateProviderMutation;
    mutation.mutate(provider.id, { onError: (error) => showError(error.message) });
  }

  function handleStartCreate() {
    setEditingProvider(null);
    setView("create");
  }

  function handleStartEdit(provider) {
    setSelectedProviderId(null);
    setEditingProvider(provider);
    setView("edit");
  }

  function handleBackToList() {
    setEditingProvider(null);
    setView("list");
  }

  return (
    <div className={styles.pageContainer}>
      {!isFormMode ? (
        <>
          <PageHeader
            className={styles.pageHeader}
            title="Prestadores"
            description="Gerencie os prestadores da clínica, comissões e contas de acesso."
            action={
              <Button onClick={handleStartCreate}>
                <Plus size={18} /> Novo prestador
              </Button>
            }
          />

          <ProviderFilters
            search={search}
            onSearchChange={setSearch}
            status={status}
            onStatusChange={setStatus}
          />

          <ProviderTable
            providers={visibleProviders}
            isLoading={isLoading}
            isError={isError}
            hasRecords={providers.length > 0}
            onViewDetails={(provider) => setSelectedProviderId(provider.id)}
          />

          <ProviderDetailsModal
            provider={selectedProvider}
            isTogglingStatus={isTogglingStatus}
            onClose={() => setSelectedProviderId(null)}
            onEdit={handleStartEdit}
            onToggleStatus={handleToggleStatus}
          />
        </>
      ) : (
        <>
          <PageHeader
            className={styles.pageHeader}
            title={isEditing ? "Editar prestador" : "Novo prestador"}
            description={
              isEditing
                ? "Atualize os dados do prestador, comissões ou credenciais de acesso."
                : "Cadastre o prestador e sua conta de acesso em etapas."
            }
            onBack={handleBackToList}
          />

          <ProviderWizard
            initialData={editingProvider}
            onCancel={handleBackToList}
            onSuccess={handleBackToList}
          />
        </>
      )}
    </div>
  );
}
