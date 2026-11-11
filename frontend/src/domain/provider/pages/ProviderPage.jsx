import { useState, useTransition } from "react";
import { Navigate } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { getHomePath, ROLES } from "@/domain/auth/roles";
import { PageHeader } from "@/shared/components/PageHeader";
import { Button } from "@/shared/components/Button";
import { showError } from "@/shared/utils/alert";
import { useProviders, useToggleProviderStatus } from "../api/useProviders";
import { ProviderFilters } from "../components/ProviderFilters";
import { ProviderTable } from "../components/ProviderTable";
import { ProviderDetailsModal } from "../components/ProviderDetailsModal";
import { ProviderWizard } from "../components/wizard/ProviderWizard";
import styles from "./ProviderPage.module.css";

export function ProviderPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;

  const [view, setView] = useState("list");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedProvider, setSelectedProvider] = useState(null);
  const [editingProvider, setEditingProvider] = useState(null);
  const [, startTransition] = useTransition();

  const { data: providers = [], isLoading } = useProviders({ search, status });
  const toggleStatusMutation = useToggleProviderStatus();

  // Acesso exclusivo aos administradores: redireciona prestador para sua tela inicial
  if (!isAdmin) {
    return <Navigate to={getHomePath(user?.role)} replace />;
  }

  const isFormMode = view === "create" || view === "edit";
  const isEditing = view === "edit";

  const handleToggleStatus = async (provider, nextStatus) => {
    try {
      await toggleStatusMutation.mutateAsync({ provider, nextStatus });
      setSelectedProvider((prev) =>
        prev
          ? {
              ...prev,
              status: nextStatus,
              active: nextStatus === "ACTIVE",
            }
          : null
      );
    } catch (error) {
      showError(error.message);
    }
  };

  const handleSearchChange = (val) => {
    startTransition(() => {
      setSearch(val);
    });
  };

  const handleStartCreate = () => {
    setEditingProvider(null);
    setView("create");
  };

  const handleStartEdit = (provider) => {
    setSelectedProvider(null);
    setEditingProvider(provider);
    setView("edit");
  };

  const handleBackToList = () => {
    setEditingProvider(null);
    setView("list");
  };

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
            onSearchChange={handleSearchChange}
            status={status}
            onStatusChange={setStatus}
          />

          <ProviderTable
            providers={providers}
            isLoading={isLoading}
            onViewDetails={(provider) => setSelectedProvider(provider)}
          />

          <ProviderDetailsModal
            provider={selectedProvider}
            onClose={() => setSelectedProvider(null)}
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
