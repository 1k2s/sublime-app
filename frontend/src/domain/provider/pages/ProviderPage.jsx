import { useState } from "react";
import { Plus } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
import { Button } from "@/shared/ui/Button";
import { PageHeader } from "@/shared/ui/PageHeader";
import { ProviderFilters } from "../components/ProviderFilters";
import { ProviderTable } from "../components/ProviderTable";
import { ProviderFormModal } from "../components/ProviderFormModal";
import { ProviderDetailsModal } from "../components/ProviderDetailsModal";
import styles from "./ProviderPage.module.css";

function filterProviders(providers, { search, status }) {
  const term = search.trim().toLowerCase();
  return providers.filter((provider) => {
    if (status === "ACTIVE" && !provider.active) return false;
    if (status === "INACTIVE" && provider.active) return false;
    return !term || provider.name.toLowerCase().includes(term);
  });
}

// Só usuários ativos e ainda sem prestador; a API valida de novo ao cadastrar
function getAvailableUsers(users, providers) {
  const linkedUserIds = new Set(providers.map((provider) => provider.userId));
  return users.filter((user) => user.active && !linkedUserIds.has(user.id));
}

export function ProviderPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;

  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedProvider, setSelectedProvider] = useState(null);
  // null = fechado; { provider: null } = cadastro; { provider } = edição
  const [form, setForm] = useState(null);

  // Próximo passo: providers e users vêm do useProviders / useUsers (API)
  const providers = [];
  const users = [];
  const isLoading = false;

  const visibleProviders = filterProviders(providers, { search, status });
  const selectedUserEmail = users.find((item) => item.id === selectedProvider?.userId)?.email;

  function openCreate() {
    setForm({ provider: null });
  }

  function openEdit(provider) {
    setSelectedProvider(null);
    setForm({ provider });
  }

  function handleSave() {
    setForm(null);
  }

  function handleToggleStatus() {
    setSelectedProvider(null);
  }

  return (
    <div className={styles.pageContainer}>
      <PageHeader
        className={styles.pageHeader}
        title="Prestadores"
        description="Gerencie os prestadores da clínica e os percentuais de comissão."
        action={
          isAdmin ? (
            <Button onClick={openCreate}>
              <Plus size={18} /> Novo prestador
            </Button>
          ) : null
        }
      />

      <ProviderFilters search={search} onSearchChange={setSearch} status={status} onStatusChange={setStatus} />

      <ProviderTable providers={visibleProviders} isLoading={isLoading} onViewDetails={setSelectedProvider} />

      <ProviderDetailsModal
        provider={selectedProvider}
        userEmail={selectedUserEmail}
        isAdmin={isAdmin}
        onEdit={openEdit}
        onToggleStatus={handleToggleStatus}
        onClose={() => setSelectedProvider(null)}
      />

      {form && (
        <ProviderFormModal
          provider={form.provider}
          users={getAvailableUsers(users, providers)}
          onSubmit={handleSave}
          onClose={() => setForm(null)}
        />
      )}
    </div>
  );
}
