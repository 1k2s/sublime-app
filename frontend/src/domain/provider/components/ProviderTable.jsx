import { Pencil } from "lucide-react";
import { Card } from "@/shared/ui/Card";
import { Badge } from "@/shared/ui/Badge";
import { ROLES } from "@/domain/auth/roles";
import styles from "./ProviderTable.module.css";

function getInitials(name = "") {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

function getRoleLabel(roleValue) {
  return ROLES[roleValue]?.label || roleValue || "Prestador";
}

export function ProviderTable({
  providers = [],
  isLoading = false,
  onViewDetails,
}) {
  if (isLoading) {
    return (
      <Card className={styles.loadingCard}>
        <span className={styles.emptyText}>Carregando prestadores...</span>
      </Card>
    );
  }

  if (providers.length === 0) {
    return (
      <Card className={styles.emptyCard}>
        <p className={styles.emptyTitle}>Nenhum prestador encontrado</p>
        <span className={styles.emptyText}>
          Tente ajustar os filtros ou cadastrar um novo prestador.
        </span>
      </Card>
    );
  }

  return (
    <Card className={styles.tableCard}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th className={styles.thName}>NOME</th>
            <th>E-MAIL</th>
            <th>PERFIL</th>
            <th>COMISSÃO PADRÃO</th>
            <th>COMISSÃO INDICAÇÃO</th>
            <th>STATUS</th>
            <th className={styles.thAction}>AÇÕES</th>
          </tr>
        </thead>
        <tbody>
          {providers.map((provider) => {
            const isAtivo = provider.status === "ACTIVE";

            return (
              <tr key={provider.id} className={styles.row}>
                <td className={styles.tdName}>
                  <div className={styles.nameCell}>
                    <div className={styles.avatar}>
                      {getInitials(provider.name)}
                    </div>
                    <span className={styles.nameText}>{provider.name}</span>
                  </div>
                </td>

                <td className={styles.tdData}>{provider.email || "--"}</td>

                <td className={styles.tdData}>{getRoleLabel(provider.role)}</td>

                <td className={styles.tdData}>
                  {provider.commissionPercentage}%
                </td>

                <td className={styles.tdData}>
                  {provider.referralCommissionPercentage}%
                </td>

                <td>
                  <Badge variant={isAtivo ? "active" : "inactive"}>
                    {isAtivo ? "Ativo" : "Inativo"}
                  </Badge>
                </td>

                <td className={styles.thAction}>
                  <button
                    type="button"
                    className={styles.actionButton}
                    onClick={() => onViewDetails?.(provider)}
                    aria-label="Ver e editar prestador"
                    title="Ver / Editar"
                  >
                    <Pencil size={16} />
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </Card>
  );
}
