import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { PageHeader } from "@/shared/ui/PageHeader";
import { showError } from "@/shared/utils/alert";
import {
  useActivateUser,
  useCreateUser,
  useDeactivateUser,
  useUpdateUser,
  useUsers,
} from "../api/useUsers";
import { UserFilters } from "../components/UserFilters";
import { UserTable } from "../components/UserTable";
import { UserFormModal } from "../components/UserFormModal";
import { UserDetailsModal } from "../components/UserDetailsModal";
import styles from "./UserPage.module.css";

// A lista é pequena e vem inteira: busca e filtros rodam em memória
function filterUsers(users, { search, role, status }) {
  const term = search.trim().toLowerCase();
  return users.filter((user) => {
    if (role !== "ALL" && user.role !== role) return false;
    if (status === "ACTIVE" && !user.active) return false;
    if (status === "INACTIVE" && user.active) return false;
    return !term || user.email.toLowerCase().includes(term);
  });
}

export function UserPage() {
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("ALL");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedUserId, setSelectedUserId] = useState(null);
  // null = fechado; { user: null } = cadastro; { user } = edição
  const [form, setForm] = useState(null);

  const { data: users = [], isLoading } = useUsers();
  const createUserMutation = useCreateUser();
  const updateUserMutation = useUpdateUser();
  const deactivateUserMutation = useDeactivateUser();
  const activateUserMutation = useActivateUser();

  const visibleUsers = filterUsers(users, { search, role, status });
  // Lido da lista: depois de inativar ou reativar, o modal mostra o estado novo
  const selectedUser = users.find((user) => user.id === selectedUserId) ?? null;
  const isSubmitting = createUserMutation.isPending || updateUserMutation.isPending;
  const isTogglingStatus = deactivateUserMutation.isPending || activateUserMutation.isPending;

  function openEdit(user) {
    setSelectedUserId(null);
    setForm({ user });
  }

  function handleSave(payload) {
    const options = {
      onSuccess: () => setForm(null),
      onError: (error) => showError(error.message),
    };

    if (form?.user) {
      updateUserMutation.mutate({ id: form.user.id, data: payload }, options);
    } else {
      createUserMutation.mutate(payload, options);
    }
  }

  function handleToggleStatus(user) {
    const mutation = user.active ? deactivateUserMutation : activateUserMutation;
    mutation.mutate(user.id, { onError: (error) => showError(error.message) });
  }

  return (
    <div className={styles.pageContainer}>
      <PageHeader
        className={styles.pageHeader}
        title="Usuários"
        description="Gerencie os acessos ao sistema: e-mail, senha e perfil."
        action={
          <Button onClick={() => setForm({ user: null })}>
            <Plus size={18} /> Novo usuário
          </Button>
        }
      />

      <UserFilters
        search={search}
        onSearchChange={setSearch}
        role={role}
        onRoleChange={setRole}
        status={status}
        onStatusChange={setStatus}
      />

      <UserTable
        users={visibleUsers}
        isLoading={isLoading}
        onViewDetails={(user) => setSelectedUserId(user.id)}
      />

      <UserDetailsModal
        user={selectedUser}
        isTogglingStatus={isTogglingStatus}
        onEdit={openEdit}
        onToggleStatus={handleToggleStatus}
        onClose={() => setSelectedUserId(null)}
      />

      {form && (
        <UserFormModal
          user={form.user}
          isSubmitting={isSubmitting}
          onSubmit={handleSave}
          onClose={() => setForm(null)}
        />
      )}
    </div>
  );
}
