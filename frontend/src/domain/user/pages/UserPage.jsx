import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { PageHeader } from "@/shared/ui/PageHeader";
import { UserFilters } from "../components/UserFilters";
import { UserTable } from "../components/UserTable";
import { UserFormModal } from "../components/UserFormModal";
import { UserDetailsModal } from "../components/UserDetailsModal";
import styles from "./UserPage.module.css";

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
  const [selectedUser, setSelectedUser] = useState(null);
  // null = fechado; { user: null } = cadastro; { user } = edição
  const [form, setForm] = useState(null);

  // Próximo passo: a lista vem do useUsers (API)
  const users = [];
  const isLoading = false;

  const visibleUsers = filterUsers(users, { search, role, status });

  function openEdit(user) {
    setSelectedUser(null);
    setForm({ user });
  }

  function handleSave() {
    setForm(null);
  }

  function handleDeactivate() {
    setSelectedUser(null);
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

      <UserTable users={visibleUsers} isLoading={isLoading} onViewDetails={setSelectedUser} />

      <UserDetailsModal
        user={selectedUser}
        onEdit={openEdit}
        onDeactivate={handleDeactivate}
        onClose={() => setSelectedUser(null)}
      />

      {form && <UserFormModal user={form.user} onSubmit={handleSave} onClose={() => setForm(null)} />}
    </div>
  );
}
