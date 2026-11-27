import { Pencil } from "lucide-react";
import { getRole } from "@/domain/auth/roles";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { ListMessage } from "@/shared/components/ListMessage";

const MESSAGES = {
  loading: "Carregando usuários...",
  error: "Não foi possível carregar os usuários",
  filtered: "Nenhum usuário encontrado",
  empty: "Nenhum usuário cadastrado",
  emptyHint: "Clique em Novo usuário para começar.",
};

export function UserTable({
  users = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  selectedUserId = null,
  onViewDetails,
}) {
  if (isLoading || isError || users.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={hasRecords} messages={MESSAGES} />;
  }

  const columns = [
    { key: "email", header: "E-mail", cell: (user) => user.email },
    { key: "role", header: "Perfil", cell: (user) => getRole(user.role)?.label ?? user.role },
    {
      key: "status",
      header: "Status",
      cell: (user) => <Badge variant={user.active ? "active" : "inactive"}>{user.active ? "Ativo" : "Inativo"}</Badge>,
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (user) => (
        <IconButton label={`Ver ou editar ${user.email}`} title="Ver / Editar" onClick={() => onViewDetails?.(user)}>
          <Pencil size={16} />
        </IconButton>
      ),
    },
  ];

  // A linha inteira abre o detalhe; a linha aberta fica marcada
  return (
    <DataTable
      columns={columns}
      rows={users}
      onRowClick={(user) => onViewDetails?.(user)}
      selectedRowKey={selectedUserId}
    />
  );
}
