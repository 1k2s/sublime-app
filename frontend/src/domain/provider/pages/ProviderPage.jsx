import { useState } from "react";
import { Plus } from "lucide-react";
import { PageHeader } from "@/shared/components/PageHeader";
import { Button } from "@/shared/components/Button";
import { showError } from "@/shared/utils/alert";
import { useUsers } from "@/domain/user/api/useUsers";
import {
  useActivateProvider,
  useCreateProvider,
  useDeactivateProvider,
  useGrantRetroactiveLaunches,
  useProviders,
  useRetroactiveGrant,
  useUpdateProvider,
} from "../api/useProviders";
import { ProviderFilters } from "../components/ProviderFilters";
import { ProviderTable } from "../components/ProviderTable";
import { ProviderDetailsModal } from "../components/ProviderDetailsModal";
import { ProviderFormModal } from "../components/ProviderFormModal";
import { filterProviders } from "../utils/filterProviders";
import { getAvailableUsers, withLinkedUser } from "../utils/providerUsers";
import styles from "./ProviderPage.module.css";

export function ProviderPage() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedProviderId, setSelectedProviderId] = useState(null);
  // null = fechado; { provider: null } = cadastro; { provider } = edição
  const [form, setForm] = useState(null);

  const providersQuery = useProviders();
  const usersQuery = useUsers();
  const createProviderMutation = useCreateProvider();
  const updateProviderMutation = useUpdateProvider();
  const deactivateProviderMutation = useDeactivateProvider();
  const activateProviderMutation = useActivateProvider();
  // A liberação de lançamentos retroativos do prestador aberto no modal
  const grantQuery = useRetroactiveGrant(selectedProviderId);
  const grantMutation = useGrantRetroactiveLaunches();

  const rawProviders = providersQuery.data ?? [];
  const users = usersQuery.data ?? [];
  // E-mail, perfil e status do usuário vêm da lista de usuários (o mesmo cache da tela de Usuários)
  const providers = withLinkedUser(rawProviders, users);
  const visibleProviders = filterProviders(providers, { search, status });
  // Lido da lista inteira: depois de inativar ou reativar, o modal mostra o estado novo
  const selectedProvider = providers.find((provider) => provider.id === selectedProviderId) ?? null;
  const isLoading = providersQuery.isLoading || usersQuery.isLoading;
  const isError = providersQuery.isError || usersQuery.isError;
  const isSubmitting = createProviderMutation.isPending || updateProviderMutation.isPending;
  const isTogglingStatus = deactivateProviderMutation.isPending || activateProviderMutation.isPending;

  function openEdit(provider) {
    setSelectedProviderId(null);
    setForm({ provider });
  }

  function handleSave(payload, { applyApiErrors }) {
    const options = {
      onSuccess: () => setForm(null),
      onError: (error) => {
        applyApiErrors?.(error);
        showError(error.message);
      },
    };

    if (form?.provider) {
      updateProviderMutation.mutate({ id: form.provider.id, data: payload }, options);
    } else {
      createProviderMutation.mutate(payload, options);
    }
  }

  function handleToggleStatus(provider) {
    const mutation = provider.active ? deactivateProviderMutation : activateProviderMutation;
    mutation.mutate(provider.id, { onError: (error) => showError(error.message) });
  }

  function handleGrant(provider, allowedCount) {
    grantMutation.mutate(
      { providerId: provider.id, allowedCount },
      { onError: (error) => showError(error.message) },
    );
  }

  return (
    <div className={styles.pageContainer}>
      <PageHeader
        className={styles.pageHeader}
        title="Prestadores"
        description="Gerencie os prestadores da clínica e os percentuais de comissão."
        action={
          <Button onClick={() => setForm({ provider: null })}>
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
        onEdit={openEdit}
        onToggleStatus={handleToggleStatus}
        grant={grantQuery.data}
        isGrantLoading={grantQuery.isLoading}
        onGrant={handleGrant}
        isGranting={grantMutation.isPending}
      />

      {form && (
        <ProviderFormModal
          provider={form.provider}
          availableUsers={getAvailableUsers(users, rawProviders)}
          isSubmitting={isSubmitting}
          onSubmit={handleSave}
          onClose={() => setForm(null)}
        />
      )}
    </div>
  );
}
