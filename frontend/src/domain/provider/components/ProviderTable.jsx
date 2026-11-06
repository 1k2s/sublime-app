import { Card } from "@/shared/ui/Card";
import { Badge } from "@/shared/ui/Badge";
import { formatPercentage } from "../percentage";
import styles from "./ProviderTable.module.css";

export function ProviderTable({ providers = [], isLoading = false, onViewDetails }) {
  if (isLoading) {
    return (
      <Card className={styles.messageCard}>
        <span className={styles.messageText}>Carregando prestadores...</span>
      </Card>
    );
  }

  if (providers.length === 0) {
    return (
      <Card className={styles.messageCard}>
        <p className={styles.messageTitle}>Nenhum prestador encontrado</p>
        <span className={styles.messageText}>Tente ajustar os filtros ou cadastrar um novo prestador.</span>
      </Card>
    );
  }

  return (
    <Card className={styles.tableCard}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.firstColumn}>NOME</th>
            <th>COMISSÃO PADRÃO</th>
            <th>PACIENTES TRAZIDOS</th>
            <th>STATUS</th>
            <th className={styles.actionColumn}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {providers.map((provider) => (
            <tr key={provider.id} className={styles.row}>
              <td className={`${styles.firstColumn} ${styles.name}`}>{provider.name}</td>
              <td className={styles.data}>{formatPercentage(provider.commissionPercentage)}</td>
              <td className={styles.data}>{formatPercentage(provider.referralCommissionPercentage)}</td>
              <td>
                <Badge variant={provider.active ? "active" : "inactive"}>
                  {provider.active ? "Ativo" : "Inativo"}
                </Badge>
              </td>
              <td className={styles.actionColumn}>
                <button type="button" className={styles.actionButton} onClick={() => onViewDetails?.(provider)}>
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
