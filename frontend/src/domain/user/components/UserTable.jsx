import { Pencil } from "lucide-react";
import { getRole } from "@/domain/auth/roles";
import { Card } from "@/shared/components/Card";
import { Badge } from "@/shared/components/Badge";
import { ListMessage } from "@/shared/components/ListMessage";
import styles from "./UserTable.module.css";

const MESSAGES = {
  loading: "Carregando usuários...",
  error: "Não foi possível carregar os usuários",
  filtered: "Nenhum usuário encontrado",
  empty: "Nenhum usuário cadastrado",
  emptyHint: "Clique em Novo usuário para começar.",
};

export function UserTable({ users = [], isLoading = false, isError = false, hasRecords = false, onViewDetails }) {
  if (isLoading || isError || users.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={hasRecords} messages={MESSAGES} />;
  }

  return (
    <Card className={styles.tableCard}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.firstColumn}>E-MAIL</th>
            <th>PERFIL</th>
            <th>STATUS</th>
            <th className={styles.actionColumn}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id} className={styles.row}>
              <td className={`${styles.firstColumn} ${styles.email}`}>{user.email}</td>
              <td className={styles.data}>{getRole(user.role)?.label ?? user.role}</td>
              <td>
                <Badge variant={user.active ? "active" : "inactive"}>{user.active ? "Ativo" : "Inativo"}</Badge>
              </td>
              <td className={styles.actionColumn}>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onViewDetails?.(user)}
                  aria-label={`Ver ou editar ${user.email}`}
                  title="Ver / Editar"
                >
                  <Pencil size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
