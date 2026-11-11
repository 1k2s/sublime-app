import { getRole } from "@/domain/auth/roles";
import { Card } from "@/shared/components/Card";
import { Badge } from "@/shared/components/Badge";
import styles from "./UserTable.module.css";

export function UserTable({ users = [], isLoading = false, onViewDetails }) {
  if (isLoading) {
    return (
      <Card className={styles.messageCard}>
        <span className={styles.messageText}>Carregando usuários...</span>
      </Card>
    );
  }

  if (users.length === 0) {
    return (
      <Card className={styles.messageCard}>
        <p className={styles.messageTitle}>Nenhum usuário encontrado</p>
        <span className={styles.messageText}>Tente ajustar os filtros ou cadastrar um novo usuário.</span>
      </Card>
    );
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
                <button type="button" className={styles.actionButton} onClick={() => onViewDetails?.(user)}>
                  Ver / Editar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
